package net.gotev.uploadservice.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import kotlinx.android.parcel.Parcelize
import java.util.ArrayList

@Parcelize
data class UploadNotificationStatusConfig @JvmOverloads constructor(
    /**
     * Notification title.
     */
    val title: String,

    /**
     * Notification message.
     */
    val message: String,

    /**
     * Notification icon.
     */
    @DrawableRes val iconResourceID: Int = android.R.drawable.ic_menu_upload,

    /**
     * Icon color tint.
     */
    @ColorInt val iconColorResourceID: Int = NotificationCompat.COLOR_DEFAULT,

    /**
     * Large notification icon.
     */
    val largeIcon: Bitmap? = null,

    /**
     * Intent to be performed when the user taps on the notification.
     */
    val clickIntent: PendingIntent? = null,

    /**
     * List of actions to be added to this notification.
     */
    val actions: ArrayList<UploadNotificationAction> = ArrayList(3),

    /**
     * Clear the notification automatically when the clickIntent is performed.
     * This would not affect progress notification, as it's ongoing and managed by upload service.
     */
    val clearOnAction: Boolean = false,

    /**
     * Clear the notification automatically.
     * This would not affect progress notification, as it's ongoing and managed by upload service.
     * It's used to be able to automatically dismiss cancelled, error or success notifications.
     */
    val autoClear: Boolean = false,

    /**
     * Intent to be performed when the user swipes away the notification or clears all notifications.
     * Only applied to cancelled, error or success notifications.
     */
    val onDismissed: PendingIntent? = null
) : Parcelable {
    fun getClickIntent(context: Context): PendingIntent {
        return clickIntent ?: PendingIntent.getBroadcast(
            context,
            0,
            Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
