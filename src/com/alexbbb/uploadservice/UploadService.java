package com.alexbbb.uploadservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

/**
 * Service to upload files as a multi-part form data in background using HTTP POST
 * with notification center progress display.
 *
 * @author alexbbb (Alex Gotev)
 *
 */
public class UploadService extends IntentService {

    private static final String SERVICE_NAME = UploadService.class.getName();

    private static final int BUFFER_SIZE = 4096;
    private static final String NEW_LINE = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private static final String ACTION_UPLOAD = "com.alexbbb.uploadservice.action.upload";
    private static final String PARAM_URL = "url";
    private static final String PARAM_FILES = "files";
    private static final String PARAM_REQUEST_HEADERS = "requestHeaders";
    private static final String PARAM_REQUEST_PARAMETERS = "requestParameters";

    public static final String BROADCAST_ACTION = "com.alexbbb.uploadservice.broadcast.status";
    public static final String STATUS = "status";
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_ERROR = 3;
    public static final String PROGRESS = "progress";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private NotificationManager notificationManager;
    private int lastPublishedProgress;

    /**
     * Utility method that creates the intent that starts the background
     * file upload service.
     *
     * @param context context from which to start the service
     * @param url server URL where to upload the files
     * @param filesToUpload list of the files to upload
     * @param requestHeaders additional request headers
     * @param requestParameters additional request parameters
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public static void startUpload(final Context context,
                                   final String url,
                                   final ArrayList<FileToUpload> filesToUpload,
                                   final ArrayList<NameValue> requestHeaders,
                                   final ArrayList<NameValue> requestParameters)
                                   throws IllegalArgumentException,
                                          MalformedURLException {
        validateParameters(url, filesToUpload, requestHeaders, requestParameters);

        final Intent intent = new Intent(context, UploadService.class);

        intent.setAction(ACTION_UPLOAD);
        intent.putExtra(PARAM_URL, url);
        intent.putParcelableArrayListExtra(PARAM_FILES, filesToUpload);
        intent.putParcelableArrayListExtra(PARAM_REQUEST_HEADERS, requestHeaders);
        intent.putParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS, requestParameters);

        context.startService(intent);
    }

    private static void validateParameters(final String url,
                                           final ArrayList<FileToUpload> filesToUpload,
                                           final ArrayList<NameValue> requestHeaders,
                                           final ArrayList<NameValue> requestParameters)
                                           throws MalformedURLException {
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (url.startsWith("https")) {
            throw new IllegalArgumentException("HTTPS is not supported yet");
        }

        //Check if the URL is valid
        new URL(url);

        if (filesToUpload == null || filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You must pass a list with at least one file");
        }

        if (requestHeaders == null) {
            throw new IllegalArgumentException("Request headers must not be null. " +
                                               "If you don't want to add any headers, " +
                                               "pass an empty list instead");
        }

        if (requestParameters == null) {
            throw new IllegalArgumentException("Request parameters must not be null. " +
                                               "If you don't want to add any parameters, " +
                                               "pass an empty list instead");
        }
    }

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (ACTION_UPLOAD.equals(action)) {
                final String url = intent.getStringExtra(PARAM_URL);
                final ArrayList<FileToUpload> files = intent.getParcelableArrayListExtra(PARAM_FILES);
                final ArrayList<NameValue> headers = intent.getParcelableArrayListExtra(PARAM_REQUEST_HEADERS);
                final ArrayList<NameValue> parameters = intent.getParcelableArrayListExtra(PARAM_REQUEST_PARAMETERS);

                lastPublishedProgress = 0;
                try {
                    handleFileUpload(url, files, headers, parameters);
                } catch (Exception exception) {
                    broadcastError(exception);
                }
            }
        }
    }

    private void handleFileUpload(final String url,
                                  final ArrayList<FileToUpload> filesToUpload,
                                  final ArrayList<NameValue> requestHeaders,
                                  final ArrayList<NameValue> requestParameters)
            throws IOException {

        final String boundary = getBoundary();
        final byte[] boundaryBytes = getBoundaryBytes(boundary);

        HttpURLConnection conn = null;
        OutputStream requestStream = null;

        try {
            conn = getMultipartHttpURLConnection(url, boundary);

            setRequestHeaders(conn, requestHeaders);

            requestStream = conn.getOutputStream();
            setRequestParameters(requestStream, requestParameters, boundaryBytes);

            uploadFiles(requestStream, filesToUpload, boundaryBytes);

            final byte[] trailer = getTrailerBytes(boundary);
            requestStream.write(trailer, 0, trailer.length);

            final int serverResponseCode = conn.getResponseCode();
            final String serverResponseMessage = conn.getResponseMessage();

            broadcastCompleted(serverResponseCode, serverResponseMessage);

        } finally {
            closeOutputStream(requestStream);
            closeConnection(conn);
        }
    }

    private String getBoundary() {
        final StringBuilder builder = new StringBuilder();

        builder.append("---------------------------")
               .append(System.currentTimeMillis());

        return builder.toString();
    }

    private byte[] getBoundaryBytes(final String boundary)
            throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE)
               .append(TWO_HYPHENS)
               .append(boundary)
               .append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private byte[] getTrailerBytes(final String boundary)
            throws UnsupportedEncodingException {
        final StringBuilder builder = new StringBuilder();

        builder.append(NEW_LINE)
               .append(TWO_HYPHENS)
               .append(boundary)
               .append(TWO_HYPHENS)
               .append(NEW_LINE);

        return builder.toString().getBytes("US-ASCII");
    }

    private HttpURLConnection getMultipartHttpURLConnection(final String url,
                                                            final String boundary)
            throws IOException, ProtocolException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        return conn;
    }

    private void setRequestHeaders(final HttpURLConnection conn,
                                   final ArrayList<NameValue> requestHeaders) {
        if (!requestHeaders.isEmpty()) {
            for (final NameValue param : requestHeaders) {
                conn.setRequestProperty(param.getName(), param.getValue());
            }
        }
    }

    private void setRequestParameters(final OutputStream requestStream,
                                      final ArrayList<NameValue> requestParameters,
                                      final byte[] boundaryBytes)
            throws IOException, UnsupportedEncodingException {
        if (!requestParameters.isEmpty()) {

            for (final NameValue parameter : requestParameters) {
                requestStream.write(boundaryBytes, 0, boundaryBytes.length);
                byte[] formItemBytes = parameter.getBytes();
                requestStream.write(formItemBytes, 0, formItemBytes.length);
            }
        }
        requestStream.write(boundaryBytes, 0, boundaryBytes.length);
    }

    private void uploadFiles(OutputStream requestStream,
                             final ArrayList<FileToUpload> filesToUpload,
                             final byte[] boundaryBytes)
            throws UnsupportedEncodingException,
                   IOException,
                   FileNotFoundException {

        final long totalBytes = getTotalBytes(filesToUpload);
        long uploadedBytes = 0;

        for (FileToUpload file : filesToUpload) {
            byte[] headerBytes = file.getMultipartHeader();
            requestStream.write(headerBytes, 0, headerBytes.length);

            final InputStream stream = file.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            long bytesRead;

            try {
                while ((bytesRead = stream.read(buffer, 0, buffer.length)) > 0) {
                    requestStream.write(buffer, 0, buffer.length);
                    uploadedBytes += bytesRead;
                    broadcastProgress(uploadedBytes, totalBytes);
                }
            } finally {
                closeInputStream(stream);
            }
            requestStream.write(boundaryBytes, 0, boundaryBytes.length);
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
            } catch (Exception exc) { }
        }
    }

    private void closeOutputStream(final OutputStream stream) {
        if (stream != null) {
            try {
                stream.flush();
                stream.close();
            } catch (Exception exc) { }
        }
    }

    private void closeConnection(final HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception exc) { }
        }
    }

    private void broadcastProgress(final long uploadedBytes, final long totalBytes) {

        final int progress = (int) (uploadedBytes * 100 / totalBytes);
        if (progress <= lastPublishedProgress) return;
        lastPublishedProgress = progress;

        final Intent intent = new Intent();

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra(STATUS, STATUS_IN_PROGRESS);
        intent.putExtra(PROGRESS, progress);

        sendBroadcast(intent);
    }

    private void broadcastCompleted(final int responseCode, final String responseMessage) {

        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        final Intent intent = new Intent();

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra(STATUS, STATUS_COMPLETED);
        intent.putExtra(SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(SERVER_RESPONSE_MESSAGE, filteredMessage);

        sendBroadcast(intent);
    }

    private void broadcastError(final Exception exception) {
        final Intent intent = new Intent();

        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra(STATUS, STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);

        sendBroadcast(intent);
    }
}
