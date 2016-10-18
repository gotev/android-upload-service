package net.gotev.uploadservice.ftp;

import android.content.Intent;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.UploadFile;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Implements the FTP upload logic.
 * @author Aleksandar Gotev
 */
public class FTPUploadTask extends UploadTask implements CopyStreamListener {

    private static final String LOG_TAG = FTPUploadTask.class.getSimpleName();

    // properties associated to each file
    protected static final String PARAM_REMOTE_PATH = "ftpRemotePath";
    protected static final String PARAM_PERMISSIONS = "ftpPermissions";

    private FTPUploadTaskParameters ftpParams = null;
    private FTPClient ftpClient = null;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        this.ftpParams = intent.getParcelableExtra(FTPUploadTaskParameters.PARAM_FTP_TASK_PARAMETERS);
    }

    @Override
    protected void upload() throws Exception {
        try {
            ftpClient = new FTPClient();
            ftpClient.setBufferSize(UploadService.BUFFER_SIZE);
            ftpClient.setCopyStreamListener(this);
            ftpClient.setDefaultTimeout(ftpParams.getConnectTimeout());
            ftpClient.setConnectTimeout(ftpParams.getConnectTimeout());
            ftpClient.setAutodetectUTF8(true);

            Logger.debug(LOG_TAG, "Connect timeout set to " + ftpParams.getConnectTimeout() + "ms");

            Logger.debug(LOG_TAG, "Connecting to " + params.getServerUrl()
                                  + ":" + ftpParams.getPort() + " as " + ftpParams.getUsername());
            ftpClient.connect(params.getServerUrl(), ftpParams.getPort());

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new Exception("Can't connect to " + params.getServerUrl()
                                    + ":" + ftpParams.getPort()
                                    + ". The server response is: " + ftpClient.getReplyString());
            }

            if (!ftpClient.login(ftpParams.getUsername(), ftpParams.getPassword())) {
                throw new Exception("Error while performing login on " + params.getServerUrl()
                                    + ":" + ftpParams.getPort()
                                    + " with username: " + ftpParams.getUsername()
                                    + ". Check your credentials and try again.");
            }

            // to prevent the socket timeout on the control socket during file transfer,
            // set the control keep alive timeout to a half of the socket timeout
            int controlKeepAliveTimeout = ftpParams.getSocketTimeout() / 2 / 1000;

            ftpClient.setSoTimeout(ftpParams.getSocketTimeout());
            ftpClient.setControlKeepAliveTimeout(controlKeepAliveTimeout);
            ftpClient.setControlKeepAliveReplyTimeout(controlKeepAliveTimeout * 1000);

            Logger.debug(LOG_TAG, "Socket timeout set to " + ftpParams.getSocketTimeout()
                         + "ms. Enabled control keep alive every " + controlKeepAliveTimeout + "s");

            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.setFileTransferMode(ftpParams.isCompressedFileTransfer() ?
                                          FTP.COMPRESSED_TRANSFER_MODE : FTP.STREAM_TRANSFER_MODE);

            // this is needed to calculate the total bytes and the uploaded bytes, because if the
            // request fails, the upload method will be called again
            // (until max retries is reached) to retry the upload, so it's necessary to
            // know at which status we left, to be able to properly notify firther progress.
            calculateUploadedAndTotalBytes();

            String baseWorkingDir = ftpClient.printWorkingDirectory();
            Logger.debug(LOG_TAG, "FTP default working directory is: " + baseWorkingDir);

            Iterator<UploadFile> iterator = params.getFiles().iterator();
            while (iterator.hasNext()) {
                UploadFile file = iterator.next();

                if (!shouldContinue)
                    break;

                uploadFile(baseWorkingDir, file);
                addSuccessfullyUploadedFile(file.getName(service));
                iterator.remove();
            }

            // Broadcast completion only if the user has not cancelled the operation.
            if (shouldContinue) {
                broadcastCompleted(UploadTask.TASK_COMPLETED_SUCCESSFULLY,
                                   UploadTask.EMPTY_RESPONSE, null);
            }

        } finally {
            if (ftpClient.isConnected()) {
                try {
                    Logger.debug(LOG_TAG, "Logout and disconnect from FTP server: "
                                          + params.getServerUrl() + ":" + ftpParams.getPort());
                    ftpClient.logout();
                    ftpClient.disconnect();
                } catch (Exception exc) {
                    Logger.error(LOG_TAG, "Error while closing FTP connection to: "
                                          + params.getServerUrl() + ":" + ftpParams.getPort(), exc);
                }
            }
            ftpClient = null;
        }
    }

    /**
     * Calculates the total bytes of this upload task.
     * This the sum of all the lengths of the successfully uploaded files and also the pending
     * ones.
     */
    private void calculateUploadedAndTotalBytes() {
        uploadedBytes = 0;

        for (String filePath : getSuccessfullyUploadedFiles()) {
            uploadedBytes += new File(filePath).length();
        }

        totalBytes = uploadedBytes;

        for (UploadFile file : params.getFiles()) {
            totalBytes += file.length(service);
        }
    }

    private void uploadFile(String baseWorkingDir, UploadFile file) throws IOException {
        Logger.debug(LOG_TAG, "Starting FTP upload of: " + file.getName(service)
                              + " to: " + file.getProperty(PARAM_REMOTE_PATH));

        String remoteDestination = file.getProperty(PARAM_REMOTE_PATH);

        if (remoteDestination.startsWith(baseWorkingDir)) {
            remoteDestination = remoteDestination.replace(baseWorkingDir, "");
        }

        makeDirectories(remoteDestination, ftpParams.getCreatedDirectoriesPermissions());

        InputStream localStream = file.getStream(service);
        try {
            String remoteFileName = getRemoteFileName(file);
            if (!ftpClient.storeFile(remoteFileName, localStream)) {
                throw new IOException("Error while uploading: " + file.getName(service)
                                      + " to: " + file.getProperty(PARAM_REMOTE_PATH));
            }

            setPermission(remoteFileName, file.getProperty(PARAM_PERMISSIONS));

        } finally {
            localStream.close();
        }

        // get back to base working directory
        if (!ftpClient.changeWorkingDirectory(baseWorkingDir)) {
            Logger.info(LOG_TAG, "Can't change working directory to: " + baseWorkingDir);
        }
    }

    private void setPermission(String remoteFileName, String permissions) {
        if (permissions == null || "".equals(permissions))
            return;

        // http://stackoverflow.com/questions/12741938/how-can-i-change-permissions-of-a-file-on-a-ftp-server-using-apache-commons-net
        try {
            if (ftpClient.sendSiteCommand("chmod " + permissions + " " + remoteFileName)) {
                Logger.error(LOG_TAG, "Error while setting permissions for: "
                        + remoteFileName + " to: " + permissions
                        + ". Check if your FTP user can set file permissions!");
            } else {
                Logger.debug(LOG_TAG, "Permissions for: " + remoteFileName + " set to: " + permissions);
            }
        } catch (IOException exc) {
            Logger.error(LOG_TAG, "Error while setting permissions for: "
                    + remoteFileName + " to: " + permissions
                    + ". Check if your FTP user can set file permissions!", exc);
        }
    }

    @Override
    public void bytesTransferred(CopyStreamEvent event) {
    }

    @Override
    public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
        uploadedBytes += bytesTransferred;
        broadcastProgress(uploadedBytes, totalBytes);

        if (!shouldContinue) {
            try {
                ftpClient.disconnect();
            } catch (Exception exc) {
                Logger.error(LOG_TAG, "Failed to abort current file transfer", exc);
            }
        }
    }

    /**
     * Creates a nested directory structure on a FTP server and enters into it.
     * @param dirPath Path of the directory, i.e /projects/java/ftp/demo
     * @param permissions UNIX permissions to apply to created directories. If null, the FTP
     *                    server defaults will be applied, because no UNIX permissions will be
     *                    explicitly set
     * @throws IOException if any error occurred during client-server communication
     */
    private void makeDirectories(String dirPath, String permissions) throws IOException {
        if (!dirPath.contains("/")) return;

        String[] pathElements = dirPath.split("/");

        if (pathElements.length == 1) return;

        // if the string ends with / it means that the dir path contains only directories,
        // otherwise if it does not contain /, the last element of the path is the file name,
        // so it must be ignored when creating the directory structure
        int lastElement = dirPath.endsWith("/") ? pathElements.length : pathElements.length - 1;

        for (int i = 0; i < lastElement; i++) {
            String singleDir = pathElements[i];
            if (singleDir.isEmpty()) continue;

            if (!ftpClient.changeWorkingDirectory(singleDir)) {
                if (ftpClient.makeDirectory(singleDir)) {
                    Logger.debug(LOG_TAG, "Created remote directory: " + singleDir);
                    if (permissions != null) {
                        setPermission(singleDir, permissions);
                    }
                    ftpClient.changeWorkingDirectory(singleDir);
                } else {
                    throw new IOException("Unable to create remote directory: " + singleDir);
                }
            }
        }
    }

    /**
     * Checks if the remote file path contains also the remote file name. If it's not specified,
     * the name of the local file will be used.
     * @param file file to upload
     * @return remote file name
     */
    private String getRemoteFileName(UploadFile file) {

        // if the remote path ends with /
        // it means that the remote path specifies only the directory structure, so
        // get the remote file name from the local file
        if (file.getProperty(PARAM_REMOTE_PATH).endsWith("/")) {
            return file.getName(service);
        }

        // if the remote path contains /, but it's not the last character
        // it means that I have something like: /path/to/myfilename
        // so the remote file name is the last path element (myfilename in this example)
        if (file.getProperty(PARAM_REMOTE_PATH).contains("/")) {
            String[] tmp = file.getProperty(PARAM_REMOTE_PATH).split("/");
            return tmp[tmp.length - 1];
        }

        // if the remote path does not contain /, it means that it specifies only
        // the remote file name
        return file.getProperty(PARAM_REMOTE_PATH);
    }
}
