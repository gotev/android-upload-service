package net.gotev.uploadservice;

import android.content.Intent;
import android.os.Handler;

import net.gotev.uploadservice.data.BroadcastData;
import net.gotev.uploadservice.data.UploadFile;
import net.gotev.uploadservice.data.UploadInfo;
import net.gotev.uploadservice.data.UploadStatus;
import net.gotev.uploadservice.data.UploadTaskParameters;
import net.gotev.uploadservice.logger.UploadServiceLogger;
import net.gotev.uploadservice.network.ServerResponse;
import net.gotev.uploadservice.tasklistener.NotificationHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Base class to subclass when creating upload tasks. It contains the logic common to all the tasks,
 * such as notification management, status broadcast, retry logic and some utility methods.
 *
 * @author Aleksandar Gotev
 */
public abstract class UploadTask implements Runnable {

    private static final String LOG_TAG = UploadTask.class.getSimpleName();

    /**
     * Constant which indicates that the upload task has been completed successfully.
     */
    protected static final int TASK_COMPLETED_SUCCESSFULLY = 200;

    /**
     * Constant which indicates an empty response from the server.
     */
    protected static final byte[] EMPTY_RESPONSE = "".getBytes(Charset.forName("UTF-8"));

    /**
     * Reference to the upload service instance.
     */
    protected UploadService service;

    /**
     * Contains all the parameters set in {@link UploadRequest}.
     */
    protected UploadTaskParameters params = null;

    /**
     * Contains the absolute local path of the successfully uploaded files.
     */
    private final ArrayList<String> successfullyUploadedFiles = new ArrayList<>();

    /**
     * Flag indicating if the operation should continue or is cancelled. You should never
     * explicitly set this value in your subclasses, as it's written by the Upload Service
     * when you call {@link UploadService#stopUpload(String)}. If this value is false, you should
     * terminate your upload task as soon as possible, so be sure to check the status when
     * performing long running operations such as data transfer. As a rule of thumb, check this
     * value at every step of the upload protocol you are implementing, and after that each chunk
     * of data that has been successfully transferred.
     */
    protected boolean shouldContinue = true;

    private long lastProgressNotificationTime;
    private Handler mainThreadHandler;
    private NotificationHandler notificationHandler;

    /**
     * Total bytes to transfer. You should initialize this value in the
     * {@link UploadTask#upload()} method of your subclasses, before starting the upload data
     * transfer.
     */
    protected long totalBytes;

    /**
     * Total transferred bytes. You should update this value in your subclasses when you upload
     * some data, and before calling {@link UploadTask#broadcastProgress(long, long)}
     */
    protected long uploadedBytes;

    /**
     * Start timestamp of this upload task.
     */
    private final long startTime;

    /**
     * Counter of the upload attempts that has been made;
     */
    private int attempts;

    /**
     * Implementation of the upload logic.
     *
     * @throws Exception if an error occurs
     */
    abstract protected void upload() throws Exception;

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {
    }

    public UploadTask() {
        startTime = new Date().getTime();
    }

    /**
     * Initializes the {@link UploadTask}.<br>
     * Override this method in subclasses to perform custom task initialization and to get the
     * custom parameters set in {@link UploadRequest#initializeIntent(Intent)} method.
     *
     * @param service Upload Service instance. You should use this reference as your context.
     * @param intent  intent sent to the service to start the upload
     * @throws IOException if an I/O exception occurs while initializing
     */
    protected void init(UploadService service, Intent intent) throws IOException {
        this.params = intent.getParcelableExtra(UploadService.PARAM_TASK_PARAMETERS);
        this.service = service;
        this.mainThreadHandler = new Handler(service.getMainLooper());
    }

