package net.gotev.uploadservice.observer.task

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
                          private val config: UploadNotificationConfig) : UploadTaskObserver {

    private val notificationCreationTimeMillis by lazy { System.currentTimeMillis() }
    private val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun NotificationCompat.Builder.setRingtoneCompat(): NotificationCompat.Builder {
        if (config.isRingToneEnabled && Build.VERSION.SDK_INT < 26) {
            val sound = RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION)
            setSound(sound)
        }

        return this
    }

    private fun NotificationCompat.Builder.notify() {
        build().apply {
            if (service.holdForegroundNotification(uploadId, this)) {
                notificationManager.cancel(notificationId)
            } else {
                notificationManager.notify(notificationId, this)
            }
        }
    }

    private fun NotificationCompat.Builder.setCommonParameters(statusConfig: UploadNotificationStatusConfig, info: UploadInfo): NotificationCompat.Builder {
        return setGroup(UploadServiceConfig.namespace)
                .setContentTitle(Placeholders.replace(statusConfig.title, info))
                .setContentText(Placeholders.replace(statusConfig.message, info))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .apply {
                    statusConfig.addActionsToNotificationBuilder(this)
                }
    }

    private fun ongoingNotification(info: UploadInfo, statusConfig: UploadNotificationStatusConfig): NotificationCompat.Builder {
        return NotificationCompat.Builder(service, config.notificationChannelId)
                .setWhen(notificationCreationTimeMillis)
                .setCommonParameters(statusConfig, info)
                .setOngoing(true)
    }

    private fun updateNotification(info: UploadInfo, statusConfig: UploadNotificationStatusConfig) {
        notificationManager.cancel(notificationId)

        if (statusConfig.message == null) return

        if (!statusConfig.autoClear) {
            val notification = NotificationCompat.Builder(service, config.notificationChannelId)
                    .setCommonParameters(statusConfig, info)
                    .setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(statusConfig.clearOnAction)
                    .setRingtoneCompat()
                    .build()

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            info.notificationID = notificationId + 1
            notificationManager.notify(notificationId + 1, notification)
        }
    }

    override fun initialize(info: UploadInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = config.notificationChannelId
                    ?: throw IllegalArgumentException("Notification Channel ID is required to be set on Android 8+.")

            notificationManager.getNotificationChannel(notificationChannelId)
                    ?: throw IllegalArgumentException("The provided notification channel ID $notificationChannelId does not exist! You must create it at app startup and before Upload Service!")
        }

        if (config.progress.message == null) return

        ongoingNotification(info, config.progress)
                .setProgress(100, 0, true)
                .notify()
    }

    override fun onProgress(info: UploadInfo) {
        if (config.progress.message == null) return

        ongoingNotification(info, config.progress)
                .setProgress(100, info.progressPercent, false)
                .notify()
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
}
