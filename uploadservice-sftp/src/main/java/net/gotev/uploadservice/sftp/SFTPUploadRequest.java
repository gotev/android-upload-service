package net.gotev.uploadservice.sftp;

import android.content.Context;
import android.content.Intent;

import net.gotev.uploadservice.UploadFile;
import net.gotev.uploadservice.UploadRequest;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;
import net.gotev.uploadservice.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Creates a new SFTP Upload Request.
 *
 * @author Mike Penz
 */
public class SFTPUploadRequest extends UploadRequest<SFTPUploadRequest> {

    protected final SFTPUploadTaskParameters sftpParams = new SFTPUploadTaskParameters();

    @Override
    protected Class<? extends UploadTask> getTaskClass() {
        return SFTPUploadTask.class;
    }

    /**
     * Creates a new FTP upload request.
     *
     * @param context   application context
     * @param uploadId  unique ID to assign to this upload request.<br>
     *                  It can be whatever string you want, as long as it's unique.
     *                  If you set it to null or an empty string, an UUID will be automatically
     *                  generated.<br> It's advised to keep a reference to it in your code,
     *                  so when you receive status updates in {@link UploadServiceBroadcastReceiver},
     *                  you know to which upload they refer to.
     * @param serverUrl server IP address or hostname
     * @param port      SFTP port
     */
    public SFTPUploadRequest(Context context, String uploadId, String serverUrl, int port) {
        super(context, uploadId, serverUrl);

        if (port <= 0) {
            throw new IllegalArgumentException("Specify valid SFTP port!");
        }

        sftpParams.port = port;
    }

