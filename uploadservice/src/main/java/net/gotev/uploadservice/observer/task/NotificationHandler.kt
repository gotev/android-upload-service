package net.gotev.uploadservice.observer.task

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.UploadServiceConfig.localizationProvider
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse

class NotificationHandler(
    private val service: UploadService,
    private val notificationId: Int,
    private val uploadId: String,
    private val config: UploadNotificationConfig
) : UploadTaskObserver {

    private val notificationCreationTimeMillis by lazy { System.currentTimeMillis() }
    private val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun NotificationCompat.Builder.setRingtoneCompat(): NotificationCompat.Builder {
        if (config.isRingToneEnabled && Build.VERSION.SDK_INT < 26) {
            val sound = RingtoneManager.getActualDefaultRingtoneUri(
                service,
                RingtoneManager.TYPE_NOTIFICATION
            )
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

    private fun NotificationCompat.Builder.setCommonParameters(
        statusConfig: UploadNotificationStatusConfig,
        info: UploadInfo
    ): NotificationCompat.Builder {
        return setGroup(UploadServiceConfig.namespace)
            .setContentTitle(localizationProvider.processPlaceholders(statusConfig.title, info))
            .setContentText(localizationProvider.processPlaceholders(statusConfig.message, info))
            .setContentIntent(statusConfig.getClickIntent(service))
            .setSmallIcon(statusConfig.iconResourceID)
            .setLargeIcon(statusConfig.largeIcon)
            .setColor(statusConfig.iconColorResourceID)
            .apply {
                statusConfig.addActionsToNotificationBuilder(this)
            }
    }

    private fun ongoingNotification(
        info: UploadInfo,
        statusConfig: UploadNotificationStatusConfig
    ): NotificationCompat.Builder? {
        if (statusConfig.message == null) return null

        return NotificationCompat.Builder(service, config.notificationChannelId)
            .setWhen(notificationCreationTimeMillis)
            .setCommonParameters(statusConfig, info)
            .setOngoing(true)
    }

    private fun updateNotification(info: UploadInfo, statusConfig: UploadNotificationStatusConfig) {
        notificationManager.cancel(notificationId)

        if (statusConfig.message == null || statusConfig.autoClear) return

        val notification = NotificationCompat.Builder(service, config.notificationChannelId)
            .setCommonParameters(statusConfig, info)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(statusConfig.clearOnAction)
            .setRingtoneCompat()
            .build()

        // this is needed because the main notification used to show progress is ongoing
        // and a new one has to be created to allow the user to dismiss it
        notificationManager.notify(notificationId + 1, notification)
    }

    override fun initialize(info: UploadInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(config.notificationChannelId)
                ?: throw IllegalArgumentException("The provided notification channel ID ${config.notificationChannelId} does not exist! You must create it at app startup and before Upload Service!")
        }

        ongoingNotification(info, config.progress)
            ?.setProgress(100, 0, true)
            ?.notify()
    }

    override fun onProgress(info: UploadInfo) {
        ongoingNotification(info, config.progress)
            ?.setProgress(100, info.progressPercent, false)
            ?.notify()
    }

    override fun onSuccess(info: UploadInfo, response: ServerResponse) {
        updateNotification(info, config.completed)
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        val statusConfig = if (exception is UserCancelledUploadException) {
            config.cancelled
        } else {
            config.error
        }

        updateNotification(info, statusConfig)
    }

    override fun onCompleted(info: UploadInfo) {}
}
