package net.gotev.uploadservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;

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
    private Handler mainThreadHandler;
    private long notificationCreationTimeMillis;

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
        this.params = intent.getParcelableExtra(UploadService.PARAM_TASK_PARAMETERS);
        this.service = service;
        this.mainThreadHandler = new Handler(service.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && params.notificationConfig != null) {
            String notificationChannelId = params.notificationConfig.getNotificationChannelId();

            if (notificationChannelId == null) {
                params.notificationConfig.setNotificationChannelId(UploadService.NAMESPACE);
                notificationChannelId = UploadService.NAMESPACE;
            }

            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                NotificationChannel channel = new NotificationChannel(notificationChannelId, "Upload Service channel", NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(channel);
            }
        }

    }

    @Override
    public final void run() {

        createNotification(new UploadInfo(params.id));

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
                    Logger.error(LOG_TAG, "Error in uploadId " + params.id
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
        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadService.PROGRESS_REPORT_INTERVAL) {
            return;
        }

        setLastProgressNotificationTime(currentTime);

        Logger.debug(LOG_TAG, "Broadcasting upload progress for " + params.id
                              + ": " + uploadedBytes + " bytes of " + totalBytes);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     pathStringListFrom(params.files));

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.IN_PROGRESS)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
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

        final boolean successfulUpload = response.getHttpCode() >= 200 && response.getHttpCode() < 400;

        if (successfulUpload) {
            onSuccessfulUpload();

            if (params.autoDeleteSuccessfullyUploadedFiles && !successfullyUploadedFiles.isEmpty()) {
                for (String filePath : successfullyUploadedFiles) {
                    deleteFile(new File(filePath));
                }
            }
        }

        Logger.debug(LOG_TAG, "Broadcasting upload " + (successfulUpload ? "completed" : "error")
                + " for " + params.id);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     pathStringListFrom(params.files));

        final UploadNotificationConfig notificationConfig = params.notificationConfig;

        if (notificationConfig != null) {
            if (successfulUpload && notificationConfig.getCompleted().message != null) {
                updateNotification(uploadInfo, notificationConfig.getCompleted());

            } else if (notificationConfig.getError().message != null){
                updateNotification(uploadInfo, notificationConfig.getError());
            }
        }

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
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
            BroadcastData data = new BroadcastData()
                    .setStatus(successfulUpload ? BroadcastData.Status.COMPLETED : BroadcastData.Status.ERROR)
                    .setUploadInfo(uploadInfo)
                    .setServerResponse(response);

            service.sendBroadcast(data.getIntent());
        }

        service.taskCompleted(params.id);
    }

    /**
     * Broadcast a cancelled status.
     * This called automatically by {@link UploadTask} when the user cancels the request,
     * and the specific implementation of {@link UploadTask#upload()} either
     * returns or throws an exception. You should never call this method explicitly in your
     * implementation.
     */
    protected final void broadcastCancelled() {

        Logger.debug(LOG_TAG, "Broadcasting cancellation for upload with ID: " + params.id);

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     pathStringListFrom(params.files));

        final UploadNotificationConfig notificationConfig = params.notificationConfig;

        if (notificationConfig != null && notificationConfig.getCancelled().message != null) {
            updateNotification(uploadInfo, notificationConfig.getCancelled());
        }

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.CANCELLED)
                .setUploadInfo(uploadInfo);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
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

        service.taskCompleted(params.id);
    }

    /**
     * Add a file to the list of the successfully uploaded files and remove it from the file list
     * @param file file on the device
     */
    protected final void addSuccessfullyUploadedFile(UploadFile file) {
        if (!successfullyUploadedFiles.contains(file.path)) {
            successfullyUploadedFiles.add(file.path);
            params.files.remove(file);
        }
    }

    /**
     * Adds all the files to the list of successfully uploaded files.
     * This will automatically remove them from the params.getFiles() list.
     */
    protected final void addAllFilesToSuccessfullyUploadedFiles() {
        for (Iterator<UploadFile> iterator = params.files.iterator(); iterator.hasNext();) {
            UploadFile file = iterator.next();

            if (!successfullyUploadedFiles.contains(file.path)) {
                successfullyUploadedFiles.add(file.path);
            }
            iterator.remove();
        }
    }

    /**
     * Gets the list of all the successfully uploaded files.
     * You must not modify this list in your subclasses! You can only read its contents.
     * If you want to add an element into it,
     * use {@link UploadTask#addSuccessfullyUploadedFile(UploadFile)}
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
                + params.id + ". " + exception.getMessage());

        final UploadInfo uploadInfo = new UploadInfo(params.id, startTime, uploadedBytes,
                                                     totalBytes, (attempts - 1),
                                                     successfullyUploadedFiles,
                                                     pathStringListFrom(params.files));

        final UploadNotificationConfig notificationConfig = params.notificationConfig;

        if (notificationConfig != null && notificationConfig.getError().message != null) {
            updateNotification(uploadInfo, notificationConfig.getError());
        }

        BroadcastData data = new BroadcastData()
                .setStatus(BroadcastData.Status.ERROR)
                .setUploadInfo(uploadInfo)
                .setException(exception);

        final UploadStatusDelegate delegate = UploadService.getUploadStatusDelegate(params.id);
        if (delegate != null) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    delegate.onError(service, uploadInfo, null, exception);
                }
            });
        } else {
            service.sendBroadcast(data.getIntent());
        }

        service.taskCompleted(params.id);
    }

    /**
     * If the upload task is initialized with a notification configuration, this handles its
     * creation.
     * @param uploadInfo upload information and statistics
     */
    private void createNotification(UploadInfo uploadInfo) {
        if (params.notificationConfig == null || params.notificationConfig.getProgress().message == null) return;

        UploadNotificationStatusConfig statusConfig = params.notificationConfig.getProgress();
        notificationCreationTimeMillis = System.currentTimeMillis();

        NotificationCompat.Builder notification = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                .setWhen(notificationCreationTimeMillis)
                .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setGroup(UploadService.NAMESPACE)
                .setProgress(100, 0, true)
                .setOngoing(true);

        statusConfig.addActionsToNotificationBuilder(notification);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.id, builtNotification)) {
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
        if (params.notificationConfig == null || params.notificationConfig.getProgress().message == null) return;

        UploadNotificationStatusConfig statusConfig = params.notificationConfig.getProgress();

        NotificationCompat.Builder notification = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                .setWhen(notificationCreationTimeMillis)
                .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setGroup(UploadService.NAMESPACE)
                .setProgress((int)uploadInfo.getTotalBytes(), (int)uploadInfo.getUploadedBytes(), false)
                .setOngoing(true);

        statusConfig.addActionsToNotificationBuilder(notification);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.id, builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    private void setRingtone(NotificationCompat.Builder notification) {

        if (params.notificationConfig.isRingToneEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri sound = RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION);
            notification.setSound(sound);
        }

    }

    private void updateNotification(UploadInfo uploadInfo, UploadNotificationStatusConfig statusConfig) {
        if (params.notificationConfig == null) return;

        notificationManager.cancel(notificationId);

        if (statusConfig.message == null) return;

        if (!statusConfig.autoClear) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                    .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                    .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                    .setContentIntent(statusConfig.getClickIntent(service))
                    .setAutoCancel(statusConfig.clearOnAction)
                    .setSmallIcon(statusConfig.iconResourceID)
                    .setLargeIcon(statusConfig.largeIcon)
                    .setColor(statusConfig.iconColorResourceID)
                    .setGroup(UploadService.NAMESPACE)
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            statusConfig.addActionsToNotificationBuilder(notification);

            setRingtone(notification);

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            uploadInfo.setNotificationID(notificationId + 1);
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

    private static List<String> pathStringListFrom(List<UploadFile> files) {
        final List<String> filesLeft = new ArrayList<>(files.size());
        for (UploadFile f : files) {
            filesLeft.add(f.getPath());
        }
        return filesLeft;
    }

    public final void cancel() {
        this.shouldContinue = false;
    }

}
