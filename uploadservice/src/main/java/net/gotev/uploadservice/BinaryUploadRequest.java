package net.gotev.uploadservice;

import android.content.Context;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * Binary file upload request. The binary upload uses a single file as the raw body of the
 * upload request.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class BinaryUploadRequest extends HttpUploadRequest {

    /**
     * Creates a binary file upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request.<br>
     *                 It can be whatever string you want, as long as it's unique.
     *                 If you set it to null or an empty string, an UUID will be automatically
     *                 generated.<br> It's advised to keep a reference to it in your code,
     *                 so when you receive status updates in {@link UploadServiceBroadcastReceiver},
     *                 you know to which upload they refer to.
     * @param serverUrl URL of the server side script that will handle the multipart form upload.
     *                  E.g.: http://www.yourcompany.com/your/script
     */
    public BinaryUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        super(context, uploadId, serverUrl);
    }

    /**
     * Creates a new binaryupload request and automatically generates an upload id, that will
     * be returned when you call {@link HttpUploadRequest#startUpload()}.
     *
     * @param context application context
     * @param serverUrl URL of the server side script that will handle the multipart form upload.
     *                  E.g.: http://www.yourcompany.com/your/script
     */
    public BinaryUploadRequest(final Context context, final String serverUrl) {
        this(context, null, serverUrl);
    }

    @Override
    protected Class getTaskClass() {
        return BinaryUploadTask.class;
    }

    /**
     * Sets the file used as raw body of the upload request.
     *
     * @param path Absolute path to the file that you want to upload
     * @throws FileNotFoundException if the file to upload does not exist
     * @return {@link BinaryUploadRequest}
     */
    public BinaryUploadRequest setFileToUpload(String path) throws FileNotFoundException {
        params.getFiles().clear();
        params.addFile(new UploadFile(path));
        return this;
    }

    @Override
    public BinaryUploadRequest setNotificationConfig(UploadNotificationConfig config) {
        super.setNotificationConfig(config);
        return this;
    }

    @Override
    public BinaryUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        super.setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFiles);
        return this;
    }

    @Override
    public BinaryUploadRequest addHeader(String headerName, String headerValue) {
        super.addHeader(headerName, headerValue);
        return this;
    }

    @Override
    public BinaryUploadRequest setBasicAuth(final String username, final String password) {
        super.setBasicAuth(username, password);
        return this;
    }

    @Override
    public BinaryUploadRequest setMethod(String method) {
        super.setMethod(method);
        return this;
    }

    @Override
    public BinaryUploadRequest setCustomUserAgent(String customUserAgent) {
        super.setCustomUserAgent(customUserAgent);
        return this;
    }

    @Override
    public BinaryUploadRequest setMaxRetries(int maxRetries) {
        super.setMaxRetries(maxRetries);
        return this;
    }

    @Override
    public BinaryUploadRequest setUsesFixedLengthStreamingMode(boolean fixedLength) {
        super.setUsesFixedLengthStreamingMode(fixedLength);
        return this;
    }

    @Override
    public HttpUploadRequest addParameter(String paramName, String paramValue) {
        logDoesNotSupportParameters();
        return this;
    }

    @Override
    public HttpUploadRequest addArrayParameter(String paramName, String... array) {
        logDoesNotSupportParameters();
        return this;
    }

    @Override
    public HttpUploadRequest addArrayParameter(String paramName, List<String> list) {
        logDoesNotSupportParameters();
        return this;
    }

    private void logDoesNotSupportParameters() {
        Logger.error(getClass().getSimpleName(),
                     "This upload method does not support adding parameters");
    }
}
