package net.gotev.uploadservice

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import net.gotev.uploadservice.UploadServiceConfig.threadPool
import net.gotev.uploadservice.extensions.acquirePartialWakeLock
import net.gotev.uploadservice.extensions.getUploadTask
import net.gotev.uploadservice.extensions.getUploadTaskCreationParameters
import net.gotev.uploadservice.extensions.safeRelease
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.logger.UploadServiceLogger.NA
import net.gotev.uploadservice.observer.task.BroadcastEmitter
import net.gotev.uploadservice.observer.task.TaskCompletionNotifier
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class UploadService : Service() {

    companion object {
        internal val TAG = UploadService::class.java.simpleName

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
        @JvmStatic
        fun stopUpload(uploadId: String) {
            uploadTasksMap[uploadId]?.cancel()
        }

        /**
         * Gets the list of the currently active upload tasks.
         * @return list of uploadIDs or an empty list if no tasks are currently running
         */
        @JvmStatic
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
        @JvmStatic
        fun stopAllUploads() {
            val iterator = uploadTasksMap.keys.iterator()

            while (iterator.hasNext()) {
                uploadTasksMap[iterator.next()]?.cancel()
            }
        }

        /**
         * Stops the service.
         * @param context application context
         * @param forceStop if true stops the service no matter if some tasks are running, else
         * stops only if there aren't any active tasks
         * @return true if the service is getting stopped, false otherwise
         */
        @Synchronized
        @JvmOverloads
        @JvmStatic
        fun stop(context: Context, forceStop: Boolean = false) = if (forceStop) {
            stopAllUploads()
            context.stopService(Intent(context, UploadService::class.java))
        } else {
            uploadTasksMap.isEmpty() && context.stopService(
                Intent(
                    context,
                    UploadService::class.java
                )
            )
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var idleTimer: Timer? = null

    private val taskObservers by lazy {
        arrayOf(
            BroadcastEmitter(this),
            UploadServiceConfig.notificationHandlerFactory(this),
            TaskCompletionNotifier(this)
        )
    }

    private val notificationActionsObserver by lazy {
        UploadServiceConfig.notificationActionsObserverFactory(this)
    }

    @Synchronized
    private fun clearIdleTimer() {
        idleTimer?.apply {
            UploadServiceLogger.info(TAG, NA) { "Clearing idle timer" }
            cancel()
        }
        idleTimer = null
    }

    @Synchronized
    private fun shutdownIfThereArentAnyActiveTasks(): Int {
        if (uploadTasksMap.isEmpty()) {
            clearIdleTimer()

            UploadServiceLogger.info(TAG, NA) {
                "Service will be shut down in ${UploadServiceConfig.idleTimeoutSeconds}s " +
                    "if no new tasks are received"
            }

            idleTimer = Timer(TAG + "IdleTimer").apply {
                schedule(
                    object : TimerTask() {
                        override fun run() {
                            UploadServiceLogger.info(TAG, NA) {
                                "Service is about to be stopped because idle timeout of " +
                                    "${UploadServiceConfig.idleTimeoutSeconds}s has been reached"
                            }
                            stopSelf()
                        }
                    },
                    (UploadServiceConfig.idleTimeoutSeconds * 1000).toLong()
                )
            }

            return START_NOT_STICKY
        }

        return START_STICKY
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
            UploadServiceLogger.debug(TAG, uploadId) { "now holds foreground notification" }
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
            UploadServiceLogger.debug(TAG, uploadId) { "now un-holded foreground notification" }
            foregroundUploadId = null
        }

        if (UploadServiceConfig.isForegroundService && uploadTasksMap.isEmpty()) {
            UploadServiceLogger.debug(TAG, NA) { "All tasks completed, stopping foreground execution" }
            stopForeground(true)
            shutdownIfThereArentAnyActiveTasks()
        }
    }

    override fun onCreate() {
        super.onCreate()

        wakeLock = acquirePartialWakeLock(wakeLock, TAG)
        notificationActionsObserver.register()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        UploadServiceLogger.debug(TAG, NA) {
            "Starting UploadService. Debug info: $UploadServiceConfig"
        }

        val taskCreationParameters = intent.getUploadTaskCreationParameters()
            ?: return shutdownIfThereArentAnyActiveTasks()

        if (uploadTasksMap.containsKey(taskCreationParameters.params.id)) {
            UploadServiceLogger.error(TAG, taskCreationParameters.params.id) {
                "Preventing upload! An upload with the same ID is already in progress. " +
                    "Every upload must have unique ID. Please check your code and fix it!"
            }
            return shutdownIfThereArentAnyActiveTasks()
        }

        // increment by 2 because the notificationIncrementalId + 1 is used internally
        // in each UploadTask. Check its sources for more info about this.
        notificationIncrementalId += 2

        val currentTask = getUploadTask(
            creationParameters = taskCreationParameters,
            notificationId = UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId,
            observers = taskObservers
        ) ?: return shutdownIfThereArentAnyActiveTasks()

        clearIdleTimer()

        uploadTasksMap[currentTask.params.id] = currentTask
        threadPool.execute(currentTask)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        notificationActionsObserver.unregister()
        stopAllUploads()

        if (UploadServiceConfig.isForegroundService) {
            UploadServiceLogger.debug(TAG, NA) { "Stopping foreground execution" }
            stopForeground(true)
        }

        wakeLock.safeRelease()

        uploadTasksMap.clear()

        UploadServiceLogger.debug(TAG, NA) { "UploadService destroyed" }
    }
}
