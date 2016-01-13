package com.alexbbb.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP/Multipart upload request.
 *
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 *
 */
public class MultipartUploadRequest extends HttpUploadRequest {

    /**
     * Static constant used to identify the task type. It must be unique between task types.
     */
    public static final String NAME = "multipart";

    private final ArrayList<MultipartUploadFile> filesToUpload;
    private final ArrayList<NameValue> parameters;

    /**
     * Creates a new multipart upload request.
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
    public MultipartUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        super(context, uploadId, serverUrl);
        filesToUpload = new ArrayList<>();
        parameters = new ArrayList<>();
    }

    /**
     * Creates a new multipart upload request and automatically generates an upload id, that will
     * be returned when you call {@link HttpUploadRequest#startUpload()}.
     *
     * @param context application context
     * @param serverUrl URL of the server side script that will handle the multipart form upload.
     *                  E.g.: http://www.yourcompany.com/your/script
     */
    public MultipartUploadRequest(final Context context, final String serverUrl) {
        this(context, null, serverUrl);
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters
     * are not properly set.
     *
     * @throws IllegalArgumentException if request protocol or URL are not correctly set or if no files are added
     * @throws MalformedURLException if the provided server URL is not valid
     */
    @Override
    protected void validate() throws IllegalArgumentException, MalformedURLException {
        super.validate();

        if (filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You have to add at least one file to upload");
        }
    }

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(UploadService.PARAM_TYPE, NAME);
        intent.putParcelableArrayListExtra(UploadService.PARAM_FILES, getFilesToUpload());
        intent.putParcelableArrayListExtra(UploadService.PARAM_REQUEST_PARAMETERS, getParameters());
    }

    /**
     * Adds a file to this upload request.
     *
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script. If null, the original file name
     *                 will be used
     * @param contentType Content type of the file. You can use constants defined in
     *                    {@link ContentType} class. Set this to null or empty string to try to
     *                    automatically detect the mime type from the file. If the mime type can't
     *                    be detected, {@code application/octet-stream} will be used by default
     * @throws FileNotFoundException if the file does not exist at the specified path
     * @throws IllegalArgumentException if one or more parameters are not valid
     * @return {@link MultipartUploadRequest}
     */
    public MultipartUploadRequest addFileToUpload(final String path, final String parameterName,
                                                  final String fileName, final String contentType)
            throws FileNotFoundException, IllegalArgumentException {
        filesToUpload.add(new MultipartUploadFile(path, parameterName, fileName, contentType));
        return this;
    }

    /**
     * Adds a file to this upload request, without setting the content type, which will be
     * automatically detected from the file extension. If you want to
     * manually set the content type, use {@link #addFileToUpload(String, String, String, String)}.
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script. If null, the original file name
     *                 will be used
     * @return {@link MultipartUploadRequest}
     * @throws FileNotFoundException if the file does not exist at the specified path
     * @throws IllegalArgumentException if one or more parameters are not valid
     */
    public MultipartUploadRequest addFileToUpload(final String path, final String parameterName,
                                                  final String fileName)
            throws FileNotFoundException, IllegalArgumentException {
        filesToUpload.add(new MultipartUploadFile(path, parameterName, fileName, null));
        return this;
    }

    /**
     * Adds a file to this upload request, without setting file name and content type.
     * The original file name will be used instead. If you want to manually set the file name seen
     * by the server side script and the content type, use
     * {@link #addFileToUpload(String, String, String, String)}
     *
     * @param path Absolute path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @throws FileNotFoundException if the file does not exist at the specified path
     * @throws IllegalArgumentException if one or more parameters are not valid
     * @return {@link MultipartUploadRequest}
     */
    public MultipartUploadRequest addFileToUpload(final String path, final String parameterName)
            throws FileNotFoundException, IllegalArgumentException {
        return addFileToUpload(path, parameterName, null, null);
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     * @return {@link MultipartUploadRequest}
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
     * @return {@link MultipartUploadRequest}
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
     * @return {@link MultipartUploadRequest}
     */
    public MultipartUploadRequest addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            parameters.add(new NameValue(paramName, value));
        }
        return this;
    }

    @Override
    public MultipartUploadRequest setNotificationConfig(UploadNotificationConfig config) {
        super.setNotificationConfig(config);
        return this;
    }

    @Override
    public MultipartUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFilesAfterSuccessfulUpload) {
        super.setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFilesAfterSuccessfulUpload);
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
    public MultipartUploadRequest setMaxRetries(int maxRetries) {
        super.setMaxRetries(maxRetries);
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
