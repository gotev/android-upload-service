package com.alexbbb.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements an HTTP Multipart upload request.
 *
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 *
 */
public class MultipartUploadRequest extends HttpUploadRequest {

    private final ArrayList<MultipartUploadFile> filesToUpload;
    private final ArrayList<NameValue> parameters;

    /**
     * Creates a new multipart upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request.
     *                 It's used in the broadcast receiver when receiving updates.
     * @param serverUrl URL of the server side script that handles the multipart form upload
     */
    public MultipartUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        super(context, uploadId, serverUrl);
        filesToUpload = new ArrayList<>();
        parameters = new ArrayList<>();
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters are not properly set.
     *
     * @throws IllegalArgumentException if request protocol or URL are not correctly set or if no files are added
     * @throws MalformedURLException if the provided server URL is not valid
     */
    @Override
    public void validate() throws IllegalArgumentException, MalformedURLException {
        super.validate();

        if (filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You have to add at least one file to upload");
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
        intent.putExtra(UploadService.PARAM_TYPE, UploadService.UPLOAD_MULTIPART);
        intent.putParcelableArrayListExtra(UploadService.PARAM_FILES, getFilesToUpload());
        intent.putParcelableArrayListExtra(UploadService.PARAM_REQUEST_PARAMETERS, getParameters());
    }

    /**
     * Adds a file to this upload request.
     *
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script
     * @param contentType Content type of the file. Set this to null if you don't want to set a
     *                    content type.
     * @throws FileNotFoundException if the file does not exist at the specified path
     */
    public MultipartUploadRequest addFileToUpload(final String path, final String parameterName,
                                                  final String fileName, final String contentType)
            throws FileNotFoundException {
        filesToUpload.add(new MultipartUploadFile(path, parameterName, fileName, contentType));
        return this;
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     */
    public MultipartUploadRequest addParameter(final String paramName, final String paramValue) {
        parameters.add(new NameValue(paramName, paramValue));
        return this;
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param array values
     */
    public MultipartUploadRequest addArrayParameter(final String paramName, final String... array) {
        for (String value : array) {
            parameters.add(new NameValue(paramName, value));
        }
        return this;
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param list values
     */
    public MultipartUploadRequest addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            parameters.add(new NameValue(paramName, value));
        }
        return this;
    }

    @Override
    public MultipartUploadRequest setNotificationConfig(int iconResourceID, String title,
                                                        String message, String completed,
                                                        String error, boolean autoClearOnSuccess,
                                                        boolean autoClearOnAction) {
        super.setNotificationConfig(iconResourceID, title, message, completed, error,
                                    autoClearOnSuccess, autoClearOnAction);
        return this;
    }

    @Override
    public MultipartUploadRequest addHeader(String headerName, String headerValue) {
        super.addHeader(headerName, headerValue);
        return this;
    }

    @Override
    public MultipartUploadRequest setMethod(String method) {
        super.setMethod(method);
        return this;
    }

    @Override
    public MultipartUploadRequest setCustomUserAgent(String customUserAgent) {
        super.setCustomUserAgent(customUserAgent);
        return this;
    }

    @Override
    public MultipartUploadRequest setNotificationClickIntent(Intent intent) {
        super.setNotificationClickIntent(intent);
        return this;
    }

    /**
     * @return Gets the list of the parameters.
     */
    protected ArrayList<NameValue> getParameters() {
        return parameters;
    }

    /**
     * @return Gets the list of the files that has to be uploaded.
     */
    protected ArrayList<MultipartUploadFile> getFilesToUpload() {
        return filesToUpload;
    }
}