    @Override
    public final void run() {

        if (notificationHandler != null) {
            notificationHandler.initialize(new UploadInfo(params.getId()));
        }

        attempts = 0;

        long errorDelay = UploadServiceConfig.INSTANCE.getRetryPolicy().getInitialWaitTimeSeconds();

        while (attempts <= params.getMaxRetries() && shouldContinue) {
            attempts++;

            try {
                upload();
                break;

            } catch (Exception exc) {
                if (!shouldContinue) {
                    break;
                } else if (attempts > params.getMaxRetries()) {
                    broadcastError(exc);
                } else {
                    UploadServiceLogger.INSTANCE.error(LOG_TAG, "Error in uploadId " + params.getId()
                            + " on attempt " + attempts
                            + ". Waiting " + errorDelay / 1000 + "s before next attempt. ", exc);

                    long beforeSleepTs = System.currentTimeMillis();

                    while (shouldContinue && System.currentTimeMillis() < (beforeSleepTs + errorDelay * 1000)) {
                        try {
                            Thread.sleep(2000);
                        } catch (Throwable ignored) {
                        }
                    }

                    errorDelay *= UploadServiceConfig.INSTANCE.getRetryPolicy().getMultiplier();
                    if (errorDelay > UploadServiceConfig.INSTANCE.getRetryPolicy().getMaxWaitTimeSeconds()) {
                        errorDelay = UploadServiceConfig.INSTANCE.getRetryPolicy().getMaxWaitTimeSeconds();
                    }
                }
            }
        }

        if (!shouldContinue) {
            broadcastCancelled();
        }
    }

    /**
     * Sets the last time the notification was updated.
     * This is handled automatically and you should never call this method.
     *
     * @param lastProgressNotificationTime time in milliseconds
     * @return {@link UploadTask}
     */
    protected final UploadTask setLastProgressNotificationTime(long lastProgressNotificationTime) {
        this.lastProgressNotificationTime = lastProgressNotificationTime;
        return this;
    }

    /**
     * Sets the upload notification ID for this task.
     * This gets called by {@link UploadService} when the task is initialized.
     * You should never call this method.
     *
     * @param notificationId notification ID
     * @return {@link UploadTask}
     */
    protected final UploadTask setNotificationId(int notificationId) {
        UploadNotificationConfig config = params.getNotificationConfig();
        if (config != null) {
            notificationHandler = new NotificationHandler(service, notificationId, params.getId(), config);
        } else {
            notificationHandler = null;
        }
        return this;
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes number of bytes which has been uploaded to the server
     * @param totalBytes    total bytes of the request
     */
    protected final void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadServiceConfig.INSTANCE.getUploadProgressNotificationIntervalMillis()) {
            return;
        }

        setLastProgressNotificationTime(currentTime);

