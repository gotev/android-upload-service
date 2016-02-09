package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * HTTP/Multipart upload request. This is the most common way to upload files on a server.
 * It's the same kind of request that browsers do when you use the &lt;form&gt; tag
 * with one or more files.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 *
 */
public class MultipartUploadRequest extends HttpUploadRequest {

    private boolean isUtf8Charset = false;

    /**
     * Creates a new multipart upload request.
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
    public MultipartUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        super(context, uploadId, serverUrl);
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

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(MultipartUploadTask.PARAM_UTF8_CHARSET, isUtf8Charset);
    }

    @Override
    protected Class getTaskClass() {
        return MultipartUploadTask.class;
    }

    @Override
    protected void validate() throws IllegalArgumentException, MalformedURLException {
        super.validate();

        if (params.getFiles().isEmpty())
            throw new IllegalArgumentException("You have to add at least one file to upload");
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
        params.addFile(new UploadFile(path, parameterName, fileName, contentType));
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
        return addFileToUpload(path, parameterName, fileName, null);
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

    // override all the supported builder methods by calling the super method and returning this
    @Override
    public MultipartUploadRequest setNotificationConfig(UploadNotificationConfig config) {
        super.setNotificationConfig(config);
        return this;
    }

    @Override
    public MultipartUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        super.setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFiles);
        return this;
    }

    @Override
    public MultipartUploadRequest addHeader(String headerName, String headerValue) {
        super.addHeader(headerName, headerValue);
        return this;
    }

    @Override
    public MultipartUploadRequest setBasicAuth(final String username, final String password) {
        super.setBasicAuth(username, password);
        return this;
    }

    @Override
    public MultipartUploadRequest addParameter(String paramName, String paramValue) {
        super.addParameter(paramName, paramValue);
        return this;
    }

    @Override
    public MultipartUploadRequest addArrayParameter(String paramName, String... array) {
        super.addArrayParameter(paramName, array);
        return this;
    }

    @Override
    public MultipartUploadRequest addArrayParameter(String paramName, List<String> list) {
        super.addArrayParameter(paramName, list);
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

    @Override
    public MultipartUploadRequest setUsesFixedLengthStreamingMode(boolean fixedLength) {
        super.setUsesFixedLengthStreamingMode(fixedLength);
        return this;
    }

    /**
     * Sets the charset for this multipart request to UTF-8. If not set, the standard US-ASCII
     * charset will be used.
     * @return request instance
     */
    public MultipartUploadRequest setUtf8Charset() {
        isUtf8Charset = true;
        return this;
    }
}
