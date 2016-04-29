package net.gotev.uploadservice.ftp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * FTP upload parameters.
 * @author Aleksandar Gotev
 */
public class FTPUploadTaskParameters implements Parcelable {

    protected static final String PARAM_FTP_TASK_PARAMETERS = "ftpTaskParameters";

    /**
     * The default FTP connection timeout in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 15000;

    /**
     * The default FTP socket timeout in milliseconds.
     */
    public static final int DEFAULT_SOCKET_TIMEOUT = 30000;

    private int port;
    private String username;
    private String password;
    private int connectTimeout;
    private int socketTimeout;
    private boolean compressedFileTransfer;
    private String createdDirectoriesPermissions;

    public FTPUploadTaskParameters() {

    }

    // This is used to regenerate the object.
    // All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<FTPUploadTaskParameters> CREATOR =
            new Creator<FTPUploadTaskParameters>() {
                @Override
                public FTPUploadTaskParameters createFromParcel(final Parcel in) {
                    return new FTPUploadTaskParameters(in);
                }

                @Override
                public FTPUploadTaskParameters[] newArray(final int size) {
                    return new FTPUploadTaskParameters[size];
                }
            };

    @Override
    public void writeToParcel(Parcel parcel, int arg1) {
        parcel.writeInt(port);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeInt(connectTimeout);
        parcel.writeInt(socketTimeout);
        parcel.writeByte((byte) (compressedFileTransfer ? 1 : 0));
        parcel.writeString(createdDirectoriesPermissions);
    }

    private FTPUploadTaskParameters(Parcel in) {
        port = in.readInt();
        username = in.readString();
        password = in.readString();
        connectTimeout = in.readInt();
        socketTimeout = in.readInt();
        compressedFileTransfer = in.readByte() == 1;
        createdDirectoriesPermissions = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getPort() {
        return port;
    }

    public FTPUploadTaskParameters setPort(int port) {
        this.port = port;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public FTPUploadTaskParameters setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public FTPUploadTaskParameters setPassword(String password) {
        this.password = password;
        return this;
    }

    public int getConnectTimeout() {
        return connectTimeout <= 0 ? DEFAULT_CONNECT_TIMEOUT : socketTimeout;
    }

    public FTPUploadTaskParameters setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public int getSocketTimeout() {
        return socketTimeout <= 0 ? DEFAULT_SOCKET_TIMEOUT : socketTimeout;
    }

    public FTPUploadTaskParameters setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public boolean isCompressedFileTransfer() {
        return compressedFileTransfer;
    }

    public FTPUploadTaskParameters setCompressedFileTransfer(boolean compressedFileTransfer) {
        this.compressedFileTransfer = compressedFileTransfer;
        return this;
    }

    public String getCreatedDirectoriesPermissions() {
        return createdDirectoriesPermissions;
    }

    public FTPUploadTaskParameters setCreatedDirectoriesPermissions(String createdDirectoriesPermissions) {
        this.createdDirectoriesPermissions = createdDirectoriesPermissions;
        return this;
    }
}
