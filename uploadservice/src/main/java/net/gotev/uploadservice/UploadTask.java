package net.gotev.uploadservice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base class to subclass when creating upload tasks. It contains the logic common to all the tasks,
 * such as notification management, status broadcast, retry logic and some utility methods.
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
    private final List<String> successfullyUploadedFiles = new ArrayList<>();

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

    private int notificationId;
    private long lastProgressNotificationTime;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notification;
    private Handler mainThreadHandler;

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
     * @throws Exception if an error occurs
     */
    abstract protected void upload() throws Exception;

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {}

    public UploadTask() {
        startTime = new Date().getTime();
    }

    /**
     * Initializes the {@link UploadTask}.<br>
     * Override this method in subclasses to perform custom task initialization and to get the
     * custom parameters set in {@link UploadRequest#initializeIntent(Intent)} method.
     *
     * @param service Upload Service instance. You should use this reference as your context.
     * @param intent intent sent to the service to start the upload
     * @throws IOException if an I/O exception occurs while initializing
     */
    protected void init(UploadService service, Intent intent) throws IOException {
        this.notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notification = new NotificationCompat.Builder(service);
        this.service = service;
        this.mainThreadHandler = new Handler(service.getMainLooper());
        this.params = intent.getParcelableExtra(UploadService.PARAM_TASK_PARAMETERS);
    }

    @Override
    public final void run() {

        if (!params.getFiles().isEmpty() && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int permissionCheck = ContextCompat.checkSelfPermission(service,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                broadcastError(new RuntimeException("Please request " + Manifest.permission.WRITE_EXTERNAL_STORAGE + " permission before performing the request! Check this: https://gist.github.com/gotev/67c300c563bdf68a502c"));
                return;
            }
        }

        createNotification(new UploadInfo(params.getId()));

        attempts = 0;

        int errorDelay = UploadService.INITIAL_RETRY_WAIT_TIME;

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
                    Logger.error(LOG_TAG, "Error in uploadId " + params.getId()
                            + " on attempt " + attempts
                            + ". Waiting " + errorDelay / 1000 + "s before next attempt. ", exc);

                    long beforeSleepTs = System.currentTimeMillis();

                    while (shouldContinue && System.currentTimeMillis() < (beforeSleepTs + errorDelay)) {
                        try {
                            Thread.sleep(2000);
                        } catch (Throwable ignored) { }
                    }

                    errorDelay *= UploadService.BACKOFF_MULTIPLIER;
                    if (errorDelay > UploadService.MAX_RETRY_WAIT_TIME) {
                        errorDelay = UploadService.MAX_RETRY_WAIT_TIME;
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
     * @param notificationId notification ID
     * @return {@link UploadTask}
     */
    protected final UploadTask setNotificationId(int notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes number of bytes which has been uploaded to the server
     * @param totalBytes total bytes of the request
     */
    protected final void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (currentTime < lastProgressNotificationTime + UploadService.PROGRESS_REPORT_INTERVAL) {
            return;
        }

        setLastProgressNotificationTime(currentTime);

        Logger.debug(LOG_TAG, "Broadcasting upload progress for " + params.getId()
                              + ": " + uploadedBytes + " bytes of " + totalBytes);

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     params.getFiles().size() + successfullyUploadedFiles.size());

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.IN_PROGRESS)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onProgress(service, uploadInfo);
                }
            });
        } else {
            service.sendBroadcast(data.getIntent());
        }

        updateNotificationProgress(uploadInfo);
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

        boolean successfulUpload = ((response.getHttpCode() / 100) == 2);

        if (successfulUpload) {
            onSuccessfulUpload();

            if (params.isAutoDeleteSuccessfullyUploadedFiles() && !successfullyUploadedFiles.isEmpty()) {
                for (String filePath : successfullyUploadedFiles) {
                    deleteFile(new File(filePath));
                }
            }
        }

        Logger.debug(LOG_TAG, "Broadcasting upload completed for " + params.getId());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     params.getFiles().size());

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onCompleted(service, uploadInfo, response);
                }
            });
        } else {
            BroadcastData data = new BroadcastData()
                    .setStatus(BroadcastData.Status.COMPLETED)
                    .setUploadInfo(uploadInfo)
                    .setServerResponse(response);

            service.sendBroadcast(data.getIntent());
        }

        final UploadNotificationConfig notificationConfig = params.getNotificationConfig();

        if (notificationConfig != null) {
            if (successfulUpload) {
                updateNotification(uploadInfo, notificationConfig.getCompletedMessage(),
                        notificationConfig.isAutoClearOnSuccess(),
                        notificationConfig.getCompletedIconResourceID(),
                        notificationConfig.getCompletedIconColorResourceID());
            } else {
                updateNotification(uploadInfo, notificationConfig.getErrorMessage(),
                        notificationConfig.isAutoClearOnError(),
                        notificationConfig.getErrorIconResourceID(),
                        notificationConfig.getErrorIconColorResourceID());
            }
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

        Logger.debug(LOG_TAG, "Broadcasting cancellation for upload with ID: " + params.getId());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     params.getFiles().size());

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.CANCELLED)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onCancelled(service, uploadInfo);
                }
            });
        } else {
            service.sendBroadcast(data.getIntent());
        }

        final UploadNotificationConfig notificationConfig = params.getNotificationConfig();

        if (notificationConfig != null) {
            updateNotification(uploadInfo, notificationConfig.getCancelledMessage(),
                    notificationConfig.isAutoClearOnCancel(),
                    notificationConfig.getCancelledIconResourceID(),
                    notificationConfig.getCancelledIconColorResourceID());
        }

        service.taskCompleted(params.getId());
    }

    /**
     * Add a file to the list of the successfully uploaded files.
     * @param absolutePath absolute path to the file on the device
     */
    protected final void addSuccessfullyUploadedFile(String absolutePath) {
        if (!successfullyUploadedFiles.contains(absolutePath)) {
            successfullyUploadedFiles.add(absolutePath);
        }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use {@link UploadTask#addSuccessfullyUploadedFile(String)}
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

        Logger.info(LOG_TAG, "Broadcasting error for upload with ID: "
                + params.getId() + ". " + exception.getMessage());

        final UploadInfo uploadInfo = new UploadInfo(params.getId(), startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     params.getFiles().size());

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.ERROR)
                .setUploadInfo(uploadInfo)
                .setException(exception);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.getId());
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onError(service, uploadInfo, exception);
                }
            });
        } else {
            service.sendBroadcast(data.getIntent());
        }

        final UploadNotificationConfig notificationConfig = params.getNotificationConfig();

        if (notificationConfig != null && notificationConfig.getErrorMessage() != null) {
            updateNotification(uploadInfo, notificationConfig.getErrorMessage(),
                    notificationConfig.isAutoClearOnError(),
                    notificationConfig.getErrorIconResourceID(),
                    notificationConfig.getErrorIconColorResourceID());
        }

        service.taskCompleted(params.getId());
    }

    /**
     * If the upload task is initialized with a notification configuration, this handles its
     * creation.
     * @param uploadInfo upload information and statistics
     */
    private void createNotification(UploadInfo uploadInfo) {
        if (params.getNotificationConfig() == null || params.getNotificationConfig().getInProgressMessage() == null) return;

        notification.setContentTitle(Placeholders.replace(params.getNotificationConfig().getTitle(), uploadInfo))
                .setContentText(Placeholders.replace(params.getNotificationConfig().getInProgressMessage(), uploadInfo))
                .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                .setSmallIcon(params.getNotificationConfig().getIconResourceID())
                .setColor(params.getNotificationConfig().getIconColorResourceID())
                .setGroup(UploadService.NAMESPACE)
                .setProgress(100, 0, true)
                .setOngoing(true);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.getId(), builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    /**
     * Informs the {@link UploadService} that the task has made some progress. You should call this
     * method from your task whenever you have successfully transferred some bytes to the server.
     * @param uploadInfo upload information and statistics
     */
    private void updateNotificationProgress(UploadInfo uploadInfo) {
        if (params.getNotificationConfig() == null || params.getNotificationConfig().getInProgressMessage() == null) return;

        notification.setContentTitle(Placeholders.replace(params.getNotificationConfig().getTitle(), uploadInfo))
                .setContentText(Placeholders.replace(params.getNotificationConfig().getInProgressMessage(), uploadInfo))
                .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                .setSmallIcon(params.getNotificationConfig().getIconResourceID())
                .setColor(params.getNotificationConfig().getIconColorResourceID())
                .setGroup(UploadService.NAMESPACE)
                .setProgress((int)uploadInfo.getTotalBytes(), (int)uploadInfo.getUploadedBytes(), false)
                .setOngoing(true);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.getId(), builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    private void setRingtone() {

        if(params.getNotificationConfig().isRingToneEnabled()) {
            notification.setSound(RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION));
            notification.setOnlyAlertOnce(false);
        }

    }

    private void updateNotification(UploadInfo uploadInfo, String message,
                                    boolean autoClearOnSuccess,
                                    int iconResourceID,
                                    int iconColorResourceID) {
        if (params.getNotificationConfig() == null) return;

        notificationManager.cancel(notificationId);

        if (message == null) return;

        if (!autoClearOnSuccess) {
            notification.setContentTitle(Placeholders.replace(params.getNotificationConfig().getTitle(), uploadInfo))
                    .setContentText(Placeholders.replace(message, uploadInfo))
                    .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                    .setAutoCancel(params.getNotificationConfig().isClearOnAction())
                    .setSmallIcon(iconResourceID)
                    .setColor(iconColorResourceID)
                    .setGroup(UploadService.NAMESPACE)
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            setRingtone();

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            notificationManager.notify(notificationId + 1, notification.build());
        }
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
                Logger.error(LOG_TAG, "Unable to delete: "
                        + fileToDelete.getAbsolutePath());
            } else {
                Logger.info(LOG_TAG, "Successfully deleted: "
                        + fileToDelete.getAbsolutePath());
            }

        } catch (Exception exc) {
            Logger.error(LOG_TAG,
                    "Error while deleting: " + fileToDelete.getAbsolutePath() +
                            " Check if you granted: android.permission.WRITE_EXTERNAL_STORAGE", exc);
        }

        return deleted;
    }

    public final void cancel() {
        this.shouldContinue = false;
    }

}
