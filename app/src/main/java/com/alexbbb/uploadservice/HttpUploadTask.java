package com.alexbbb.uploadservice;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.SystemClock;
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
 */
abstract class HttpUploadTask {

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

    protected long totalBodyBytes;
    protected long uploadedBodyBytes;

    HttpUploadTask(UploadService service, Intent intent) {

        this.service = service;

        this.uploadId = intent.getStringExtra(UploadService.PARAM_ID);
        this.url = intent.getStringExtra(UploadService.PARAM_URL);
        this.method = intent.getStringExtra(UploadService.PARAM_METHOD);
        this.customUserAgent = intent.getStringExtra(UploadService.PARAM_CUSTOM_USER_AGENT);
        this.maxRetries = intent.getIntExtra(UploadService.PARAM_MAX_RETRIES, 0);
        this.headers = intent.getParcelableArrayListExtra(UploadService.PARAM_REQUEST_HEADERS);
    }

    public void run() {
        int attempts = 0;

        int errorDelay = 1000;
        int maxErrorDelay = 10 * 60 * 1000;

        while (attempts <= maxRetries && shouldContinue) {
            attempts++;
            try {
                this.upload();

                break;
            } catch (Exception exc) {
                if (attempts > maxRetries || !shouldContinue) {
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

    protected void broadcastProgress(long uploadedBytes, long totalBytes) {
        this.service.broadcastProgress(uploadId, uploadedBytes, totalBytes);
    }

    private void broadcastError(Exception exc) {
        this.service.broadcastError(uploadId, exc);
    }

    private void broadcastCompleted(final int responseCode, final String responseMessage) {
        this.service.broadcastCompleted(uploadId, responseCode, responseMessage);
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
            } catch (Exception exc) {
            }
        }
    }

    private void closeOutputStream() {
        if (requestStream != null) {
            try {
                requestStream.flush();
                requestStream.close();
            } catch (Exception exc) {
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception exc) {
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
            } catch (Exception readerExc) {
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
}
