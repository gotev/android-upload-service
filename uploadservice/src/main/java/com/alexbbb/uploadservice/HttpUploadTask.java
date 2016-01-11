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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Generic HTTP Upload Task.
 *
 * @author cankov
 * @author alexbbb (Aleksandar Gotev)
 * @author mabdurrahman
 */
abstract class HttpUploadTask implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    protected UploadService service;

    protected final String uploadId;
    protected final String url;
    protected final String method;
    protected final String customUserAgent;
    protected final int maxRetries;
    protected final ArrayList<NameValue> headers;

    protected HttpURLConnection connection = null;
    protected OutputStream requestStream = null;
    protected InputStream responseStream = null;
    protected boolean shouldContinue = true;

    private int notificationId;
    private long lastProgressNotificationTime;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notification;
    protected UploadNotificationConfig notificationConfig;

    protected long totalBodyBytes;
    protected long uploadedBodyBytes;

    HttpUploadTask(UploadService service, Intent intent) {

        this.notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationConfig = intent.getParcelableExtra(UploadService.PARAM_NOTIFICATION_CONFIG);
        this.notification = new NotificationCompat.Builder(service);
        this.service = service;

        this.uploadId = intent.getStringExtra(UploadService.PARAM_ID);
        this.url = intent.getStringExtra(UploadService.PARAM_URL);
        this.method = intent.getStringExtra(UploadService.PARAM_METHOD);
        this.customUserAgent = intent.getStringExtra(UploadService.PARAM_CUSTOM_USER_AGENT);
        this.maxRetries = intent.getIntExtra(UploadService.PARAM_MAX_RETRIES, 0);
        this.headers = intent.getParcelableArrayListExtra(UploadService.PARAM_REQUEST_HEADERS);
    }

    @Override
    public void run() {

        createNotification();

        int attempts = 0;

        int errorDelay = 1000;
        int maxErrorDelay = 10 * 60 * 1000;

        while (attempts <= maxRetries && shouldContinue) {
            attempts++;
            try {
                this.upload();

                break;
            } catch (Exception exc) {
                if (!shouldContinue) {
                    broadcastCancelled();
                } else if (attempts > maxRetries) {
                    broadcastError(exc);
                } else {
                    Log.w(getClass().getName(), "Error in uploadId " + uploadId + " on attempt " + attempts
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
    }

    public void cancel() {
        this.shouldContinue = false;
    }

    public HttpUploadTask setLastProgressNotificationTime(long lastProgressNotificationTime) {
        this.lastProgressNotificationTime = lastProgressNotificationTime;
        return this;
    }

    public HttpUploadTask setNotificationId(int notificationId) {
        this.notificationId = notificationId;
        return this;
    }

    @SuppressLint("NewApi")
    protected void upload() throws IOException {

        try {
            totalBodyBytes = getBodyLength();

            if (android.os.Build.VERSION.SDK_INT < 19 && totalBodyBytes > Integer.MAX_VALUE)
                throw new IOException("You need Android API version 19 or newer to "
                        + "upload more than 2GB in a single request using "
                        + "fixed size content length. Try switching to "
                        + "chunked mode instead, but make sure your server side supports it!");

            connection = getHttpURLConnection();

            if (customUserAgent != null && !customUserAgent.equals("")) {
                headers.add(new NameValue("User-Agent", customUserAgent));
            }

            setRequestHeaders();

            if (android.os.Build.VERSION.SDK_INT >= 19) {
                connection.setFixedLengthStreamingMode(totalBodyBytes);
            } else {
                connection.setFixedLengthStreamingMode((int) totalBodyBytes);
            }

            requestStream = connection.getOutputStream();

            try {
                writeBody();
            } finally {
                closeInputStream();
            }

            final int serverResponseCode = connection.getResponseCode();

            if (serverResponseCode / 100 == 2) {
                responseStream = connection.getInputStream();
            } else { // getErrorStream if the response code is not 2xx
                responseStream = connection.getErrorStream();
            }
            final String serverResponseMessage = getResponseBodyAsString(responseStream);

            broadcastCompleted(serverResponseCode, serverResponseMessage);

        } finally {
            closeOutputStream();
            closeInputStream();
            closeConnection();
        }
    }

    protected HttpURLConnection getHttpURLConnection() throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod(method);

        return conn;
    }

    /**
     * Implement in derived classes to provide the expected upload in the progress notifications.
     * @return The expected size of the http request body.
     * @throws UnsupportedEncodingException
     */
    protected abstract long getBodyLength() throws UnsupportedEncodingException;

    /**
     * Implement in derived classes to write the body of the http request.
     * @throws IOException
     */
    protected abstract void writeBody() throws IOException;

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
        if (!headers.isEmpty()) {
            for (final NameValue param : headers) {
                connection.setRequestProperty(param.getName(), param.getValue());
            }
        }
    }

    private String getResponseBodyAsString(final InputStream inputStream) {
        StringBuilder outString = new StringBuilder();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                outString.append(line).append("\n");
            }
        } catch (Exception exc) {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception ignored) {
            }
        }

        return outString.toString();
    }

    protected void writeStream(InputStream stream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0 && shouldContinue) {
            requestStream.write(buffer, 0, bytesRead);
            uploadedBodyBytes += bytesRead;
            broadcastProgress(uploadedBodyBytes, totalBodyBytes);
        }
    }

    protected void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        long currentTime = System.currentTimeMillis();
        if (currentTime < lastProgressNotificationTime + UploadService.PROGRESS_REPORT_INTERVAL) {
            return;
        }

        lastProgressNotificationTime = currentTime;

        final Intent intent = new Intent(UploadService.getActionBroadcast());
        intent.putExtra(UploadService.UPLOAD_ID, uploadId);
        intent.putExtra(UploadService.STATUS, UploadService.STATUS_IN_PROGRESS);

        final int percentsProgress = (int) (uploadedBytes * 100 / totalBytes);
        intent.putExtra(UploadService.PROGRESS, percentsProgress);

        intent.putExtra(UploadService.PROGRESS_UPLOADED_BYTES, uploadedBytes);
        intent.putExtra(UploadService.PROGRESS_TOTAL_BYTES, totalBytes);
        service.sendBroadcast(intent);

        updateNotificationProgress((int) uploadedBytes, (int) totalBytes);
    }

    void broadcastCompleted(final int responseCode, final String responseMessage) {

        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        final Intent intent = new Intent(UploadService.getActionBroadcast());
        intent.putExtra(UploadService.UPLOAD_ID, uploadId);
        intent.putExtra(UploadService.STATUS, UploadService.STATUS_COMPLETED);
        intent.putExtra(UploadService.SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(UploadService.SERVER_RESPONSE_MESSAGE, filteredMessage);
        service.sendBroadcast(intent);

        if (responseCode >= 200 && responseCode <= 299)
            updateNotificationCompleted();
        else
            updateNotificationError();

        service.taskCompleted(uploadId);
    }

    void broadcastError(final Exception exception) {

        final Intent intent = new Intent(UploadService.getActionBroadcast());
        intent.putExtra(UploadService.UPLOAD_ID, uploadId);
        intent.putExtra(UploadService.STATUS, UploadService.STATUS_ERROR);
        intent.putExtra(UploadService.ERROR_EXCEPTION, exception);
        service.sendBroadcast(intent);

        updateNotificationError();

        service.taskCompleted(uploadId);
    }

    void broadcastCancelled() {
        final Intent intent = new Intent(UploadService.getActionBroadcast());
        intent.putExtra(UploadService.UPLOAD_ID, uploadId);
        intent.putExtra(UploadService.STATUS, UploadService.STATUS_CANCELLED);
        service.sendBroadcast(intent);

        updateNotificationError();

        service.taskCompleted(uploadId);
    }

    private void createNotification() {
        if (notificationConfig == null) return;

        notification.setContentTitle(notificationConfig.getTitle())
                .setContentText(notificationConfig.getInProgressMessage())
                .setContentIntent(notificationConfig.getPendingIntent(service))
                .setSmallIcon(notificationConfig.getIconResourceID())
                .setProgress(100, 0, true)
                .setOngoing(true);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(uploadId, builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    private void updateNotificationProgress(int uploadedBytes, int totalBytes) {
        if (notificationConfig == null) return;

        notification.setContentTitle(notificationConfig.getTitle())
                .setContentText(notificationConfig.getInProgressMessage())
                .setContentIntent(notificationConfig.getPendingIntent(service))
                .setSmallIcon(notificationConfig.getIconResourceID())
                .setProgress(totalBytes, uploadedBytes, false)
                .setOngoing(true);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(uploadId, builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    private void setRingtone() {

        if(notificationConfig.isRingToneEnabled()) {
            notification.setSound(RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION));
            notification.setOnlyAlertOnce(false);
        }

    }

    private void updateNotificationCompleted() {
        if (notificationConfig == null) return;

        notificationManager.cancel(notificationId);

        if (!notificationConfig.isAutoClearOnSuccess()) {
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getCompletedMessage())
                    .setContentIntent(notificationConfig.getPendingIntent(service))
                    .setAutoCancel(notificationConfig.isClearOnAction())
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            setRingtone();

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            notificationManager.notify(notificationId + 1, notification.build());
        }
    }

    private void updateNotificationError() {
        if (notificationConfig == null) return;

        notificationManager.cancel(notificationId);

        notification.setContentTitle(notificationConfig.getTitle())
                .setContentText(notificationConfig.getErrorMessage())
                .setContentIntent(notificationConfig.getPendingIntent(service))
                .setAutoCancel(notificationConfig.isClearOnAction())
                .setSmallIcon(notificationConfig.getIconResourceID())
                .setProgress(0, 0, false).setOngoing(false);
        setRingtone();

        // this is needed because the main notification used to show progress is ongoing
        // and a new one has to be created to allow the user to dismiss it
        notificationManager.notify(notificationId + 1, notification.build());
    }
}