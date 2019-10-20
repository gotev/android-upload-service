package net.gotev.uploadservice.data

import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.Placeholders

@Parcelize
class UploadNotificationConfig @JvmOverloads constructor(
    var notificationChannelId: String,
    // TODO: study how to apply this to notification channels
    var isRingToneEnabled: Boolean = true,
    val progress: UploadNotificationStatusConfig = UploadNotificationStatusConfig().apply {
        message = "Uploading at ${Placeholders.UPLOAD_RATE} (${Placeholders.PROGRESS})"
    },
    val completed: UploadNotificationStatusConfig = UploadNotificationStatusConfig().apply {
        message = "Upload completed successfully in ${Placeholders.ELAPSED_TIME}"
    },
    val error: UploadNotificationStatusConfig = UploadNotificationStatusConfig().apply {
        message = "Error during upload"
    },
    val cancelled: UploadNotificationStatusConfig = UploadNotificationStatusConfig().apply {
        message = "Upload cancelled"
    }
) : Parcelable {

    @IgnoredOnParcel
    private val allStatuses by lazy {
        arrayOf(progress, completed, error, cancelled)
    }

    /**
     * Sets the notification title for all the notification statuses.
     *
     * @param title Title to show in the notification icon
     * @return [UploadNotificationConfig]
     */
    fun setTitleForAllStatuses(title: String): UploadNotificationConfig {
        allStatuses.forEach { it.title = title }
        return this
    }

    /**
     * Sets the same notification icon for all the notification statuses.
     *
     * @param resourceID Resource ID of the icon to use
     * @return [UploadNotificationConfig]
     */
    fun setIconForAllStatuses(resourceID: Int): UploadNotificationConfig {
        allStatuses.forEach { it.iconResourceID = resourceID }
        return this
    }

    /**
     * Sets the same notification icon for all the notification statuses.
     *
     * @param iconColorResourceID Resource ID of the color to use
     * @return [UploadNotificationConfig]
     */
    fun setIconColorForAllStatuses(iconColorResourceID: Int): UploadNotificationConfig {
        allStatuses.forEach { it.iconColorResourceID = iconColorResourceID }
        return this
    }

    /**
     * Sets the same large notification icon for all the notification statuses.
     *
     * @param largeIcon Bitmap of the icon to use
     * @return [UploadNotificationConfig]
     */
    fun setLargeIconForAllStatuses(largeIcon: Bitmap): UploadNotificationConfig {
        allStatuses.forEach { it.largeIcon = largeIcon }
        return this
    }

    /**
     * Sets the same intent to be executed when the user taps on the notification
     * for all the notification statuses.
     *
     * @param clickIntent [android.app.PendingIntent] containing the user's action
     * @return [UploadNotificationConfig]
     */
    fun setClickIntentForAllStatuses(clickIntent: PendingIntent): UploadNotificationConfig {
        allStatuses.forEach { it.clickIntent = clickIntent }
        return this
    }

    /**
     * Adds the same notification action for all the notification statuses.
     * So for example, if you want to have the same action while the notification is in progress,
     * cancelled, completed or with an error, this method will save you lines of code.
     *
     * @param action [UploadNotificationAction] action to add
     * @return [UploadNotificationConfig]
     */
    fun addActionForAllStatuses(action: UploadNotificationAction): UploadNotificationConfig {
        allStatuses.forEach { it.actions.add(action) }
        return this
    }

    /**
     * Sets whether or not to clear the notification when the user taps on it
     * for all the notification statuses.
     *
     *
     * This would not affect progress notification, as it's ongoing and managed by the upload
     * service.
     *
     * @param clearOnAction true to clear the notification, otherwise false
     * @return [UploadNotificationConfig]
     */
    fun setClearOnActionForAllStatuses(clearOnAction: Boolean): UploadNotificationConfig {
        allStatuses.forEach { it.clearOnAction = clearOnAction }
        return this
    }
}
