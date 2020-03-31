package net.gotev.uploadservice.observer.task

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig.namespace
import net.gotev.uploadservice.UploadServiceConfig.placeholdersProcessor
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.extensions.validateNotificationChannel
import net.gotev.uploadservice.network.ServerResponse

class NotificationHandler(private val service: UploadService) : UploadTaskObserver {

    private val notificationCreationTimeMillis by lazy { System.currentTimeMillis() }

    private val notificationManager by lazy {
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun NotificationCompat.Builder.addActions(config: UploadNotificationStatusConfig): NotificationCompat.Builder {
        config.actions.forEach { addAction(it.asAction()) }
        return this
    }

    private fun NotificationCompat.Builder.setRingtoneCompat(isRingToneEnabled: Boolean): NotificationCompat.Builder {
        if (isRingToneEnabled && Build.VERSION.SDK_INT < 26) {
            setSound(
                RingtoneManager.getActualDefaultRingtoneUri(
                    service,
                    RingtoneManager.TYPE_NOTIFICATION
                )
            )
        }

        return this
    }

    private fun NotificationCompat.Builder.notify(uploadId: String, notificationId: Int) {
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
        return setGroup(namespace)
            .setContentTitle(placeholdersProcessor.processPlaceholders(statusConfig.title, info))
            .setContentText(placeholdersProcessor.processPlaceholders(statusConfig.message, info))
            .setContentIntent(statusConfig.getClickIntent(service))
            .setSmallIcon(statusConfig.iconResourceID)
            .setLargeIcon(statusConfig.largeIcon)
            .setColor(statusConfig.iconColorResourceID)
            .addActions(statusConfig)
    }

    private fun ongoingNotification(
        notificationConfig: UploadNotificationConfig,
        info: UploadInfo
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(service, notificationConfig.notificationChannelId)
            .setWhen(notificationCreationTimeMillis)
            .setCommonParameters(notificationConfig.progress, info)
            .setOngoing(true)
    }

    private fun NotificationCompat.Builder.setDeleteIntentIfPresent(
        intent: PendingIntent?
    ): NotificationCompat.Builder {
        return intent?.let { setDeleteIntent(it) } ?: this
    }

    private fun updateNotification(
        notificationId: Int,
        info: UploadInfo,
        notificationChannelId: String,
        isRingToneEnabled: Boolean,
        statusConfig: UploadNotificationStatusConfig
    ) {
        notificationManager.cancel(notificationId)

        if (statusConfig.autoClear) return

        val notification = NotificationCompat.Builder(service, notificationChannelId)
            .setCommonParameters(statusConfig, info)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setDeleteIntentIfPresent(statusConfig.onDismissed)
            .setAutoCancel(statusConfig.clearOnAction)
            .setRingtoneCompat(isRingToneEnabled)
            .build()

        // this is needed because the main notification used to show progress is ongoing
        // and a new one has to be created to allow the user to dismiss it
        notificationManager.notify(notificationId + 1, notification)
    }

    override fun onStart(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
        notificationManager.validateNotificationChannel(notificationConfig.notificationChannelId)

        ongoingNotification(notificationConfig, info)
            .setProgress(100, 0, true)
            .notify(info.uploadId, notificationId)
    }

    override fun onProgress(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
        ongoingNotification(notificationConfig, info)
            .setProgress(100, info.progressPercent, false)
            .notify(info.uploadId, notificationId)
    }

    override fun onSuccess(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        response: ServerResponse
    ) {
        updateNotification(
            notificationId,
            info,
            notificationConfig.notificationChannelId,
            notificationConfig.isRingToneEnabled,
            notificationConfig.success
        )
    }

    override fun onError(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        exception: Throwable
    ) {
        val statusConfig = if (exception is UserCancelledUploadException) {
            notificationConfig.cancelled
        } else {
            notificationConfig.error
        }

        updateNotification(
            notificationId,
            info,
            notificationConfig.notificationChannelId,
            notificationConfig.isRingToneEnabled,
            statusConfig
        )
    }

    override fun onCompleted(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
    }
}
