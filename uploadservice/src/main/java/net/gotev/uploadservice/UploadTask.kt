package net.gotev.uploadservice

import android.content.Intent
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.task.BroadcastEmitter
import net.gotev.uploadservice.observer.task.NotificationHandler
import net.gotev.uploadservice.observer.task.UploadTaskObserver
import java.io.IOException
import java.util.*

/**
 * Base class to subclass when creating upload tasks. It contains the logic common to all the tasks,
 * such as notification management, status broadcast, retry logic and some utility methods.
 *
 * @author Aleksandar Gotev
 */
abstract class UploadTask : Runnable {

    companion object {
        private val LOG_TAG = UploadTask::class.java.simpleName
    }

    /**
     * Reference to the upload service instance.
     */
    protected lateinit var service: UploadService

    /**
     * Contains all the parameters set in [UploadRequest].
     */
    lateinit var params: UploadTaskParameters

    /**
     * Contains the absolute local path of the successfully uploaded files.
     */
    private val successfullyUploadedFiles = ArrayList<UploadFile>()

    /**
     * Flag indicating if the operation should continue or is cancelled. You should never
     * explicitly set this value in your subclasses, as it's written by the Upload Service
     * when you call [UploadService.stopUpload]. If this value is false, you should
     * terminate your upload task as soon as possible, so be sure to check the status when
     * performing long running operations such as data transfer. As a rule of thumb, check this
     * value at every step of the upload protocol you are implementing, and after that each chunk
     * of data that has been successfully transferred.
     */
    var shouldContinue = true

    private var lastProgressNotificationTime: Long = 0

    private val observers = ArrayList<UploadTaskObserver>(2)

    /**
     * Total bytes to transfer. You should initialize this value in the
     * [UploadTask.upload] method of your subclasses, before starting the upload data
     * transfer.
     */
    var totalBytes: Long = 0

    /**
     * Total transferred bytes. You should update this value in your subclasses when you upload
     * some data, and before calling [UploadTask.broadcastProgress]
     */
    protected var uploadedBytes: Long = 0

    /**
     * Start timestamp of this upload task.
     */
    private val startTime: Long = Date().time

    /**
     * Counter of the upload attempts that has been made;
     */
    private var attempts: Int = 0

    private var errorDelay = UploadServiceConfig.retryPolicy.initialWaitTimeSeconds.toLong()

    private val uploadInfo: UploadInfo
        get() = UploadInfo(
                uploadId = params.id,
                startTime = startTime,
                uploadedBytes = uploadedBytes,
                totalBytes = totalBytes,
                numberOfRetries = attempts - 1,
                notificationID = null,
                successfullyUploadedFiles = successfullyUploadedFiles,
                remainingFiles = params.files
        )

    /**
     * Implementation of the upload logic.
     *
     * @throws Exception if an error occurs
     */
    @Throws(Exception::class)
    protected abstract fun upload()

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected open fun onSuccessfulUpload() {}

    private inline fun doForEachObserver(action: UploadTaskObserver.() -> Unit) {
        observers.forEach {
            try {
                action(it)
            } catch (exc: Throwable) {
                UploadServiceLogger.error(LOG_TAG, "(uploadID: ${params.id}) error while dispatching event to observer", exc)
            }
        }
    }

    /**
     * Initializes the [UploadTask].<br></br>
     * Override this method in subclasses to perform custom task initialization and to get the
     * custom parameters set in [UploadRequest.initializeIntent] method.
     *
     * @param service Upload Service instance. You should use this reference as your context.
     * @param intent  intent sent to the service to start the upload
     * @throws IOException if an I/O exception occurs while initializing
     */
    @Throws(IOException::class)
    open fun init(service: UploadService, notificationID: Int, intent: Intent) {
        this.params = intent.getParcelableExtra(UploadService.PARAM_TASK_PARAMETERS) ?: throw IOException("Missing task parameters")
        this.service = service

        observers.add(BroadcastEmitter(service))

        params.notificationConfig?.let { config ->
            observers.add(NotificationHandler(service, notificationID, params.id, config))
        }
    }

    private fun resetAttempts() {
        attempts = 0
        errorDelay = UploadServiceConfig.retryPolicy.initialWaitTimeSeconds.toLong()
    }

    override fun run() {
        doForEachObserver { initialize(UploadInfo(params.id)) }

        resetAttempts()

        while (attempts <= params.maxRetries && shouldContinue) {
            try {
                upload()
                break

            } catch (exc: Throwable) {
                if (!shouldContinue) {
                    break
                } else if (attempts >= params.maxRetries) {
                    broadcastError(exc)
                } else {
                    UploadServiceLogger.error(LOG_TAG, "(uploadID: ${params.id}) error on attempt ${attempts + 1}. Waiting ${errorDelay}s before next attempt. ", exc)

                    val beforeSleepTs = System.currentTimeMillis()

                    while (shouldContinue && System.currentTimeMillis() < beforeSleepTs + errorDelay * 1000) {
                        safeSleep(2000)
                    }

                    errorDelay *= UploadServiceConfig.retryPolicy.multiplier.toLong()

                    if (errorDelay > UploadServiceConfig.retryPolicy.maxWaitTimeSeconds) {
                        errorDelay = UploadServiceConfig.retryPolicy.maxWaitTimeSeconds.toLong()
                    }
                }
            }

            attempts++
        }

        if (!shouldContinue) {
            broadcastCancelled()
        }
    }

