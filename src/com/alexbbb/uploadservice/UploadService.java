package com.alexbbb.uploadservice;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

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
    protected static final String PARAM_CUSTOM_USER_AGENT = "customUserAgent";
    protected static final String PARAM_MAX_RETRIES = "maxRetries";

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

    // indicates if the upload request should be continued
    private static volatile boolean shouldContinue = true;

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
            intent.putExtra(PARAM_CUSTOM_USER_AGENT, task.getCustomUserAgent());
            intent.putExtra(PARAM_MAX_RETRIES, task.getMaxRetries());
            intent.putParcelableArrayListExtra(PARAM_FILES, task.getFilesToUpload());
            intent.putParcelableArrayListExtra(PARAM_REQUEST_HEADERS, task.getHeaders());
            intent.putParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS, task.getParameters());

            task.getContext().startService(intent);
        }
    }

    /**
     * Stops the currently active upload task.
     */
    public static void stopCurrentUpload() {
        shouldContinue = false;
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
                final String customUserAgent = intent.getStringExtra(PARAM_CUSTOM_USER_AGENT);
                final int maxRetries = intent.getIntExtra(PARAM_MAX_RETRIES, 0);
                final ArrayList<FileToUpload> files = intent.getParcelableArrayListExtra(PARAM_FILES);
                final ArrayList<NameValue> headers = intent.getParcelableArrayListExtra(PARAM_REQUEST_HEADERS);
                final ArrayList<NameValue> parameters = intent.getParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS);

                lastPublishedProgress = 0;
                shouldContinue = true;
                wakeLock.acquire();
                int attempts = 0;

                createNotification();

                while (attempts <= maxRetries && shouldContinue) {
                    attempts++;
                    try {
                        handleFileUpload(uploadId, url, method, files, headers, parameters, customUserAgent);
                    } catch (Exception exc) {
                        if (attempts > maxRetries || !shouldContinue)
                            broadcastError(uploadId, exc);
                        else
                            Log.w(getClass().getName(), "Error in uploadId " + uploadId + " on attempt " + attempts,
                                  exc);
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private
            void
            handleFileUpload(final String uploadId, final String url, final String method,
                             final ArrayList<FileToUpload> filesToUpload, final ArrayList<NameValue> requestHeaders,
                             final ArrayList<NameValue> requestParameters, final String customUserAgent)
                                                                                                        throws IOException {

        final String boundary = getBoundary();
        final byte[] boundaryBytes = getBoundaryBytes(boundary);

        HttpURLConnection conn = null;
        OutputStream requestStream = null;
        InputStream responseStream = null;

        try {
            // get the content length of the entire HTTP/Multipart request body
            long parameterBytes = getRequestParametersBytes(requestParameters, boundaryBytes.length);
            final long totalFileBytes = getFileBytes(filesToUpload, boundaryBytes.length);
            final byte[] trailer = getTrailerBytes(boundary);
            final long bodyLength = parameterBytes + totalFileBytes + trailer.length;

            if (android.os.Build.VERSION.SDK_INT < 19 && bodyLength > Integer.MAX_VALUE)
                throw new IOException("You need Android API version 19 or newer to "
                        + "upload more than 2GB in a single request using "
                        + "fixed size content length. Try switching to "
                        + "chunked mode instead, but make sure your server side supports it!");

            conn = getMultipartHttpURLConnection(url, method, boundary, filesToUpload.size());

            if (customUserAgent != null && !customUserAgent.equals("")) {
                requestHeaders.add(new NameValue("User-Agent", customUserAgent));
            }

            setRequestHeaders(conn, requestHeaders);

            if (android.os.Build.VERSION.SDK_INT >= 19) {
                conn.setFixedLengthStreamingMode(bodyLength);
            } else {
                conn.setFixedLengthStreamingMode((int) bodyLength);
            }

            requestStream = conn.getOutputStream();

            setRequestParameters(requestStream, requestParameters, boundaryBytes);
            uploadFiles(uploadId, requestStream, filesToUpload, boundaryBytes);

            requestStream.write(trailer, 0, trailer.length);
            final int serverResponseCode = conn.getResponseCode();

            if (serverResponseCode / 100 == 2) {
                responseStream = conn.getInputStream();
            } else { // getErrorStream if the response code is not 2xx
                responseStream = conn.getErrorStream();
            }
            final String serverResponseMessage = getResponseBodyAsString(responseStream);

            broadcastCompleted(uploadId, serverResponseCode, serverResponseMessage);

        } finally {
            closeOutputStream(requestStream);
            closeInputStream(responseStream);
            closeConnection(conn);
        }
    }

    private String getResponseBodyAsString(final InputStream inputStream) {
        StringBuilder outString = new StringBuilder();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                outString.append(line);
            }
        } catch (Exception exc) {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception readerExc) {
            }
        }

        return outString.toString();
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

    private HttpURLConnection getMultipartHttpURLConnection(final String url, final String method,
                                                            final String boundary, int totalFiles) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod(method);
        if (totalFiles <= 1) {
            conn.setRequestProperty("Connection", "close");
        } else {
            conn.setRequestProperty("Connection", "Keep-Alive");
        }
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
            long
            getRequestParametersBytes(final ArrayList<NameValue> requestParameters, final long boundaryBytesLength)
                                                                                                                   throws UnsupportedEncodingException {
        long parametersBytes = 0;

        if (!requestParameters.isEmpty()) {
            for (final NameValue parameter : requestParameters) {
                // the bytes needed for every parameter are the sum of the boundary bytes
                // and the bytes occupied by the parameter. Check setRequestParameters method
                parametersBytes += boundaryBytesLength + parameter.getBytes().length;
            }
        }

        return parametersBytes;
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
            if (!shouldContinue)
                continue;

            requestStream.write(boundaryBytes, 0, boundaryBytes.length);
            byte[] headerBytes = file.getMultipartHeader();
            requestStream.write(headerBytes, 0, headerBytes.length);

            final InputStream stream = file.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            try {
                while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0 && shouldContinue) {
                    requestStream.write(buffer, 0, bytesRead);
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

    private
            long
            getFileBytes(final ArrayList<FileToUpload> filesToUpload, long boundaryBytesLength)
                                                                                               throws UnsupportedEncodingException {
        long total = 0;

        for (FileToUpload file : filesToUpload) {
            total += file.getTotalMultipartBytes(boundaryBytesLength);
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
        wakeLock.release();
    }

    private void broadcastError(final String uploadId, final Exception exception) {

        updateNotificationError();

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);
        sendBroadcast(intent);
        wakeLock.release();

    }

    private void createNotification() {
        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getMessage())
                .setContentIntent(notificationConfig.getPendingIntent(this))
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(100, 0, true).setOngoing(true);

        startForeground(UPLOAD_NOTIFICATION_ID, notification.build());
    }

    private void updateNotificationProgress(final int progress) {
        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getMessage())
                .setContentIntent(notificationConfig.getPendingIntent(this))
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(100, progress, false)
                .setOngoing(true);

        startForeground(UPLOAD_NOTIFICATION_ID, notification.build());
    }

    private void updateNotificationCompleted() {
        stopForeground(notificationConfig.isAutoClearOnSuccess());

        if (!notificationConfig.isAutoClearOnSuccess()) {
            notification.setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getCompleted())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(0, 0, false).setOngoing(false);

            notificationManager.notify(UPLOAD_NOTIFICATION_ID_DONE, notification.build());
        }
    }

    private void updateNotificationError() {
        stopForeground(false);

        notification.setContentTitle(notificationConfig.getTitle()).setContentText(notificationConfig.getError())
                .setContentIntent(notificationConfig.getPendingIntent(this))
                .setSmallIcon(notificationConfig.getIconResourceID()).setProgress(0, 0, false).setOngoing(false);

        notificationManager.notify(UPLOAD_NOTIFICATION_ID_DONE, notification.build());
    }
}