    /**
     * Creates a new SFTP upload request and automatically generates an upload id that will
     * be returned when you call {@link UploadRequest#startUpload()}.
     *
     * @param context   application context
     * @param serverUrl server IP address or hostname
     * @param port      SFTP port
     */
    public SFTPUploadRequest(final Context context, final String serverUrl, int port) {
        this(context, null, serverUrl, port);
    }

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(SFTPUploadTaskParameters.PARAM_SFTP_TASK_PARAMETERS, sftpParams);
    }

    /**
     * Set the credentials used to login on the SFTP Server.
     *
     * @param username account username
     * @param password account password
     * @return {@link SFTPUploadRequest}
     */
    public SFTPUploadRequest setUsernameAndPassword(String username, String password) {
        if (username == null || "".equals(username)) {
            throw new IllegalArgumentException("Specify SFTP account username!");
        }

        if (password == null || "".equals(password)) {
            throw new IllegalArgumentException("Specify SFTP account password!");
        }

        sftpParams.username = username;
        sftpParams.password = password;
        return this;
    }

    /**
     * Add a file to be uploaded.
     *
     * @param filePath   path to the local file on the device
     * @param remotePath absolute path (or relative path to the default remote working directory)
     *                   of the file on the SFTP server. Valid paths are for example:
     *                   {@code /path/to/myfile.txt}, {@code relative/path/} or {@code myfile.zip}.
     *                   If any of the directories of the specified remote path does not exist,
     *                   they will be automatically created. You can also set with which permissions
     *                   to create them by using
     *                   {@link SFTPUploadRequest#setCreatedDirectoriesPermissions(UnixPermissions)}
     *                   method.
     *                   <br><br>
     *                   Remember that if the remote path ends with {@code /}, the remote file name
     *                   will be the same as the local file, so for example if I'm uploading
     *                   {@code /home/alex/photos.zip} into {@code images/} remote path, I will have
     *                   {@code photos.zip} into the remote {@code images/} directory.
     *                   <br><br>
     *                   If the remote path does not end with {@code /}, the last path segment
     *                   will be used as the remote file name, so for example if I'm uploading
     *                   {@code /home/alex/photos.zip} into {@code images/vacations.zip}, I will
     *                   have {@code vacations.zip} into the remote {@code images/} directory.
     * @return {@link SFTPUploadRequest}
     * @throws FileNotFoundException if the local file does not exist
     */
    public SFTPUploadRequest addFileToUpload(String filePath, String remotePath) throws FileNotFoundException {
        return addFileToUpload(filePath, remotePath, null);
    }

    /**
     * Add a file to be uploaded.
     *
     * @param filePath    path to the local file on the device
     * @param remotePath  absolute path (or relative path to the default remote working directory)
     *                    of the file on the SFTP server. Valid paths are for example:
     *                    {@code /path/to/myfile.txt}, {@code relative/path/} or {@code myfile.zip}.
     *                    If any of the directories of the specified remote path does not exist,
     *                    they will be automatically created. You can also set with which permissions
     *                    to create them by using
     *                    {@link SFTPUploadRequest#setCreatedDirectoriesPermissions(UnixPermissions)}
     *                    method.
     *                    <br><br>
     *                    Remember that if the remote path ends with {@code /}, the remote file name
     *                    will be the same as the local file, so for example if I'm uploading
     *                    {@code /home/alex/photos.zip} into {@code images/} remote path, I will have
     *                    {@code photos.zip} into the remote {@code images/} directory.
     *                    <br><br>
     *                    If the remote path does not end with {@code /}, the last path segment
     *                    will be used as the remote file name, so for example if I'm uploading
     *                    {@code /home/alex/photos.zip} into {@code images/vacations.zip}, I will
     *                    have {@code vacations.zip} into the remote {@code images/} directory.
     * @param permissions UNIX permissions for the uploaded file
     * @return {@link SFTPUploadRequest}
     * @throws FileNotFoundException if the local file does not exist
     */
    public SFTPUploadRequest addFileToUpload(String filePath, String remotePath, UnixPermissions permissions)
            throws FileNotFoundException {
        UploadFile file = new UploadFile(filePath);

        if (remotePath == null || remotePath.isEmpty()) {
            throw new IllegalArgumentException("You have to specify a remote path");
        }

        file.setProperty(SFTPUploadTask.PARAM_REMOTE_PATH, remotePath);

        if (permissions != null) {
            file.setProperty(SFTPUploadTask.PARAM_PERMISSIONS, permissions.toString());
        }

        params.files.add(file);
        return this;
    }

    /**
     * Add a file to be uploaded in the default working directory of the account used to login
     * into the SFTP server. The uploaded file name will be the same as the local file name, so
     * if you are uploading {@code /path/to/myfile.txt}, you will have {@code myfile.txt}
     * inside the default remote working directory.
     * If any of the directories of the specified remote path does not exist,
     * they will be automatically created. You can also set with which permissions to create them
     * by using {@link SFTPUploadRequest#setCreatedDirectoriesPermissions(UnixPermissions)}
     * method.
     *
     * @param filePath path to the local file on the device
     * @return {@link SFTPUploadRequest}
     * @throws FileNotFoundException if the local file does not exist
     */
    public SFTPUploadRequest addFileToUpload(String filePath) throws FileNotFoundException {
        UploadFile file = new UploadFile(filePath);

        file.setProperty(SFTPUploadTask.PARAM_REMOTE_PATH, new File(filePath).getName());

        params.files.add(file);
        return this;
    }

    /**
     * Sets the FTP connection timeout.
     * The default value is defined in {@link SFTPUploadTaskParameters#DEFAULT_CONNECT_TIMEOUT}.
     *
     * @param milliseconds timeout in milliseconds
     * @return {@link SFTPUploadRequest}
     */
    public SFTPUploadRequest setConnectTimeout(int milliseconds) {
        if (milliseconds < 2000) {
            throw new IllegalArgumentException("Set at least 2000ms connect timeout!");
        }

        sftpParams.connectTimeout = milliseconds;
        return this;
    }

    /**
     * Sets the UNIX permissions to set to newly created directories (if any). This may happen if
     * you upload files to directories which does not exist on your SFTP server. They will be
     * automatically created. If <b>null</b> is set here or you never call this method,
     * the default permissions for new folders set on your SFTP server will be applied.
     *
     * @param permissions UNIX permissions to set to newly created directories
     * @return {@link SFTPUploadRequest}
     */
    public SFTPUploadRequest setCreatedDirectoriesPermissions(UnixPermissions permissions) {
        if (permissions == null)
            return this;

        sftpParams.createdDirectoriesPermissions = permissions.toString();
        return this;
    }

    @Override
    public String startUpload() {
        if (params.files.isEmpty())
            throw new IllegalArgumentException("Add at least one file to start SFTP upload!");

        return super.startUpload();
    }
}
