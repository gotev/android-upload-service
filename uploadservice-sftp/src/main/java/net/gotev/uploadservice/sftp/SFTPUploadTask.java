package net.gotev.uploadservice.sftp;

import android.content.Intent;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadFile;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * Implements the SFTP upload logic.
 *
 * @author Mike Penz
 */
public class SFTPUploadTask extends UploadTask implements SftpProgressMonitor {

    private static final String LOG_TAG = SFTPUploadTask.class.getSimpleName();

    // properties associated to each file
    protected static final String PARAM_REMOTE_PATH = "sftpRemotePath";
    protected static final String PARAM_PERMISSIONS = "sftpPermissions";

    private SFTPUploadTaskParameters sftpParams = null;

    private JSch jsch = new JSch();
    private Session session = null;
    private Channel channel;
    private ChannelSftp channelSftp;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        this.sftpParams = intent.getParcelableExtra(SFTPUploadTaskParameters.PARAM_SFTP_TASK_PARAMETERS);
    }

    @Override
    protected void upload() throws Exception {
        Exception exx = null;
        try {
            try {
                session = jsch.getSession(sftpParams.username, params.serverUrl, sftpParams.port);
            } catch (Exception ex) {
                throw new Exception("Can't connect to " + params.serverUrl
                        + ":" + sftpParams.port
                        + ". The server response is: ...");
            }

            try {
                session.setPassword(sftpParams.password);
            } catch (Exception ex) {
                throw new Exception("Error while performing login on " + params.serverUrl
                        + ":" + sftpParams.port
                        + " with username: " + sftpParams.username
                        + ". Check your credentials and try again.");
            }

            session.setTimeout(sftpParams.connectTimeout);
            Logger.debug(LOG_TAG, "Connect timeout set to " + sftpParams.connectTimeout + "ms");

            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            prop.put("PreferredAuthentications", "password");
            session.setConfig(prop);

            Logger.debug(LOG_TAG, "Connecting to " + params.serverUrl
                    + ":" + sftpParams.port + " as " + sftpParams.username);
            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();

            channelSftp = (ChannelSftp) channel;

            // this is needed to calculate the total bytes and the uploaded bytes, because if the
            // request fails, the upload method will be called again
            // (until max retries is reached) to retry the upload, so it's necessary to
            // know at which status we left, to be able to properly notify firther progress.
            calculateUploadedAndTotalBytes();

            String baseWorkingDir = channelSftp.pwd();
            Logger.debug(LOG_TAG, "SFTP default working directory is: " + baseWorkingDir);

            Iterator<UploadFile> iterator = new ArrayList<>(params.files).iterator();
            while (iterator.hasNext()) {
                UploadFile file = iterator.next();

                if (!shouldContinue)
                    break;

                uploadFile(baseWorkingDir, file);
                Logger.debug(LOG_TAG, "addSuccessfullyUploadedFile: " + file);
                addSuccessfullyUploadedFile(file);
                iterator.remove();
            }

            // Broadcast completion only if the user has not cancelled the operation.
            if (shouldContinue) {
                broadcastCompleted(new ServerResponse(UploadTask.TASK_COMPLETED_SUCCESSFULLY,
                        UploadTask.EMPTY_RESPONSE, null));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            exx = ex;
        } finally {
            if (channel != null && channel.isConnected()) {
                try {
                    Logger.debug(LOG_TAG, "Logout and disconnect from SFTP server: "
                            + params.serverUrl + ":" + sftpParams.port);
                    channel.disconnect();
                } catch (Exception exc) {
                    Logger.error(LOG_TAG, "Error while closing SFTP connection to: "
                            + params.serverUrl + ":" + sftpParams.port, exc);
                }
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }

            channel = null;
            channelSftp = null;
            session = null;
        }
        if (exx != null) {
            throw exx;
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

        for (UploadFile file : params.files) {
            totalBytes += file.length(service);
        }
    }

    private void uploadFile(String baseWorkingDir, UploadFile file) throws IOException {
        Logger.debug(LOG_TAG, "Starting SFTP upload of: " + file.getName(service)
                + " to: " + file.getProperty(PARAM_REMOTE_PATH));

        String remoteDestination = file.getProperty(PARAM_REMOTE_PATH);

        if (remoteDestination.startsWith(baseWorkingDir)) {
            remoteDestination = remoteDestination.substring(baseWorkingDir.length());
        }

        Logger.debug(LOG_TAG, "Starting SFTP upload of: " + remoteDestination);

        makeDirectories(remoteDestination, sftpParams.createdDirectoriesPermissions);

        try {
            String remoteFileName = getRemoteFileName(file);
            try {
                Logger.debug(LOG_TAG, "Put of: " + file.getPath() + " to: " + remoteFileName + " current: " + channelSftp.pwd());
                channelSftp.put(file.getPath(), remoteFileName, this);
            } catch (Exception ex) {
                throw new IOException("Error while uploading: " + file.getName(service)
                        + " to: " + file.getProperty(PARAM_REMOTE_PATH));
            }

            setPermission(remoteFileName, file.getProperty(PARAM_PERMISSIONS));
        } finally {
            ;
        }

        // get back to base working directory
        try {
            channelSftp.cd(baseWorkingDir);
        } catch (Exception ex) {
            Logger.info(LOG_TAG, "Can't change working directory to: " + baseWorkingDir);
        }
    }

    private void setPermission(String remoteFileName, String permissions) {
        if (permissions == null || "".equals(permissions))
            return;

        try {
            channelSftp.chmod(Integer.parseInt(permissions), remoteFileName);
            Logger.debug(LOG_TAG, "Permissions for: " + remoteFileName + " set to: " + permissions);
        } catch (Exception exc) {
            Logger.error(LOG_TAG, "Error while setting permissions for: "
                    + remoteFileName + " to: " + permissions
                    + ". Check if your SFTP user can set file permissions!", exc);
        }
    }

    /**
     * Creates a nested directory structure on a SFTP server and enters into it.
     *
     * @param dirPath     Path of the directory, i.e /projects/java/sftp/demo
     * @param permissions UNIX permissions to apply to created directories. If null, the SFTP
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

            try {
                channelSftp.cd(singleDir);
                continue;
            } catch (Exception ex) {
                //did not exist
            }

            try {
                channelSftp.mkdir(singleDir);

                if (permissions != null) {
                    setPermission(singleDir, permissions);
                }
                channelSftp.cd(singleDir);
            } catch (Exception ex) {
                throw new IOException("Unable to create remote directory: " + singleDir);
            }
        }
    }

    /**
     * Checks if the remote file path contains also the remote file name. If it's not specified,
     * the name of the local file will be used.
     *
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

    @Override
    public void init(int op, String src, String dest, long max) {

    }

    @Override
    public boolean count(long count) {
        uploadedBytes += count;
        broadcastProgress(uploadedBytes, totalBytes);

        if (!shouldContinue) {
            try {
                channel.disconnect();
                session.disconnect();

                channel = null;
                channelSftp = null;
                session = null;
            } catch (Exception exc) {
                Logger.error(LOG_TAG, "Failed to abort current file transfer", exc);
            }
        }

        return false;
    }

    @Override
    public void end() {

    }
}