    private fun safeSleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (exc: Throwable) { }
    }

    private fun shouldThrottle(uploadedBytes: Long, totalBytes: Long): Boolean {
        val currentTime = System.currentTimeMillis()

        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadServiceConfig.uploadProgressNotificationIntervalMillis) {
            return true
        }

        lastProgressNotificationTime = currentTime
        return false
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes number of bytes which has been uploaded to the server
     * @param totalBytes    total bytes of the request
     */
    protected fun broadcastProgress(uploadedBytes: Long, totalBytes: Long) {
        // reset retry attempts on progress. This way only a single while cycle is needed
        resetAttempts()

        if (shouldThrottle(uploadedBytes, totalBytes)) return

        UploadServiceLogger.debug(LOG_TAG, "(uploadID: ${params.id}) uploaded ${uploadedBytes * 100 / totalBytes}%, $uploadedBytes of $totalBytes bytes")
        val uploadInfo = uploadInfo
        doForEachObserver { onProgress(uploadInfo) }
    }

    /**
     * Broadcasts a completion status update and informs the [UploadService] that the task
     * executes successfully.
     * Call this when the task has completed the upload request and has received the response
     * from the server.
     *
     * @param response response got from the server
     */
    protected fun broadcastCompleted(response: ServerResponse) {
        UploadServiceLogger.debug(LOG_TAG, "(uploadID: ${params.id}) upload ${if (response.isSuccessful) "completed" else "error"}")

        if (response.isSuccessful) {
            onSuccessfulUpload()

            if (params.autoDeleteSuccessfullyUploadedFiles && successfullyUploadedFiles.isNotEmpty()) {
                for (file in successfullyUploadedFiles) {
                    if (file.handler.delete(service)) {
                        UploadServiceLogger.info(LOG_TAG, "(uploadID: ${params.id}) successfully deleted: ${file.path}")
                    } else {
                        UploadServiceLogger.error(LOG_TAG, "(uploadID: ${params.id}) error while deleting: ${file.path}")
                    }
                }
            }
        }

        val uploadInfo = uploadInfo

        doForEachObserver { onCompleted(uploadInfo, response) }

        service.taskCompleted(params.id)
    }

    /**
     * Broadcast a cancelled status.
     * This called automatically by [UploadTask] when the user cancels the request,
     * and the specific implementation of [UploadTask.upload] either
     * returns or throws an exception. You should never call this method explicitly in your
     * implementation.
     */
    private fun broadcastCancelled() {
        UploadServiceLogger.debug(LOG_TAG, "(uploadID: ${params.id}) upload cancelled")

        val uploadInfo = uploadInfo
        doForEachObserver { onCancelled(uploadInfo) }

        service.taskCompleted(params.id)
    }

    /**
     * Broadcasts an error.
     * This called automatically by [UploadTask] when the specific implementation of
     * [UploadTask.upload] throws an exception and there aren't any left retries.
     * You should never call this method explicitly in your implementation.
     *
     * @param exception exception to broadcast. It's the one thrown by the specific implementation
     * of [UploadTask.upload]
     */
    private fun broadcastError(exception: Throwable) {
        UploadServiceLogger.error(LOG_TAG, "(uploadID: ${params.id}) error", exception)

        val uploadInfo = uploadInfo

        doForEachObserver { onError(uploadInfo, exception) }

        service.taskCompleted(params.id)
    }

    /**
     * Add a file to the list of the successfully uploaded files and remove it from the file list
     *
     * @param file file on the device
     */
    protected fun addSuccessfullyUploadedFile(file: UploadFile) {
        if (!successfullyUploadedFiles.contains(file)) {
            successfullyUploadedFiles.add(file)
            params.files.remove(file)
        }
    }

    /**
     * Adds all the files to the list of successfully uploaded files.
     * This will automatically remove them from the params.getFiles() list.
     */
    protected fun addAllFilesToSuccessfullyUploadedFiles() {
        val iterator = params.files.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()

            if (!successfullyUploadedFiles.contains(file)) {
                successfullyUploadedFiles.add(file)
            }
            iterator.remove()
        }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use [UploadTask.addSuccessfullyUploadedFile]
     *
     * @return list of strings
     */
    protected fun getSuccessfullyUploadedFiles(): List<UploadFile> {
        return successfullyUploadedFiles
    }

    fun cancel() {
        shouldContinue = false
    }

}
