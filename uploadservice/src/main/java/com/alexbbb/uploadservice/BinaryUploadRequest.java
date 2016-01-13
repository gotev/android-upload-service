package com.alexbbb.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

/**
 * Binary file upload request.
 *
 * @author cankov
 */
public class BinaryUploadRequest extends HttpUploadRequest {

    /**
     * Static constant used to identify the task type. It must be unique between task types.
     */
    public static final String NAME = "binary";

    private BinaryUploadFile file = null;

    /**
     * Creates a binary file upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request.<br>
     *                 It can be whatever string you want. If you set it to null or an
     *                 empty string, an UUID will be automatically generated.<br>
     *                 It's advised to keep a reference to it in your code, so when you receive
     *                 status updates in {@link UploadServiceBroadcastReceiver}, you know to
     *                 which upload they refer to.
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

    /**
     * Validates the upload request and throws exceptions if one or more parameters are not
     * properly set.
     *
     * @throws IllegalArgumentException if request protocol or URL are not correctly set o
     * if no file is set
     * @throws MalformedURLException if the provided server URL is not valid
     */
    @Override
    protected void validate() throws IllegalArgumentException, MalformedURLException {
        super.validate();
        if (file == null) {
            throw new IllegalArgumentException("You have to set a file to upload");
        }
    }

    /**
     * Write any upload request data to the intent used to start the upload service.
     *
     * @param intent the intent used to start the upload service
     */
    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(UploadService.PARAM_TYPE, NAME);
        intent.putExtra(UploadService.PARAM_FILE, getFile());
    }

    /**
     * Sets the file used as raw body of the upload request.
     *
     * @param path Absolute path to the file that you want to upload
     * @throws FileNotFoundException if the file to upload does not exist
     * @return {@link BinaryUploadRequest}
     */
    public BinaryUploadRequest setFileToUpload(String path) throws FileNotFoundException {
        file = new BinaryUploadFile(path);
        return this;
    }

    @Override
    public BinaryUploadRequest setNotificationConfig(UploadNotificationConfig config) {
        super.setNotificationConfig(config);
        return this;
    }

    @Override
    public BinaryUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFilesAfterSuccessfulUpload) {
        super.setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFilesAfterSuccessfulUpload);
        return this;
    }

    @Override
    public BinaryUploadRequest addHeader(String headerName, String headerValue) {
        super.addHeader(headerName, headerValue);
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

    /**
     * Gets the file used as raw body of the upload request.
     *
     * @return The absolute path of the file that will be used for the upload
     */
    protected BinaryUploadFile getFile() {
        return file;
    }
}
