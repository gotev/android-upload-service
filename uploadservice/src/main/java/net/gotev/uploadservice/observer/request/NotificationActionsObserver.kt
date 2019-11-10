package net.gotev.uploadservice.observer.request

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig.broadcastNotificationAction
import net.gotev.uploadservice.UploadServiceConfig.broadcastNotificationActionIntentFilter
import net.gotev.uploadservice.extensions.uploadIdToCancel
import net.gotev.uploadservice.logger.UploadServiceLogger

open class NotificationActionsObserver(
    private val context: Context
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != broadcastNotificationAction) return
        onActionIntent(intent)
    }

    open fun onActionIntent(intent: Intent) {
        intent.uploadIdToCancel?.let {
            UploadServiceLogger.info(NotificationActionsObserver::class.java.simpleName) {
                "Requested cancellation of upload with ID: $it"
            }
            UploadService.stopUpload(it)
        }
    }

    fun register() {
        context.registerReceiver(this, broadcastNotificationActionIntentFilter)
        UploadServiceLogger.debug(NotificationActionsObserver::class.java.simpleName) {
            "Registered"
        }
    }

    fun unregister() {
        context.unregisterReceiver(this)
        UploadServiceLogger.debug(NotificationActionsObserver::class.java.simpleName) {
            "Unregistered"
        }
    }
}
