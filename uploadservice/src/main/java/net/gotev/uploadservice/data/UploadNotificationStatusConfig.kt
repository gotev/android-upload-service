package net.gotev.uploadservice.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import java.util.ArrayList
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UploadNotificationStatusConfig(
    /**
     * Notification title.
     */
    var title: String? = "File Upload",

    /**
     * Notification message.
     */
    var message: String? = null,

    /**
     * Clear the notification automatically.
     * This would not affect progress notification, as it's ongoing and managed by upload service.
     * It's used to be able to automatically dismiss cancelled, error or success notifications.
     */
    var autoClear: Boolean = false,

    /**
     * Notification icon.
     */
    @DrawableRes var iconResourceID: Int = android.R.drawable.ic_menu_upload,

    /**
     * Large notification icon.
     */
    var largeIcon: Bitmap? = null,

    /**
     * Icon color tint.
     */
    @ColorInt var iconColorResourceID: Int = NotificationCompat.COLOR_DEFAULT,

    /**
     * Intent to be performed when the user taps on the notification.
     */
    var clickIntent: PendingIntent? = null,

    /**
     * Clear the notification automatically when the clickIntent is performed.
     * This would not affect progress notification, as it's ongoing and managed by upload service.
     */
    var clearOnAction: Boolean = false,

    /**
     * List of actions to be added to this notification.
     */
    var actions: ArrayList<UploadNotificationAction> = ArrayList(3)
) : Parcelable {
    fun getClickIntent(context: Context): PendingIntent {
        return clickIntent ?: PendingIntent.getBroadcast(
            context,
            0,
            Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun addActionsToNotificationBuilder(builder: NotificationCompat.Builder) {
        actions.forEach {
            builder.addAction(it.asAction())
        }
    }
}
