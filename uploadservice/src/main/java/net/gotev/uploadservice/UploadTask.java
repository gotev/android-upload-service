package net.gotev.uploadservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;

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
     * Implementation of the upload logic.
     * @throws Exception if an error occurs
     */
    abstract protected void upload() throws Exception;

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {}

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
        this.params = intent.getParcelableExtra(UploadService.PARAM_TASK_PARAMETERS);
    }

    @Override
    public final void run() {

        createNotification();

        int attempts = 0;

        int errorDelay = 1000;
        int maxErrorDelay = 10 * 60 * 1000;

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
                    Logger.info(LOG_TAG, "Error in uploadId " + params.getId()
                            + " on attempt " + attempts
                            + ". Waiting " + errorDelay / 1000 + "s before next attempt. "
                            + exc.getMessage());
                    SystemClock.sleep(errorDelay);

                    errorDelay *= 10;
                    if (errorDelay > maxErrorDelay) {
                        errorDelay = maxErrorDelay;
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
                + " Uploaded bytes: " + uploadedBytes + " out of " + totalBytes);

        BroadcastData data = new BroadcastData()
                .setId(params.getId())
                .setStatus(BroadcastData.Status.IN_PROGRESS)
                .setUploadedBytes(uploadedBytes)
                .setTotalBytes(totalBytes);

        service.sendBroadcast(data.getIntent());

        updateNotificationProgress((int) uploadedBytes, (int) totalBytes);
    }

    /**
     * Broadcasts a completion status update and informs the {@link UploadService} that the task
     * executes successfully.
     * Call this when the task has completed the upload request and has received the response
     * from the server.
     *
     * @param responseCode HTTP response code got from the server. If you are implementing another
     *                     protocol, set this to {@link UploadTask#TASK_COMPLETED_SUCCESSFULLY}
     *                     to inform that the task has been completed successfully. Integer values
     *                     lower than 200 or greater that 299 indicates error response from server.
     * @param serverResponseBody bytes read from server's response body. If your server does not
     *                           return anything, set this to {@link UploadTask#EMPTY_RESPONSE}.
     */
    protected final void broadcastCompleted(final int responseCode, final byte[] serverResponseBody) {

        boolean successfulUpload = ((responseCode / 100) == 2);

        if (successfulUpload) {
            if (params.isAutoDeleteSuccessfullyUploadedFiles() && !params.getFiles().isEmpty()) {
                Iterator<UploadFile> iterator = params.getFiles().iterator();

                while (iterator.hasNext()) {
                    deleteFile(iterator.next().file);
                }
            }

            onSuccessfulUpload();
        }

        Logger.debug(LOG_TAG, "Broadcasting upload completed for " + params.getId());

        BroadcastData data = new BroadcastData()
                .setId(params.getId())
                .setStatus(BroadcastData.Status.COMPLETED)
                .setResponseCode(responseCode)
                .setResponseBody(serverResponseBody);

        service.sendBroadcast(data.getIntent());

        if (successfulUpload)
            updateNotificationCompleted();
        else
            updateNotificationError();

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

        BroadcastData data = new BroadcastData()
                .setId(params.getId())
                .setStatus(BroadcastData.Status.CANCELLED);

        service.sendBroadcast(data.getIntent());

        updateNotificationError();

        service.taskCompleted(params.getId());
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

        BroadcastData data = new BroadcastData()
                .setId(params.getId())
                .setStatus(BroadcastData.Status.ERROR)
                .setException(exception);

        service.sendBroadcast(data.getIntent());

        updateNotificationError();

        service.taskCompleted(params.getId());
    }

    /**
     * If the upload task is initialized with a notification configuration, this handles its
     * creation.
     */
    private void createNotification() {
        if (params.getNotificationConfig() == null) return;

        notification.setContentTitle(params.getNotificationConfig().getTitle())
                .setContentText(params.getNotificationConfig().getInProgressMessage())
                .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                .setSmallIcon(params.getNotificationConfig().getIconResourceID())
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
     * @param uploadedBytes total bytes uploaded so far
     * @param totalBytes total bytes to upload
     */
    private void updateNotificationProgress(int uploadedBytes, int totalBytes) {
        if (params.getNotificationConfig() == null) return;

        notification.setContentTitle(params.getNotificationConfig().getTitle())
                .setContentText(params.getNotificationConfig().getInProgressMessage())
                .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                .setSmallIcon(params.getNotificationConfig().getIconResourceID())
                .setGroup(UploadService.NAMESPACE)
                .setProgress(totalBytes, uploadedBytes, false)
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

    private void updateNotificationCompleted() {
        if (params.getNotificationConfig() == null) return;

        notificationManager.cancel(notificationId);

        if (!params.getNotificationConfig().isAutoClearOnSuccess()) {
            notification.setContentTitle(params.getNotificationConfig().getTitle())
                    .setContentText(params.getNotificationConfig().getCompletedMessage())
                    .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                    .setAutoCancel(params.getNotificationConfig().isClearOnAction())
                    .setSmallIcon(params.getNotificationConfig().getIconResourceID())
                    .setGroup(UploadService.NAMESPACE)
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            setRingtone();

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            notificationManager.notify(notificationId + 1, notification.build());
        }
    }

    private void updateNotificationError() {
        if (params.getNotificationConfig() == null) return;

        notificationManager.cancel(notificationId);

        notification.setContentTitle(params.getNotificationConfig().getTitle())
                .setContentText(params.getNotificationConfig().getErrorMessage())
                .setContentIntent(params.getNotificationConfig().getPendingIntent(service))
                .setAutoCancel(params.getNotificationConfig().isClearOnAction())
                .setSmallIcon(params.getNotificationConfig().getIconResourceID())
                .setGroup(UploadService.NAMESPACE)
                .setProgress(0, 0, false).setOngoing(false);
        setRingtone();

        // this is needed because the main notification used to show progress is ongoing
        // and a new one has to be created to allow the user to dismiss it
        notificationManager.notify(notificationId + 1, notification.build());
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
