package net.gotev.uploadservice;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import net.gotev.uploadservice.logger.UploadServiceLogger;
import net.gotev.uploadservice.network.BodyWriter;
import net.gotev.uploadservice.network.HttpRequest;
import net.gotev.uploadservice.network.HttpStack;
import net.gotev.uploadservice.network.ServerResponse;

import java.io.UnsupportedEncodingException;

/**
 * Generic HTTP Upload Task.<br>
 * Subclass to create your custom upload task.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 * @author mabdurrahman
 */
public abstract class HttpUploadTask extends UploadTask
        implements HttpRequest.RequestBodyDelegate, BodyWriter.OnStreamWriteListener {

    private static final String LOG_TAG = HttpUploadTask.class.getSimpleName();

    /**
     * {@link HttpRequest} used to perform the upload task.
     */
    private HttpRequest request;

    protected HttpUploadTaskParameters getHttpParams() {
        return (HttpUploadTaskParameters) getParams().getAdditionalParams();
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
    protected void upload(@NonNull HttpStack httpStack) throws Exception {

        UploadServiceLogger.INSTANCE.debug(LOG_TAG, "Starting upload task with ID " + params.getId());

        try {
            HttpUploadTaskParameters httpParams = getHttpParams();

            getSuccessfullyUploadedFiles().clear();
            setTotalBytes(getBodyLength());

            if (httpParams.isCustomUserAgentDefined()) {
                httpParams.addHeader("User-Agent", httpParams.customUserAgent);
            } else {
                httpParams.addHeader("User-Agent", "AndroidUploadService/" + BuildConfig.VERSION_NAME);
            }

            request = httpStack.newRequest(httpParams.method, params.getServerUrl())
                    .setHeaders(httpParams.getRequestHeaders())
                    .setTotalBodyBytes(getTotalBytes(), httpParams.usesFixedLengthStreamingMode);

            final ServerResponse response = request.getResponse(this);
            UploadServiceLogger.INSTANCE.debug(LOG_TAG, "Server responded with code " + response.getCode()
                            + " and body " + response.getBodyString()
                            + " to upload with ID: " + params.getId());

            // Broadcast completion only if the user has not cancelled the operation.
            // It may happen that when the body is not completely written and the client
            // closes the connection, no exception is thrown here, and the server responds
            // with an HTTP status code. Without this, what happened was that completion was
            // broadcasted and then the cancellation. That behaviour was not desirable as the
            // library user couldn't execute code on user cancellation.
            if (getShouldContinue()) {
                broadcastCompleted(response);
            }

        } finally {
            if (request != null)
                request.close();
        }
    }

    /**
     * Implement in subclasses to provide the expected upload in the progress notifications.
     * @return The expected size of the http request body.
     * @throws UnsupportedEncodingException
     */
    protected abstract long getBodyLength() throws UnsupportedEncodingException;

    // BodyWriter.OnStreamWriteListener methods implementation

    @Override
    public boolean shouldContinueWriting() {
        return getShouldContinue();
    }

    @Override
    public void onBytesWritten(int bytesWritten) {
        broadcastProgress(bytesWritten);
    }

}
