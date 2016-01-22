package net.gotev.uploadservice;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the configuration of the upload notification.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class UploadNotificationConfig implements Parcelable {

    private int iconResourceID;
    private String title;
    private String inProgress;
    private String completed;
    private String error;
    private boolean autoClearOnSuccess;
    private boolean clearOnAction;
    private boolean ringToneEnabled;
    private Intent clickIntent;

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
        iconResourceID = android.R.drawable.ic_menu_upload;
        title = "File Upload";
        inProgress = "Upload in progress";
        completed = "Upload completed successfully!";
        error = "Error during upload";
        autoClearOnSuccess = false;
        clearOnAction = false;
        clickIntent = null;
        ringToneEnabled = true;
    }

    /**
     * Sets the notification icon.
     * @param resourceID Resource ID of the icon to use
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setIcon(int resourceID) {
        this.iconResourceID = resourceID;
        return this;
    }

    /**
     * Sets the notification title.
     * @param title Title to show in the notification icon
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the message to be shown while upload is in progress.
     * @param message Message to show
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setInProgressMessage(String message) {
        inProgress = message;
        return this;
    }

    /**
     * Sets the message to be shown if an error occurs.
     * @param message Message to show
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setErrorMessage(String message) {
        error = message;
        return this;
    }

    /**
     * Sets the message to be shown when the upload is completed.
     * @param message Message to show
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setCompletedMessage(String message) {
        completed = message;
        return this;
    }

    /**
     * Sets whether or not to auto clear the notification when the upload is completed successfully.
     * @param clear true to automatically clear the notification, otherwise false
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setAutoClearOnSuccess(boolean clear) {
        autoClearOnSuccess = clear;
        return this;
    }

    /**
     * Sets whether or not to clear the notification when the user taps on it.
     * @param clear true to clear the notification, otherwise false
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setClearOnAction(boolean clear) {
        clearOnAction = clear;
        return this;
    }

    /**
     * Sets the intent to be executed when the user taps on the notification.
     * @param clickIntent {@link android.content.Intent}.
     *                    For example: new Intent(context, YourActivity.class)
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setClickIntent(Intent clickIntent) {
        this.clickIntent = clickIntent;
        return this;
    }

    /**
     * Sets whether or not to enable the notification sound when the upload gets completed with
     * success or error
     * @param enabled true to enable the default ringtone
     * @return {@link UploadNotificationConfig}
     */
    public final UploadNotificationConfig setRingToneEnabled(Boolean enabled) {
        this.ringToneEnabled = enabled;
        return this;
    }

    final int getIconResourceID() {
        return iconResourceID;
    }

    final String getTitle() {
        return title;
    }

    final String getInProgressMessage() {
        return inProgress;
    }

    final String getCompletedMessage() {
        return completed;
    }

    final String getErrorMessage() {
        return error;
    }

    final boolean isAutoClearOnSuccess() {
        return autoClearOnSuccess;
    }

    final boolean isClearOnAction() {
        return clearOnAction;
    }

    final boolean isRingToneEnabled() {
        return ringToneEnabled;
    }

    final PendingIntent getPendingIntent(Context context) {
        if (clickIntent == null) {
            return PendingIntent.getBroadcast(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return PendingIntent.getActivity(context, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<UploadNotificationConfig> CREATOR =
            new Parcelable.Creator<UploadNotificationConfig>() {
        @Override
        public UploadNotificationConfig createFromParcel(final Parcel in) {
            return new UploadNotificationConfig(in);
        }

        @Override
        public UploadNotificationConfig[] newArray(final int size) {
            return new UploadNotificationConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(iconResourceID);
        parcel.writeString(title);
        parcel.writeString(inProgress);
        parcel.writeString(completed);
        parcel.writeString(error);
        parcel.writeByte((byte) (autoClearOnSuccess ? 1 : 0));
        parcel.writeByte((byte) (clearOnAction ? 1 : 0));
        parcel.writeByte((byte) (ringToneEnabled ? 1 : 0));
        parcel.writeParcelable(clickIntent, 0);
    }

    private UploadNotificationConfig(Parcel in) {
        iconResourceID = in.readInt();
        title = in.readString();
        inProgress = in.readString();
        completed = in.readString();
        error = in.readString();
        autoClearOnSuccess = in.readByte() == 1;
        clearOnAction = in.readByte() == 1;
        ringToneEnabled = in.readByte() == 1;
        clickIntent = in.readParcelable(Intent.class.getClassLoader());
    }
}
