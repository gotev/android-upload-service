package net.gotev.uploadservice;

import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * Contains the configuration of the upload notification.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class UploadNotificationConfig implements Parcelable {

    private boolean ringToneEnabled;

    /**
     * Notification channel ID
     */
    private String notificationChannelId;

    private UploadNotificationStatusConfig progress;
    private UploadNotificationStatusConfig completed;
    private UploadNotificationStatusConfig error;
    private UploadNotificationStatusConfig cancelled;

    /**
     * Creates a new upload notification configuration with default settings:
     * <ul>
     *     <li>{@code android.R.drawable.ic_menu_upload} will be used as the icon</li>
     *     <li>If the user taps on the notification, nothing will happen</li>
     *     <li>Once the operation is completed (either successfully or with an error):
     *         <ul>
     *             <li>the default notification sound will be emitted (or the default notification vibration if the device is in silent mode)</li>
     *             <li>the notification will remain in the Notification Center until the user swipes it out</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    public UploadNotificationConfig() {

        // common configuration for all the notification statuses
        ringToneEnabled = true;

        // progress notification configuration
        progress = new UploadNotificationStatusConfig();
        progress.message = "Uploading at " + Placeholders.UPLOAD_RATE + " (" + Placeholders.PROGRESS + ")";

        // completed notification configuration
        completed = new UploadNotificationStatusConfig();
        completed.message = "Upload completed successfully in " + Placeholders.ELAPSED_TIME;

        // error notification configuration
        error = new UploadNotificationStatusConfig();
        error.message = "Error during upload";

        // cancelled notification configuration
        cancelled = new UploadNotificationStatusConfig();
        cancelled.message = "Upload cancelled";
    }

    /**
     * Sets the notification title for all the notification statuses.
     * @param title Title to show in the notification icon
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setTitleForAllStatuses(String title) {
        progress.title = title;
        completed.title = title;
        error.title = title;
        cancelled.title = title;
        return this;
    }

    /**
     * Sets the same notification icon for all the notification statuses.
     * @param resourceID Resource ID of the icon to use
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setIconForAllStatuses(int resourceID) {
        progress.iconResourceID = resourceID;
        completed.iconResourceID = resourceID;
        error.iconResourceID = resourceID;
        cancelled.iconResourceID = resourceID;
        return this;
    }

    /**
     * Sets the same notification icon for all the notification statuses.
     * @param iconColorResourceID Resource ID of the color to use
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setIconColorForAllStatuses(int iconColorResourceID) {
        progress.iconColorResourceID = iconColorResourceID;
        completed.iconColorResourceID = iconColorResourceID;
        error.iconColorResourceID = iconColorResourceID;
        cancelled.iconColorResourceID = iconColorResourceID;
        return this;
    }

    /**
     * Sets the same large notification icon for all the notification statuses.
     * @param largeIcon Bitmap of the icon to use
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setLargeIconForAllStatuses(Bitmap largeIcon) {
        progress.largeIcon = largeIcon;
        completed.largeIcon = largeIcon;
        error.largeIcon = largeIcon;
        cancelled.largeIcon = largeIcon;
        return this;
    }

    /**
     * Sets the same intent to be executed when the user taps on the notification
     * for all the notification statuses.
     *
     * @param clickIntent {@link android.app.PendingIntent} containing the user's action
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setClickIntentForAllStatuses(PendingIntent clickIntent) {
        progress.clickIntent = clickIntent;
        completed.clickIntent = clickIntent;
        error.clickIntent = clickIntent;
        cancelled.clickIntent = clickIntent;
        return this;
    }

    /**
     * Adds the same notification action for all the notification statuses.
     * So for example, if you want to have the same action while the notification is in progress,
     * cancelled, completed or with an error, this method will save you lines of code.
     *
     * @param action {@link UploadNotificationAction} action to add
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig addActionForAllStatuses(UploadNotificationAction action) {
        progress.actions.add(action);
        completed.actions.add(action);
        error.actions.add(action);
        cancelled.actions.add(action);
        return this;
    }

    /**
     * Sets whether or not to clear the notification when the user taps on it
     * for all the notification statuses.
     *
     * This would not affect progress notification, as it's ongoing and managed by the upload
     * service.
     *
     * @param clearOnAction true to clear the notification, otherwise false
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setClearOnActionForAllStatuses(boolean clearOnAction) {
        progress.clearOnAction = clearOnAction;
        completed.clearOnAction = clearOnAction;
        error.clearOnAction = clearOnAction;
        cancelled.clearOnAction = clearOnAction;
        return this;
    }

    /**
     * Sets whether or not to enable the notification sound when the upload gets completed with
     * success or error.
     *
     * @param enabled true to enable the default ringtone
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setRingToneEnabled(Boolean enabled) {
        this.ringToneEnabled = enabled;
        return this;
    }

    /**
     * Sets notification channel ID
     *
     * @param channelId notification channel ID
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setNotificationChannelId(@NonNull String channelId){
        this.notificationChannelId = channelId;
        return this;
    }

    public boolean isRingToneEnabled() {
        return ringToneEnabled;
    }

    public UploadNotificationStatusConfig getProgress() {
        return progress;
    }

    public UploadNotificationStatusConfig getCompleted() {
        return completed;
    }

    public UploadNotificationStatusConfig getError() {
        return error;
    }

    public UploadNotificationStatusConfig getCancelled() {
        return cancelled;
    }

    public String getNotificationChannelId(){
        return notificationChannelId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.notificationChannelId);
        dest.writeByte(this.ringToneEnabled ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.progress, flags);
        dest.writeParcelable(this.completed, flags);
        dest.writeParcelable(this.error, flags);
        dest.writeParcelable(this.cancelled, flags);
    }

    protected UploadNotificationConfig(Parcel in) {
        this.notificationChannelId = in.readString();
        this.ringToneEnabled = in.readByte() != 0;
        this.progress = in.readParcelable(UploadNotificationStatusConfig.class.getClassLoader());
        this.completed = in.readParcelable(UploadNotificationStatusConfig.class.getClassLoader());
        this.error = in.readParcelable(UploadNotificationStatusConfig.class.getClassLoader());
        this.cancelled = in.readParcelable(UploadNotificationStatusConfig.class.getClassLoader());
    }

    public static final Creator<UploadNotificationConfig> CREATOR = new Creator<UploadNotificationConfig>() {
        @Override
        public UploadNotificationConfig createFromParcel(Parcel source) {
            return new UploadNotificationConfig(source);
        }

        @Override
        public UploadNotificationConfig[] newArray(int size) {
            return new UploadNotificationConfig[size];
        }
    };
}
