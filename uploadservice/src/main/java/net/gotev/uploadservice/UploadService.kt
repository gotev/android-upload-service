package net.gotev.uploadservice

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import net.gotev.uploadservice.UploadService.Companion.taskClass
import net.gotev.uploadservice.UploadService.Companion.taskParameters
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.extensions.acquirePartialWakeLock
import net.gotev.uploadservice.extensions.safeRelease
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.observer.task.BroadcastEmitter
import net.gotev.uploadservice.observer.task.NotificationHandler
import net.gotev.uploadservice.observer.task.TaskCompletionNotifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class UploadService : Service() {

    companion object {
        private val TAG = UploadService::class.java.simpleName

        // constants used in the intent which starts this service
        internal const val taskParameters = "taskParameters"
        internal const val taskClass = "taskClass"

        private const val UPLOAD_NOTIFICATION_BASE_ID = 1234 // Something unique

        private var notificationIncrementalId = 0
        private val uploadTasksMap = ConcurrentHashMap<String, UploadTask>()

        @Volatile
        private var foregroundUploadId: String? = null

        /**
         * Stops the upload task with the given uploadId.
         * @param uploadId The unique upload id
         */
        @Synchronized
        fun stopUpload(uploadId: String) {
            uploadTasksMap[uploadId]?.cancel()
        }

        /**
         * Gets the list of the currently active upload tasks.
         * @return list of uploadIDs or an empty list if no tasks are currently running
         */
        val taskList: List<String>
            @Synchronized get() = if (uploadTasksMap.isEmpty()) {
                emptyList()
            } else {
                uploadTasksMap.keys().toList()
            }

        /**
         * Stop all the active uploads.
         */
        @Synchronized
        fun stopAllUploads() {
            val iterator = uploadTasksMap.keys.iterator()

            while (iterator.hasNext()) {
                uploadTasksMap[iterator.next()]?.cancel()
            }
        }

        /**
         * Stops the service if no upload tasks are currently running
         * @param context application context
         * @return true if the service is getting stopped, false otherwise
         */
        @Synchronized
        fun stop(context: Context) = stop(context, false)

        /**
         * Stops the service.
         * @param context application context
         * @param forceStop stops the service no matter if some tasks are running
         * @return true if the service is getting stopped, false otherwise
         */
        @Synchronized
        fun stop(context: Context, forceStop: Boolean) = if (forceStop) {
            context.stopService(Intent(context, UploadService::class.java))
        } else {
            uploadTasksMap.isEmpty() && context.stopService(Intent(context, UploadService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val uploadTasksQueue = LinkedBlockingQueue<Runnable>()
    private val uploadThreadPool by lazy {
        ThreadPoolExecutor(
                UploadServiceConfig.uploadPoolSize, // Initial pool size
                UploadServiceConfig.uploadPoolSize, // Max pool size
                UploadServiceConfig.keepAliveTimeSeconds.toLong(),
                TimeUnit.SECONDS,
                uploadTasksQueue
        )
    }
    private var idleTimer: Timer? = null

    override fun onCreate() {
        super.onCreate()

        wakeLock = acquirePartialWakeLock(wakeLock, TAG)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || UploadServiceConfig.uploadAction != intent.action) {
            return shutdownIfThereArentAnyActiveTasks()
        }

        UploadServiceLogger.debug(TAG) { "Starting UploadService. Debug info: $UploadServiceConfig" }

        val currentTask = getTask(intent) ?: return shutdownIfThereArentAnyActiveTasks()

        if (uploadTasksMap.containsKey(currentTask.params.id)) {
            UploadServiceLogger.error(TAG) {
                "Preventing upload with id: ${currentTask.params.id} to be uploaded twice! " +
                        "Please check your code and fix it!"
            }
            return shutdownIfThereArentAnyActiveTasks()
        }

        clearIdleTimer()

        uploadTasksMap[currentTask.params.id] = currentTask
        uploadThreadPool.execute(currentTask)

        return START_STICKY
    }

    @Synchronized
    private fun clearIdleTimer() {
        idleTimer?.apply {
            UploadServiceLogger.info(TAG) { "Clearing idle timer" }
            cancel()
        }
        idleTimer = null
    }

    @Synchronized
    private fun shutdownIfThereArentAnyActiveTasks(): Int {
        if (uploadTasksMap.isEmpty()) {
            clearIdleTimer()

            UploadServiceLogger.info(TAG) {
                "Service will be shut down in ${UploadServiceConfig.idleTimeoutSeconds}s " +
                        "if no new tasks are received"
            }

            idleTimer = Timer(TAG + "IdleTimer").apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        UploadServiceLogger.info(TAG) {
                            "Service is about to be stopped because idle timeout of " +
                                    "${UploadServiceConfig.idleTimeoutSeconds}s has been reached"
                        }
                        stopSelf()
                    }
                }, (UploadServiceConfig.idleTimeoutSeconds * 1000).toLong())
            }

            return START_NOT_STICKY
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        stopAllUploads()
        uploadThreadPool.shutdown()

        if (UploadServiceConfig.isForegroundService) {
            UploadServiceLogger.debug(TAG) { "Stopping foreground execution" }
            stopForeground(true)
        }

        wakeLock.safeRelease()

        uploadTasksMap.clear()

        UploadServiceLogger.debug(TAG) { "UploadService destroyed" }
    }

    /**
     * Creates a new task instance based on the requested task class in the intent.
     * @param intent intent passed to the service
     * @return task instance or null if the task class is not supported or invalid
     */
    fun getTask(intent: Intent): UploadTask? {
        val taskClassString = intent.getStringExtra(taskClass) ?: run {
            UploadServiceLogger.error(TAG) { "Error while instantiating new task. No task class defined in Intent." }
            return null
        }

        val params: UploadTaskParameters = intent.getParcelableExtra(taskParameters) ?: run {
            UploadServiceLogger.error(TAG) { "Error while instantiating new task. Missing task parameters." }
            return null
        }

        return try {
            val task = Class.forName(taskClassString)

            if (!UploadTask::class.java.isAssignableFrom(task)) {
                UploadServiceLogger.error(TAG) { "$taskClassString does not extend UploadTask!" }
                return null
            }

            val uploadTask = UploadTask::class.java.cast(task.newInstance()) ?: return null

            // increment by 2 because the notificationIncrementalId + 1 is used internally
            // in each UploadTask. Check its sources for more info about this.
            notificationIncrementalId += 2

            val observers = listOfNotNull(
                    BroadcastEmitter(this),
                    params.notificationConfig?.let {
                        NotificationHandler(this, UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId, params.id, it)
                    },
                    TaskCompletionNotifier(this)
            ).toTypedArray()

            uploadTask.init(this, params, *observers)

            UploadServiceLogger.debug(TAG) { "Successfully created new task with class: $taskClassString" }
            uploadTask

        } catch (exc: Throwable) {
            UploadServiceLogger.error(TAG, exc) { "Error while instantiating new task" }
            null
        }
    }

    /**
     * Check if the task is currently the one shown in the foreground notification.
     * @param uploadId ID of the upload
     * @return true if the current upload task holds the foreground notification, otherwise false
     */
    @Synchronized
    fun holdForegroundNotification(uploadId: String, notification: Notification): Boolean {
        if (!UploadServiceConfig.isForegroundService) return false

        if (foregroundUploadId == null) {
            foregroundUploadId = uploadId
            UploadServiceLogger.debug(TAG) { "$uploadId now holds the foreground notification" }
        }

        if (uploadId == foregroundUploadId) {
            startForeground(UPLOAD_NOTIFICATION_BASE_ID, notification)
            return true
        }

        return false
    }

    /**
     * Called by each task when it is completed (either successfully, with an error or due to
     * user cancellation).
     * @param uploadId the uploadID of the finished task
     */
    @Synchronized
    fun taskCompleted(uploadId: String) {
        val task = uploadTasksMap.remove(uploadId)

        // un-hold foreground upload ID if it's been hold
        if (UploadServiceConfig.isForegroundService && task != null && task.params.id == foregroundUploadId) {
            UploadServiceLogger.debug(TAG) { "$uploadId now un-holded the foreground notification" }
            foregroundUploadId = null
        }

        if (UploadServiceConfig.isForegroundService && uploadTasksMap.isEmpty()) {
            UploadServiceLogger.debug(TAG) { "All tasks completed, stopping foreground execution" }
            stopForeground(true)
            shutdownIfThereArentAnyActiveTasks()
        }
    }
}

// TODO: move in extension
fun Intent.setupTask(taskClassString: String, params: UploadTaskParameters): Intent {
    putExtra(taskClass, taskClassString)
    putExtra(taskParameters, params)
    return this
}
