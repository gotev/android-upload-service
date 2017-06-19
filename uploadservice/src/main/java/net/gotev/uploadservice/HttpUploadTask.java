package net.gotev.uploadservice;

import android.annotation.SuppressLint;
import android.content.Intent;

import net.gotev.uploadservice.http.BodyWriter;
import net.gotev.uploadservice.http.HttpConnection;

import java.io.IOException;
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
        implements HttpConnection.RequestBodyDelegate, BodyWriter.OnStreamWriteListener {

    private static final String LOG_TAG = HttpUploadTask.class.getSimpleName();

    /**
     * Contains all the parameters set in {@link HttpUploadRequest}.
     */
    protected HttpUploadTaskParameters httpParams = null;

    /**
     * {@link HttpConnection} used to perform the upload task.
     */
    private HttpConnection connection;

    @Override
    protected void init(UploadService service, Intent intent) throws IOException {
        super.init(service, intent);
        this.httpParams = intent.getParcelableExtra(HttpUploadTaskParameters.PARAM_HTTP_TASK_PARAMETERS);
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

        getSuccessfullyUploadedFiles().clear();
        totalBytes = getBodyLength();

        if (params.isChunkUpload()) {
            UploadFile currentFile = params.getFiles().get(0);
            currentFile.generateChunk();
            UploadFile currentChunk = currentFile.getCurrentChunk();
            if (currentChunk != null) {
                uploadedBytes = currentChunk.getStartByte();
                final String contentRange = Long.toString(currentChunk.getStartByte()) + "-" + Long.toString(currentChunk.getEndByte()) + "/" + currentFile.length(service);
                httpParams.addRequestHeader("Content-Range", contentRange);

                Logger.debug(LOG_TAG, "Starting chunk " + contentRange);

                ServerResponse response = startRequest(httpParams, currentChunk.length(service));

                if (shouldContinue) {
                    long endByte = currentChunk.getEndByte() + 1;
                    Logger.debug(LOG_TAG, "Setting next chunk start point: " + Long.toString(endByte));
                    params.getFiles().get(0).setStartByte(endByte);

                    broadcastChunkCompleted(response);

                    closeConnection();

                    if (endByte < currentFile.length(service)) {
                        upload();
                    } else {
                        broadcastCompleted(response);
                        closeConnection();
                    }

                }
            }
        } else {
            uploadedBytes = 0;
            ServerResponse response = startRequest(httpParams, totalBytes);

            // Broadcast completion only if the user has not cancelled the operation.
            // It may happen that when the body is not completely written and the client
            // closes the connection, no exception is thrown here, and the server responds
            // with an HTTP status code. Without this, what happened was that completion was
            // broadcasted and then the cancellation. That behaviour was not desirable as the
            // library user couldn't execute code on user cancellation.
            if (shouldContinue) {
                broadcastCompleted(response);
                closeConnection();
            }
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
        return shouldContinue;
    }

    @Override
    public void onBytesWritten(int bytesWritten) {
        uploadedBytes += bytesWritten;
        broadcastProgress(uploadedBytes, totalBytes);
    }

    /**
     * Implementation of the connection request logic.
     * @throws IOException if an error occurs
     */
    protected ServerResponse startRequest(HttpUploadTaskParameters httpParams, long totalBytes) throws IOException {
        if (httpParams.isCustomUserAgentDefined()) {
            httpParams.addRequestHeader("User-Agent", httpParams.getCustomUserAgent());
        }

        connection = UploadService.HTTP_STACK
                .createNewConnection(httpParams.getMethod(), params.getServerUrl())
                .setHeaders(httpParams.getRequestHeaders())
                .setTotalBodyBytes(totalBytes, httpParams.isUsesFixedLengthStreamingMode());

        final ServerResponse response = connection.getResponse(this);
        Logger.debug(LOG_TAG, "Server responded with HTTP " + response.getHttpCode()
                + " to upload with ID: " + params.getId());

        return response;
    }

    void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

}
