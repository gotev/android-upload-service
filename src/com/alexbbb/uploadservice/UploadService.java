package com.alexbbb.uploadservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

/**
 * Service to upload files as a multi-part form data in background using HTTP POST with notification center progress
 * display.
 * 
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 */
public class UploadService extends IntentService {

    private static final String SERVICE_NAME = UploadService.class.getName();
    private static final String TAG = "AndroidUploadService";

    private static final int UPLOAD_NOTIFICATION_ID = 1234; // Something unique
    private static final int UPLOAD_NOTIFICATION_ID_DONE = 1235; // Something unique
    private static final int BUFFER_SIZE = 4096;
    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    public static String NAMESPACE = "com.alexbbb";

    private static final String ACTION_UPLOAD_SUFFIX = ".uploadservice.action.upload";
    protected static final String PARAM_NOTIFICATION_CONFIG = "notificationConfig";
    protected static final String PARAM_ID = "id";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_METHOD = "method";
    protected static final String PARAM_FILES = "files";
    protected static final String PARAM_REQUEST_HEADERS = "requestHeaders";
    protected static final String PARAM_REQUEST_PARAMETERS = "requestParameters";

    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";
    public static final String UPLOAD_ID = "id";
    public static final String STATUS = "status";
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_ERROR = 3;
    public static final String PROGRESS = "progress";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private NotificationManager notificationManager;
    private Builder notification;
    private PowerManager.WakeLock wakeLock;
    private UploadNotificationConfig notificationConfig;
    private int lastPublishedProgress;

