package net.gotev.uploadservice.sftp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * SFTP upload parameters.
 *
 * @author Mike Penz
 */
public class SFTPUploadTaskParameters implements Parcelable {

    protected static final String PARAM_SFTP_TASK_PARAMETERS = "sftpTaskParameters";

    /**
     * The default SFTP connection timeout in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 15000;

    public int port;
    public String username;
    public String password;
    public int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    public String createdDirectoriesPermissions;

    public SFTPUploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<SFTPUploadTaskParameters> CREATOR =
            new Creator<SFTPUploadTaskParameters>() {
                @Override
                public SFTPUploadTaskParameters createFromParcel(final Parcel in) {
                    return new SFTPUploadTaskParameters(in);
                }

                @Override
                public SFTPUploadTaskParameters[] newArray(final int size) {
                    return new SFTPUploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(port);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeInt(connectTimeout);
        parcel.writeString(createdDirectoriesPermissions);
    }

    private SFTPUploadTaskParameters(Parcel in) {
        port = in.readInt();
        username = in.readString();
        password = in.readString();
        connectTimeout = in.readInt();
        createdDirectoriesPermissions = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
