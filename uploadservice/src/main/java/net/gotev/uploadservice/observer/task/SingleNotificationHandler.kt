package net.gotev.uploadservice.observer.task

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.data.*
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractNotificationHandler(private val service: UploadService, private val notificationChannelId: String) : UploadTaskObserver {

    private val tasks = ConcurrentHashMap<String, TaskData>()

    private val notificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(notificationChannelId)
                    ?: throw IllegalArgumentException("The provided notification channel ID $notificationChannelId " +
                            "does not exist! You must create it at app startup and before Upload Service!")
        }
    }

    fun removeTask(uploadId: String) {
        tasks.remove(uploadId)
    }

    abstract fun updateNotification(
            notificationManager: NotificationManager,
            notificationBuilder: NotificationCompat.Builder,
            tasks: Map<String, TaskData>
    ): NotificationCompat.Builder?

    @Synchronized
    private fun updateTask(
            status: TaskStatus,
            info: UploadInfo,
            config: UploadNotificationStatusConfig
    ) {
        tasks[info.uploadId] = TaskData(status, info, config)

        val builder = NotificationCompat.Builder(service, notificationChannelId)
        val notification = updateNotification(notificationManager, builder, HashMap(tasks))
                ?.setGroup(UploadServiceConfig.namespace)
                ?.setOngoing(true)
                ?.build()
                ?: return
        service.holdForegroundNotification(javaClass.name, notification)
    }

    override fun onStart(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig) {
        updateTask(TaskStatus.InProgress, info, notificationConfig.progress)
    }

    override fun onProgress(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig) {
        updateTask(TaskStatus.InProgress, info, notificationConfig.progress)
    }

    override fun onSuccess(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig, response: ServerResponse) {
        updateTask(TaskStatus.Succeeded, info, notificationConfig.success)
    }

    override fun onError(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig, exception: Throwable) {
        if (exception is UserCancelledUploadException) {
            updateTask(TaskStatus.Cancelled, info, notificationConfig.cancelled)
        } else {
            updateTask(TaskStatus.Failed, info, notificationConfig.cancelled)
        }
    }

    override fun onCompleted(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig) {

    }

}