    public static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    public static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    /**
     * Utility method that creates the intent that starts the background file upload service.
     * 
     * @param task object containing the upload request
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public static void startUpload(final UploadRequest task) throws IllegalArgumentException, MalformedURLException {

        if (task == null) {
            throw new IllegalArgumentException("Can't pass an empty task!");

        } else {
            task.validate();

            final Intent intent = new Intent(task.getContext(), UploadService.class);

            intent.setAction(getActionUpload());
            intent.putExtra(PARAM_NOTIFICATION_CONFIG, task.getNotificationConfig());
            intent.putExtra(PARAM_ID, task.getUploadId());
            intent.putExtra(PARAM_URL, task.getServerUrl());
            intent.putExtra(PARAM_METHOD, task.getMethod());
            intent.putParcelableArrayListExtra(PARAM_FILES, task.getFilesToUpload());
            intent.putParcelableArrayListExtra(PARAM_REQUEST_HEADERS, task.getHeaders());
            intent.putParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS, task.getParameters());

            task.getContext().startService(intent);
        }
    }

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = new NotificationCompat.Builder(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (getActionUpload().equals(action)) {
                notificationConfig = intent.getParcelableExtra(PARAM_NOTIFICATION_CONFIG);
                final String uploadId = intent.getStringExtra(PARAM_ID);
                final String url = intent.getStringExtra(PARAM_URL);
                final String method = intent.getStringExtra(PARAM_METHOD);
                final ArrayList<FileToUpload> files = intent.getParcelableArrayListExtra(PARAM_FILES);
                final ArrayList<NameValue> headers = intent.getParcelableArrayListExtra(PARAM_REQUEST_HEADERS);
                final ArrayList<NameValue> parameters = intent.getParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS);

                lastPublishedProgress = 0;
                wakeLock.acquire();
                try {
                    createNotification();
                    handleFileUpload(uploadId, url, method, files, headers, parameters);
                } catch (Exception exception) {
                    broadcastError(uploadId, exception);
                } finally {
                    wakeLock.release();
                }
            }
        }
    }

    private void handleFileUpload(final String uploadId, final String url, final String method,
                                  final ArrayList<FileToUpload> filesToUpload,
                                  final ArrayList<NameValue> requestHeaders,
                                  final ArrayList<NameValue> requestParameters) throws IOException {

        final String boundary = getBoundary();
        final byte[] boundaryBytes = getBoundaryBytes(boundary);

        HttpURLConnection conn = null;
        OutputStream requestStream = null;

        try {
            conn = getMultipartHttpURLConnection(url, method, boundary);

            setRequestHeaders(conn, requestHeaders);

            requestStream = conn.getOutputStream();
            setRequestParameters(requestStream, requestParameters, boundaryBytes);

            uploadFiles(uploadId, requestStream, filesToUpload, boundaryBytes);

            final byte[] trailer = getTrailerBytes(boundary);
            requestStream.write(trailer, 0, trailer.length);
            final int serverResponseCode = conn.getResponseCode();
            final String serverResponseMessage = conn.getResponseMessage();

            broadcastCompleted(uploadId, serverResponseCode, serverResponseMessage);

        } finally {
            closeOutputStream(requestStream);
            closeConnection(conn);
        }
    }

    private String getBoundary() {
        final StringBuilder builder = new StringBuilder();

        builder.append("---------------------------").append(System.currentTimeMillis());

        return builder.toString();
    }

    private byte[] getBoundaryBytes(final String boundary) throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private byte[] getTrailerBytes(final String boundary) throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE).append(TWO_HYPHENS).append(boundary).append(TWO_HYPHENS).append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private
            HttpURLConnection
            getMultipartHttpURLConnection(final String url, final String method, final String boundary)
                                                                                                       throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setChunkedStreamingMode(0);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        return conn;
    }

    private void setRequestHeaders(final HttpURLConnection conn, final ArrayList<NameValue> requestHeaders) {
        if (!requestHeaders.isEmpty()) {
            for (final NameValue param : requestHeaders) {
                conn.setRequestProperty(param.getName(), param.getValue());
            }
        }
    }

    private void setRequestParameters(final OutputStream requestStream, final ArrayList<NameValue> requestParameters,
                                      final byte[] boundaryBytes) throws IOException, UnsupportedEncodingException {
        if (!requestParameters.isEmpty()) {

            for (final NameValue parameter : requestParameters) {
                requestStream.write(boundaryBytes, 0, boundaryBytes.length);
                byte[] formItemBytes = parameter.getBytes();
                requestStream.write(formItemBytes, 0, formItemBytes.length);
            }
        }
    }

    private
            void
            uploadFiles(final String uploadId, final OutputStream requestStream,
                        final ArrayList<FileToUpload> filesToUpload, final byte[] boundaryBytes)
                                                                                                throws UnsupportedEncodingException,
                                                                                                IOException,
                                                                                                FileNotFoundException {

        final long totalBytes = getTotalBytes(filesToUpload);
        long uploadedBytes = 0;

        for (FileToUpload file : filesToUpload) {
            requestStream.write(boundaryBytes, 0, boundaryBytes.length);
            byte[] headerBytes = file.getMultipartHeader();
            requestStream.write(headerBytes, 0, headerBytes.length);

            final InputStream stream = file.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesRead;

            try {
                while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0) {
                    requestStream.write(buffer, 0, buffer.length);
                    uploadedBytes += bytesRead;
                    broadcastProgress(uploadId, uploadedBytes, totalBytes);
                }
            } finally {
                closeInputStream(stream);
            }
        }
    }

    private long getTotalBytes(final ArrayList<FileToUpload> filesToUpload) {
        long total = 0;

        for (FileToUpload file : filesToUpload) {
            total += file.length();
        }

        return total;
    }

    private void closeInputStream(final InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception exc) {
            }
        }
    }

    private void closeOutputStream(final OutputStream stream) {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (Exception exc) {
            }
        }
    }

    private void closeConnection(final HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception exc) {
            }
        }
    }

    private void broadcastProgress(final String uploadId, final long uploadedBytes, final long totalBytes) {

        final int progress = (int) (uploadedBytes * 100 / totalBytes);
        if (progress <= lastPublishedProgress)
            return;
        lastPublishedProgress = progress;

        updateNotificationProgress(progress);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_IN_PROGRESS);
        intent.putExtra(PROGRESS, progress);
        sendBroadcast(intent);
    }

    private void broadcastCompleted(final String uploadId, final int responseCode, final String responseMessage) {

        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        if (responseCode >= 200 && responseCode <= 299)
            updateNotificationCompleted();
        else
            updateNotificationError();

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_COMPLETED);
        intent.putExtra(SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(SERVER_RESPONSE_MESSAGE, filteredMessage);
        sendBroadcast(intent);
    }

    private void broadcastError(final String uploadId, final Exception exception) {

        updateNotificationError();

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);
        sendBroadcast(intent);
    }

    private void createNotification() {
        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getMessage())
                .setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(100, 0, true).setOngoing(true);

        startForeground(UPLOAD_NOTIFICATION_ID, notification.build());
    }

    private void updateNotificationProgress(final int progress) {
        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getMessage())
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(100, progress, false)
                .setOngoing(true);

        startForeground(UPLOAD_NOTIFICATION_ID, notification.build());
    }

    private void updateNotificationCompleted() {
        stopForeground(notificationConfig.isAutoClearOnSuccess());

        if (!notificationConfig.isAutoClearOnSuccess()) {
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getCompleted())
                    .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(0, 0, false).setOngoing(false);

            notificationManager.notify(UPLOAD_NOTIFICATION_ID_DONE, notification.build());
        }
    }

    private void updateNotificationError() {
        stopForeground(false);

        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getError())
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(0, 0, false).setOngoing(false);

        notificationManager.notify(UPLOAD_NOTIFICATION_ID_DONE, notification.build());
    }
}
