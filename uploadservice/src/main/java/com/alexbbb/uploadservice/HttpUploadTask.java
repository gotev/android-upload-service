package com.alexbbb.uploadservice;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

/**
 * Generic HTTP Upload Task.
 *
 * @author cankov
 * @author alexbbb (Aleksandar Gotev)
 * @author mabdurrahman
 */
public abstract class HttpUploadTask implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    protected UploadService service;
    protected TaskParameters params = null;

    protected HttpURLConnection connection = null;
    protected OutputStream requestStream = null;
    protected InputStream responseStream = null;
    protected boolean shouldContinue = true;

    private int notificationId;
    private long lastProgressNotificationTime;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notification;

    protected long totalBodyBytes;
    protected long uploadedBodyBytes;

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
                    Log.w(getClass().getName(), "Error in uploadId " + params.getId()
                                    + " on attempt " + attempts
                                    + ". Waiting " + errorDelay / 1000 + "s before next attempt",
                                    exc);
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

    @SuppressLint("NewApi")
    protected void upload() throws Exception {

        try {
            totalBodyBytes = getBodyLength();

            connection = getHttpURLConnection();

            if (params.isCustomUserAgentDefined()) {
                params.addRequestHeader("User-Agent", params.getCustomUserAgent());
            }

            setRequestHeaders();

            requestStream = connection.getOutputStream();

            writeBody();

            final int serverResponseCode = connection.getResponseCode();

            if (serverResponseCode / 100 == 2) {
                responseStream = connection.getInputStream();
            } else { // getErrorStream if the response code is not 2xx
                responseStream = connection.getErrorStream();
            }

            // Broadcast completion only if the user has not cancelled the operation.
            // It may happen that when the body is not completely written and the client
            // closes the connection, no exception is thrown here, and the server responds
            // with an HTTP status code. Without this, what happened was that completion was
            // broadcasted and then the cancellation. That behaviour was not desirable as the
            // library user couldn't execute code on user cancellation.
            if (shouldContinue) {
                broadcastCompleted(serverResponseCode, getResponseBodyAsByteArray(responseStream));
            }

        } finally {
            closeOutputStream();
            closeInputStream();
            closeConnection();
        }
    }

    protected final HttpURLConnection getHttpURLConnection() throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(params.getUrl()).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestMethod(params.getMethod());

        if (params.isUsesFixedLengthStreamingMode()) {
            if (android.os.Build.VERSION.SDK_INT >= 19) {
                conn.setFixedLengthStreamingMode(totalBodyBytes);

            } else {
                if (totalBodyBytes > Integer.MAX_VALUE)
                    throw new RuntimeException("You need Android API version 19 or newer to "
                            + "upload more than 2GB in a single request using "
                            + "fixed size content length. Try switching to "
                            + "chunked mode instead, but make sure your server side supports it!");

                conn.setFixedLengthStreamingMode((int) totalBodyBytes);
            }
        } else {
            conn.setChunkedStreamingMode(0);
        }

        setupHttpUrlConnection(conn);

        return conn;
    }

    /**
     * Implement in subclasses to be able to perform additional setup to the underlying
     * {@link HttpURLConnection}.
     * @param connection connection to configure
     * @throws IOException if some IO errors occurs
     */
    protected abstract void setupHttpUrlConnection(HttpURLConnection connection) throws IOException;

    /**
     * Implement in subclasses to provide the expected upload in the progress notifications.
     * @return The expected size of the http request body.
     * @throws UnsupportedEncodingException
     */
    protected abstract long getBodyLength() throws UnsupportedEncodingException;

    /**
     * Implement in subclasses to write the body of the http request.
     * @throws IOException
     */
    protected abstract void writeBody() throws IOException;

    /**
     * Implement in subclasses to be able to do something when the upload is successful.
     */
    protected void onSuccessfulUpload() {}

    private void closeInputStream() {
        if (responseStream != null) {
            try {
                responseStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void closeOutputStream() {
        if (requestStream != null) {
            try {
                requestStream.flush();
                requestStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception ignored) {
            }
        }
    }

    private void setRequestHeaders() {
        if (!params.getRequestHeaders().isEmpty()) {
            for (final NameValue param : params.getRequestHeaders()) {
                connection.setRequestProperty(param.getName(), param.getValue());
            }
        }
    }

    public final void cancel() {
        this.shouldContinue = false;
    }

    public final HttpUploadTask setLastProgressNotificationTime(long lastProgressNotificationTime) {
        this.lastProgressNotificationTime = lastProgressNotificationTime;
        return this;
    }

    public final HttpUploadTask setNotificationId(int notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    private byte[] getResponseBodyAsByteArray(final InputStream inputStream) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        try {
            while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) > 0) {
                byteStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception ignored) {}

        return byteStream.toByteArray();
    }

    protected final void writeStream(InputStream stream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0 && shouldContinue) {
            requestStream.write(buffer, 0, bytesRead);
            uploadedBodyBytes += bytesRead;
            broadcastProgress(uploadedBodyBytes, totalBodyBytes);
        }
    }

    /**
     * Broadcasts a progress update.
     *
     * @param uploadedBytes
     * @param totalBytes
     */
    protected final void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (currentTime < lastProgressNotificationTime + UploadService.PROGRESS_REPORT_INTERVAL) {
            return;
        }

        lastProgressNotificationTime = currentTime;

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
                Log.e(this.getClass().getSimpleName(), "Unable to delete: " + fileToDelete.getAbsolutePath());
            }

        } catch (Exception exc) {
            Log.e(this.getClass().getSimpleName(), "Error while deleting: " + fileToDelete.getAbsolutePath(), exc);
        }

        return deleted;
    }

    /**
     * Broadcasts an error.
     *
     * @param exception exception to broadcast
     */
    private void broadcastError(final Exception exception) {

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
                .setProgress(0, 0, false).setOngoing(false);
        setRingtone();

        // this is needed because the main notification used to show progress is ongoing
        // and a new one has to be created to allow the user to dismiss it
        notificationManager.notify(notificationId + 1, notification.build());
    }
}
