package net.gotev.uploadservice;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import net.gotev.uploadservice.http.HttpConnection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Generic HTTP Upload Task.<br>
 * Subclass to create your custom upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 * @author mabdurrahman
 */
public abstract class HttpUploadTask implements Runnable {

    private static final String LOG_TAG = HttpUploadTask.class.getSimpleName();

    private static final int BUFFER_SIZE = 4096;

    /**
     * Reference to the upload service instance.
     */
    protected UploadService service;

    /**
     * Contains all the parameters set in {@link HttpUploadRequest}.
     */
    protected TaskParameters params = null;

    /**
     * HttpUrlConnection used to perform the upload task.
     */
    private HttpConnection connection;

    /**
     * Flag indicating if the operation should continue or is cancelled. You should never
     * explicitly set this value in your subclasses, as it's written by the Upload Service
     * when you call {@link UploadService#stopUpload(String)}.
     */
    protected boolean shouldContinue = true;

    /**
     * Counter of how many bytes have been successfully transferred to the server.
     */
    protected long uploadedBodyBytes;

    /**
     * Total bytes to send in the request body. This value is set by the value returned from
     * {@link HttpUploadTask#getBodyLength()} method.
     */
    protected long totalBodyBytes;

    private int notificationId;
    private long lastProgressNotificationTime;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notification;

    /**
     * Initializes the {@link HttpUploadTask}.<br>
     * Override this method in subclasses to perform custom task initialization and to get the
     * custom parameters set in {@link HttpUploadRequest#initializeIntent(Intent)} method.
     *
     * @param service Upload Service instance
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
     * Implementation of the upload logic.<br>
     * If you want to take advantage of the automations which Android Upload Service provides,
     * do not override or change the implementation of this method in your subclasses. If you do,
     * you have full control on how the upload is done, so for example you can use your custom
     * http stack, but you have to manually setup the request to the server with everything you
     * set in your {@link HttpUploadRequest} subclass and to get the response from the server.
     *
     * @throws Exception if an error occurs
     */
    @SuppressLint("NewApi")
    protected void upload() throws Exception {

        Logger.debug(LOG_TAG, "Starting upload task with ID " + params.getId());

        try {
            totalBodyBytes = getBodyLength();

            if (params.isCustomUserAgentDefined()) {
                params.addRequestHeader("User-Agent", params.getCustomUserAgent());
            }

            connection = UploadService.HTTP_STACK.createNewConnection(params.getMethod(), params.getUrl());

            connection.setHeaders(params.getRequestHeaders(),
                                  params.isUsesFixedLengthStreamingMode(), getBodyLength());

            writeBody(connection);

            final int serverResponseCode = connection.getServerResponseCode();
            Logger.debug(LOG_TAG, "Server responded with HTTP " + serverResponseCode
                            + " to upload with ID: " + params.getId());

            // Broadcast completion only if the user has not cancelled the operation.
            // It may happen that when the body is not completely written and the client
            // closes the connection, no exception is thrown here, and the server responds
            // with an HTTP status code. Without this, what happened was that completion was
            // broadcasted and then the cancellation. That behaviour was not desirable as the
            // library user couldn't execute code on user cancellation.
            if (shouldContinue) {
                broadcastCompleted(serverResponseCode, connection.getServerResponseBody());
            }

        } finally {
            connection.close();
        }
    }

    /**
     * Implement in subclasses to provide the expected upload in the progress notifications.
     * @return The expected size of the http request body.
     * @throws UnsupportedEncodingException
     */
    protected abstract long getBodyLength() throws UnsupportedEncodingException;

    /**
     * Implement in subclasses to write the body of the http request.
     * @param connection connection on which to write the body
     * @throws IOException
     */
    protected abstract void writeBody(HttpConnection connection) throws IOException;

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {}

    public final void cancel() {
        this.shouldContinue = false;
    }

    protected final HttpUploadTask setLastProgressNotificationTime(long lastProgressNotificationTime) {
        this.lastProgressNotificationTime = lastProgressNotificationTime;
        return this;
    }

    protected final HttpUploadTask setNotificationId(int notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    protected final void writeStream(InputStream stream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0 && shouldContinue) {
            connection.writeBody(buffer, bytesRead);
            uploadedBodyBytes += bytesRead;
            broadcastProgress(uploadedBodyBytes, totalBodyBytes);
        }
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
     * Broadcasts a completion update. Call this when the task has completed the upload request and
     * has received the response from the server.
     *
     * @param responseCode HTTP response code got from the server
     * @param serverResponseBody bytes read from server's response body
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
     * Broadcast a cancelled status. Call this when the value of {@code shouldContinue} is false,
     * after that you have done the needed actions to properly cancel the request.
     */
    protected final void broadcastCancelled() {

        Logger.debug(LOG_TAG, "Broadcasting cancellation for upload with ID: "
                + params.getId());

        BroadcastData data = new BroadcastData()
                .setId(params.getId())
                .setStatus(BroadcastData.Status.CANCELLED);

        service.sendBroadcast(data.getIntent());

        updateNotificationError();

        service.taskCompleted(params.getId());
    }

    /**
     * Tries to delete a file from the device. If it fails, the error will be printed in the LogCat
     * log.
     *
     * @param fileToDelete file to delete
     * @return true if the file has been deleted, otherwise false.
     */
    protected final boolean deleteFile(File fileToDelete) {
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

    /**
     * Broadcasts an error.
     *
     * @param exception exception to broadcast
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
}
