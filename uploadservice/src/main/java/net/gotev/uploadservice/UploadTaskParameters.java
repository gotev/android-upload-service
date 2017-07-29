package net.gotev.uploadservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Class which contains all the basic parameters passed to the upload task.
 * If you want to add more parameters, which are specific to your implementation, you should not
 * extends this class, but instead create a new class which implements {@link Parcelable} and
 * define a constant string which indicates the key used to store the serialized form of the class
 * into the intent. Look at {@link HttpUploadTaskParameters} for an example.
 *
 * @author gotev (Aleksandar Gotev)
 */
public final class UploadTaskParameters implements Parcelable {

    public String id;
    public String serverUrl;
    private int maxRetries = 0;
    public boolean autoDeleteSuccessfullyUploadedFiles = false;
    public UploadNotificationConfig notificationConfig;
    public ArrayList<UploadFile> files = new ArrayList<>();

    public UploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<UploadTaskParameters> CREATOR =
            new Creator<UploadTaskParameters>() {
                @Override
                public UploadTaskParameters createFromParcel(final Parcel in) {
                    return new UploadTaskParameters(in);
                }

                @Override
                public UploadTaskParameters[] newArray(final int size) {
                    return new UploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeString(id);
        parcel.writeString(serverUrl);
        parcel.writeInt(maxRetries);
        parcel.writeByte((byte) (autoDeleteSuccessfullyUploadedFiles ? 1 : 0));
        parcel.writeParcelable(notificationConfig, 0);
        parcel.writeList(files);
    }

    private UploadTaskParameters(Parcel in) {
        id = in.readString();
        serverUrl = in.readString();
        maxRetries = in.readInt();
        autoDeleteSuccessfullyUploadedFiles = in.readByte() == 1;
        notificationConfig = in.readParcelable(UploadNotificationConfig.class.getClassLoader());
        in.readList(files, UploadFile.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public UploadTaskParameters setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;

        return this;
    }

}
