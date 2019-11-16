package net.gotev.uploadservice

import android.content.Context
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.task.UploadTaskObserver
import java.io.IOException
import java.util.ArrayList
import java.util.Date

abstract class UploadTask : Runnable {

    companion object {
        private val TAG = UploadTask::class.java.simpleName
    }

    private var lastProgressNotificationTime: Long = 0

    protected lateinit var context: Context
    lateinit var params: UploadTaskParameters
    var notificationId: Int = 0

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

    private val observers = ArrayList<UploadTaskObserver>(2)

    /**
     * Total bytes to transfer. You should initialize this value in the
     * [UploadTask.upload] method of your subclasses, before starting the upload data
     * transfer.
     */
    var totalBytes: Long = 0

    /**
     * Total transferred bytes. You should update this value in your subclasses when you upload
     * some data, and before calling [UploadTask.onProgress]
     */
    private var uploadedBytes: Long = 0

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
            files = params.files
        )

    /**
     * Implementation of the upload logic.
     *
     * @throws Exception if an error occurs
     */
    @Throws(Exception::class)
    protected abstract fun upload(httpStack: HttpStack)

    private inline fun doForEachObserver(action: UploadTaskObserver.() -> Unit) {
        observers.forEach {
            try {
                action(it)
            } catch (exc: Throwable) {
                UploadServiceLogger.error(TAG, params.id, exc) {
                    "error while dispatching event to observer"
                }
            }
        }
    }

    /**
     * Initializes the [UploadTask].<br></br>
     * Override this method in subclasses to perform custom task initialization and to get the
     * custom parameters set in [UploadRequest.initializeIntent] method.
     *
     * @param context Upload Service instance. You should use this reference as your context.
     * @param intent intent sent to the context to start the upload
     * @throws IOException if an I/O exception occurs while initializing
     */
    @Throws(IOException::class)
    fun init(
        context: Context,
        taskParams: UploadTaskParameters,
        notificationId: Int,
        vararg taskObservers: UploadTaskObserver
    ) {
        this.context = context
        this.params = taskParams
        this.notificationId = notificationId
        taskObservers.forEach { observers.add(it) }
        performInitialization()
    }

    open fun performInitialization() {}

    private fun resetAttempts() {
        attempts = 0
        errorDelay = UploadServiceConfig.retryPolicy.initialWaitTimeSeconds.toLong()
    }

    override fun run() {
        doForEachObserver {
            onStart(
                UploadInfo(params.id),
                notificationId,
                params.notificationConfig
            )
        }
        resetAttempts()

        while (attempts <= params.maxRetries && shouldContinue) {
            try {
                resetUploadedBytes()
                upload(UploadServiceConfig.httpStack)
                break
            } catch (exc: Throwable) {
                if (!shouldContinue) {
                    UploadServiceLogger.error(TAG, params.id, exc) { "error while uploading but user requested cancellation." }
                    break
                } else if (attempts >= params.maxRetries) {
                    onError(exc)
                } else {
                    UploadServiceLogger.error(TAG, params.id, exc) { "error on attempt ${attempts + 1}. Waiting ${errorDelay}s before next attempt." }

                    val sleepDeadline = System.currentTimeMillis() + errorDelay * 1000

                    sleepWhile { shouldContinue && System.currentTimeMillis() < sleepDeadline }

                    errorDelay *= UploadServiceConfig.retryPolicy.multiplier.toLong()

                    if (errorDelay > UploadServiceConfig.retryPolicy.maxWaitTimeSeconds) {
                        errorDelay = UploadServiceConfig.retryPolicy.maxWaitTimeSeconds.toLong()
                    }
                }
            }

            attempts++
        }

        if (!shouldContinue) {
            onUserCancelledUpload()
        }
    }

    private inline fun sleepWhile(millis: Long = 1000, condition: () -> Boolean) {
        while (condition()) {
            try {
                Thread.sleep(millis)
            } catch (exc: Throwable) {
            }
        }
    }

    protected fun resetUploadedBytes() {
        uploadedBytes = 0
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes number of bytes which has been uploaded to the server
     * @param totalBytes total bytes of the request
     */
    protected fun onProgress(bytesSent: Long) {
        uploadedBytes += bytesSent
        if (shouldThrottle(uploadedBytes, totalBytes)) return
        UploadServiceLogger.debug(TAG, params.id) { "uploaded ${uploadedBytes * 100 / totalBytes}%, $uploadedBytes of $totalBytes bytes" }
        doForEachObserver { onProgress(uploadInfo, notificationId, params.notificationConfig) }
    }

    /**
     * Broadcasts a completion status update and informs the [UploadService] that the task
     * executes successfully.
     * Call this when the task has completed the upload request and has received the response
     * from the server.
     *
     * @param response response got from the server
     */
    protected fun onResponseReceived(response: ServerResponse) {
        UploadServiceLogger.debug(TAG, params.id) { "upload ${if (response.isSuccessful) "completed" else "error"}" }

        if (response.isSuccessful) {
            if (params.autoDeleteSuccessfullyUploadedFiles) {
                for (file in successfullyUploadedFiles) {
                    if (file.handler.delete(context)) {
                        UploadServiceLogger.info(TAG, params.id) { "successfully deleted: ${file.path}" }
                    } else {
                        UploadServiceLogger.error(TAG, params.id) { "error while deleting: ${file.path}" }
                    }
                }
            }

            doForEachObserver {
                onSuccess(
                    uploadInfo,
                    notificationId,
                    params.notificationConfig,
                    response
                )
            }
        } else {
            doForEachObserver {
                onError(
                    uploadInfo,
                    notificationId,
                    params.notificationConfig,
                    UploadError(response)
                )
            }
        }

        doForEachObserver { onCompleted(uploadInfo, notificationId, params.notificationConfig) }
    }

    /**
     * Broadcast a cancelled status.
     * This called automatically by [UploadTask] when the user cancels the request,
     * and the specific implementation of [UploadTask.upload] either
     * returns or throws an exception. You should never call this method explicitly in your
     * implementation.
     */
    private fun onUserCancelledUpload() {
        UploadServiceLogger.debug(TAG, params.id) { "upload cancelled" }
        onError(UserCancelledUploadException())
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
    private fun onError(exception: Throwable) {
        UploadServiceLogger.error(TAG, params.id, exception) { "error" }
        uploadInfo.let {
            doForEachObserver { onError(it, notificationId, params.notificationConfig, exception) }
            doForEachObserver { onCompleted(it, notificationId, params.notificationConfig) }
        }
    }

    /**
     * Adds all the files to the list of successfully uploaded files.
     * This will automatically remove them from the params.getFiles() list.
     */
    protected fun setAllFilesHaveBeenSuccessfullyUploaded(value: Boolean = true) {
        params.files.forEach { it.successfullyUploaded = value }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use [UploadTask.addSuccessfullyUploadedFile]
     *
     * @return list of strings
     */
    protected val successfullyUploadedFiles: List<UploadFile>
        get() = params.files.filter { it.successfullyUploaded }

    fun cancel() {
        shouldContinue = false
    }

    private fun shouldThrottle(uploadedBytes: Long, totalBytes: Long): Boolean {
        val currentTime = System.currentTimeMillis()

        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadServiceConfig.uploadProgressNotificationIntervalMillis) {
            return true
        }

        lastProgressNotificationTime = currentTime
        return false
    }
}
