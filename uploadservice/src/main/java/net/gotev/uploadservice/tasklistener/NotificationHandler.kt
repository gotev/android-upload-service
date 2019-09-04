package net.gotev.uploadservice.tasklistener

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadNotificationConfig
import net.gotev.uploadservice.UploadNotificationStatusConfig
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.notifications.Placeholders

/**
 * @author Aleksandar Gotev
 */
class NotificationHandler(private val service: UploadService,
                          private val notificationId: Int,
                          private val uploadId: String,
                          private val config: UploadNotificationConfig) : UploadTaskListener {

    private val notificationCreationTimeMillis by lazy {
        System.currentTimeMillis()
    }

    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun initialize(info: UploadInfo) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = config.notificationChannelId ?: throw IllegalArgumentException("Notification Channel ID is required to be set on Android 8+.")
            notificationManager.getNotificationChannel(notificationChannelId) ?: throw java.lang.IllegalArgumentException("The provided notification channel ID $notificationChannelId does not exist! You must create it at app startup and before Upload Service!")
        }

        //create notification
        if (config.progress.message == null)
            return

        val statusConfig = config.progress

        val notification = NotificationCompat.Builder(service, config.notificationChannelId)
                .setWhen(notificationCreationTimeMillis)
                .setContentTitle(Placeholders.replace(statusConfig.title, info))
                .setContentText(Placeholders.replace(statusConfig.message, info))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setGroup(UploadServiceConfig.namespace)
                .setProgress(100, 0, true)
                .setOngoing(true)

        statusConfig.addActionsToNotificationBuilder(notification)

        val builtNotification = notification.build()

        if (service.holdForegroundNotification(uploadId, builtNotification)) {
            notificationManager.cancel(notificationId)
        } else {
            notificationManager.notify(notificationId, builtNotification)
        }
    }

    override fun onProgress(info: UploadInfo) {
        if (config.progress.message == null)
            return

        val statusConfig = config.progress

        val notification = NotificationCompat.Builder(service, config.notificationChannelId)
                .setWhen(notificationCreationTimeMillis)
                .setContentTitle(Placeholders.replace(statusConfig.title, info))
                .setContentText(Placeholders.replace(statusConfig.message, info))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setGroup(UploadServiceConfig.namespace)
                .setProgress(info.totalBytes.toInt(), info.uploadedBytes.toInt(), false)
                .setOngoing(true)

        statusConfig.addActionsToNotificationBuilder(notification)

        val builtNotification = notification.build()

        if (service.holdForegroundNotification(uploadId, builtNotification)) {
            notificationManager.cancel(notificationId)
        } else {
            notificationManager.notify(notificationId, builtNotification)
        }
    }

    override fun onCompleted(info: UploadInfo, response: ServerResponse) {
        if (response.isSuccessful && config.completed.message != null) {
            updateNotification(info, config.completed)

        } else if (config.error.message != null) {
            updateNotification(info, config.error)
        }
    }

    override fun onCancelled(info: UploadInfo) {
        if (config.cancelled.message != null) {
            updateNotification(info, config.cancelled)
        }
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        if (config.error.message != null) {
            updateNotification(info, config.error)
        }
    }

    private fun updateNotification(uploadInfo: UploadInfo, statusConfig: UploadNotificationStatusConfig) {
        notificationManager.cancel(notificationId)

        if (statusConfig.message == null) return

        if (!statusConfig.autoClear) {
            val notification = NotificationCompat.Builder(service, config.notificationChannelId)
                    .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                    .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                    .setContentIntent(statusConfig.getClickIntent(service))
                    .setAutoCancel(statusConfig.clearOnAction)
                    .setSmallIcon(statusConfig.iconResourceID)
                    .setLargeIcon(statusConfig.largeIcon)
                    .setColor(statusConfig.iconColorResourceID)
                    .setGroup(UploadServiceConfig.namespace)
                    .setProgress(0, 0, false)
                    .setOngoing(false)

            statusConfig.addActionsToNotificationBuilder(notification)

            setRingtone(notification)

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            uploadInfo.notificationID = notificationId + 1
            notificationManager.notify(notificationId + 1, notification.build())
        }
    }

    private fun setRingtone(notification: NotificationCompat.Builder) {
        if (config.isRingToneEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val sound = RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION)
            notification.setSound(sound)
        }
    }
}
