package com.alexbbb.uploadservice;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the configuration of the upload notification.
 *
 * @author alexbbb (Alex Gotev)
 */
class UploadNotificationConfig implements Parcelable {

    private final int iconResourceID;
    private final String title;
    private final String message;
    private final String completed;
    private final String error;
    private final boolean autoClearOnSuccess;
    private boolean ringTone;
    private Intent clickIntent;

    public UploadNotificationConfig() {
        iconResourceID = android.R.drawable.ic_menu_upload;
        title = "File Upload";
        message = "uploading in progress";
        completed = "upload completed successfully!";
        error = "error during upload";
        autoClearOnSuccess = false;
        clickIntent = null;
        ringTone = false;
    }

    public UploadNotificationConfig(final int iconResourceID, final String title,
                                    final String message, final String completed,
                                    final String error, final boolean autoClearOnSuccess)
            throws IllegalArgumentException {

        if (title == null || message == null || completed == null || error == null) {
            throw new IllegalArgumentException("You can't provide null parameters");
        }

        this.iconResourceID = iconResourceID;
        this.title = title;
        this.message = message;
        this.completed = completed;
        this.error = error;
        this.autoClearOnSuccess = autoClearOnSuccess;
    }

    public final int getIconResourceID() {
        return iconResourceID;
    }

    public final String getTitle() {
        return title;
    }

    public final String getMessage() {
        return message;
    }

    public final String getCompleted() {
        return completed;
    }

    public final String getError() {
        return error;
    }

    public final boolean isAutoClearOnSuccess() {
        return autoClearOnSuccess;
    }

    public final boolean isRingTone() {
        return ringTone;
    }

    public final PendingIntent getPendingIntent(Context context) {
        if (clickIntent == null) {
            return PendingIntent.getBroadcast(context, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return PendingIntent.getActivity(context, 1, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public final void setClickIntent(Intent clickIntent) {
        this.clickIntent = clickIntent;
    }
    public final void enableRingTone(Boolean tone) {
        this.ringTone = tone;
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
        parcel.writeString(message);
        parcel.writeString(completed);
        parcel.writeString(error);
        parcel.writeByte((byte) (autoClearOnSuccess ? 1 : 0));
        parcel.writeByte((byte) (ringTone ? 1 : 0));
        parcel.writeParcelable(clickIntent, 0);
    }

    private UploadNotificationConfig(Parcel in) {
        iconResourceID = in.readInt();
        title = in.readString();
        message = in.readString();
        completed = in.readString();
        error = in.readString();
        autoClearOnSuccess = in.readByte() == 1;
        ringTone = in.readByte() == 1;
        clickIntent = in.readParcelable(Intent.class.getClassLoader());
    }
}