        UploadServiceLogger.INSTANCE.debug(LOG_TAG, "Broadcasting upload progress for " + params.getId()
                + ": " + uploadedBytes + " bytes of " + totalBytes);

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                null,
                successfullyUploadedFiles,
                pathStringListFrom(params.getFiles()));

        BroadcastData data = new BroadcastData(UploadStatus.IN_PROGRESS, uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onProgress(service, uploadInfo);
                }
            });
        } else {
            data.send(service);
        }

        if (notificationHandler != null) {
            notificationHandler.onProgress(uploadInfo);
        }
    }

    /**
     * Broadcasts a completion status update and informs the {@link UploadService} that the task
     * executes successfully.
     * Call this when the task has completed the upload request and has received the response
     * from the server.
     *
     * @param response response got from the server
     */
    protected final void broadcastCompleted(final ServerResponse response) {

        final boolean successfulUpload = response.getCode() >= 200 && response.getCode() < 400;

        if (successfulUpload) {
            onSuccessfulUpload();

            if (params.getAutoDeleteSuccessfullyUploadedFiles() && !successfullyUploadedFiles.isEmpty()) {
                for (String filePath : successfullyUploadedFiles) {
                    deleteFile(new File(filePath));
                }
            }
        }

        UploadServiceLogger.INSTANCE.debug(LOG_TAG, "Broadcasting upload " + (successfulUpload ? "completed" : "error")
                + " for " + params.getId());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                null,
                successfullyUploadedFiles,
                pathStringListFrom(params.getFiles()));

        if (notificationHandler != null) {
            notificationHandler.onCompleted(uploadInfo, response);
        }

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (successfulUpload) {
                        delegate.onCompleted(service, uploadInfo, response);
                    } else {
                        delegate.onError(service, uploadInfo, response, null);
                    }
                }
            });
        } else {
            BroadcastData data = new BroadcastData(
                    successfulUpload ? UploadStatus.COMPLETED : UploadStatus.ERROR,
                    uploadInfo,
                    response
            );

            data.send(service);
        }

        service.taskCompleted(params.getId());
    }

    /**
     * Broadcast a cancelled status.
     * This called automatically by {@link UploadTask} when the user cancels the request,
     * and the specific implementation of {@link UploadTask#upload()} either
     * returns or throws an exception. You should never call this method explicitly in your
     * implementation.
     */
    protected final void broadcastCancelled() {

        UploadServiceLogger.INSTANCE.debug(LOG_TAG, "Broadcasting cancellation for upload with ID: " + params.getId());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                null,
                successfullyUploadedFiles,
                pathStringListFrom(params.getFiles()));

        if (notificationHandler != null) {
            notificationHandler.onCancelled(uploadInfo);
        }

        BroadcastData data = new BroadcastData(UploadStatus.CANCELLED, uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onCancelled(service, uploadInfo);
                }
            });
        } else {
            data.send(service);
        }

        service.taskCompleted(params.getId());
    }

    /**
     * Add a file to the list of the successfully uploaded files and remove it from the file list
     *
     * @param file file on the device
     */
    protected final void addSuccessfullyUploadedFile(UploadFile file) {
        if (!successfullyUploadedFiles.contains(file.getPath())) {
            successfullyUploadedFiles.add(file.getPath());
            params.getFiles().remove(file);
        }
    }

    /**
     * Adds all the files to the list of successfully uploaded files.
     * This will automatically remove them from the params.getFiles() list.
     */
    protected final void addAllFilesToSuccessfullyUploadedFiles() {
        for (Iterator<UploadFile> iterator = params.getFiles().iterator(); iterator.hasNext(); ) {
            UploadFile file = iterator.next();

            if (!successfullyUploadedFiles.contains(file.getPath())) {
                successfullyUploadedFiles.add(file.getPath());
            }
            iterator.remove();
        }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use {@link UploadTask#addSuccessfullyUploadedFile(UploadFile)}
     *
     * @return list of strings
     */
    protected final List<String> getSuccessfullyUploadedFiles() {
        return successfullyUploadedFiles;
    }

    /**
     * Broadcasts an error.
     * This called automatically by {@link UploadTask} when the specific implementation of
     * {@link UploadTask#upload()} throws an exception and there aren't any left retries.
     * You should never call this method explicitly in your implementation.
     *
     * @param exception exception to broadcast. It's the one thrown by the specific implementation
     *                  of {@link UploadTask#upload()}
     */
    private void broadcastError(final Exception exception) {

        UploadServiceLogger.INSTANCE.info(LOG_TAG, "Broadcasting error for upload with ID: "
                + params.getId() + ". " + exception.getMessage());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                totalBytes, (attempts - 1),
                null,
                successfullyUploadedFiles,
                pathStringListFrom(params.getFiles()));

        if (notificationHandler != null) {
            notificationHandler.onError(uploadInfo, exception);
        }

        BroadcastData data = new BroadcastData(UploadStatus.ERROR, uploadInfo, null, exception);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onError(service, uploadInfo, null, exception);
                }
            });
        } else {
            data.send(service);
        }

        service.taskCompleted(params.getId());
    }

    /**
     * Tries to delete a file from the device.
     * If it fails, the error will be printed in the LogCat.
     *
     * @param fileToDelete file to delete
     * @return true if the file has been deleted, otherwise false.
     */
    private boolean deleteFile(File fileToDelete) {
        boolean deleted = false;

        try {
            deleted = fileToDelete.delete();

            if (!deleted) {
                UploadServiceLogger.INSTANCE.error(LOG_TAG, "Unable to delete: "
                        + fileToDelete.getAbsolutePath());
            } else {
                UploadServiceLogger.INSTANCE.info(LOG_TAG, "Successfully deleted: "
                        + fileToDelete.getAbsolutePath());
            }

        } catch (Exception exc) {
            UploadServiceLogger.INSTANCE.error(LOG_TAG,
                    "Error while deleting: " + fileToDelete.getAbsolutePath() +
                            " Check if you granted: android.permission.WRITE_EXTERNAL_STORAGE", exc);
        }

        return deleted;
    }

    private static ArrayList<String> pathStringListFrom(List<UploadFile> files) {
        final ArrayList<String> filesLeft = new ArrayList<>(files.size());
        for (UploadFile f : files) {
            filesLeft.add(f.getPath());
        }
        return filesLeft;
    }

    public final void cancel() {
        this.shouldContinue = false;
    }

}
