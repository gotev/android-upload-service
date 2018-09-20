package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

/**
 * HTTP/Multipart upload request. This is the most common way to upload files on a server.
 * It's the same kind of request that browsers do when you use the &lt;form&gt; tag
 * with one or more files.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 *
 */
public class MultipartUploadRequest extends HttpUploadRequest<MultipartUploadRequest> {

    private static final String LOG_TAG = MultipartUploadRequest.class.getSimpleName();
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
     * @throws IllegalArgumentException if one or more arguments are not valid
     * @throws MalformedURLException if the server URL is not valid
     */
    public MultipartUploadRequest(final Context context, final String uploadId, final String serverUrl)
        throws IllegalArgumentException, MalformedURLException {
        super(context, uploadId, serverUrl);
    }

    /**
     * Creates a new multipart upload request and automatically generates an upload id, that will
     * be returned when you call {@link UploadRequest#startUpload()}.
     *
     * @param context application context
     * @param serverUrl URL of the server side script that will handle the multipart form upload.
     *                  E.g.: http://www.yourcompany.com/your/script
     * @throws IllegalArgumentException if one or more arguments are not valid
     * @throws MalformedURLException if the server URL is not valid
     */
    public MultipartUploadRequest(final Context context, final String serverUrl)
        throws MalformedURLException, IllegalArgumentException {
        this(context, null, serverUrl);
    }

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(MultipartUploadTask.PARAM_UTF8_CHARSET, isUtf8Charset);
    }

    @Override
    protected Class<? extends UploadTask> getTaskClass() {
        return MultipartUploadTask.class;
    }

    /**
     * Adds a file to this upload request.
     *
     * @param filePath path to the file that you want to upload
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
    public MultipartUploadRequest addFileToUpload(String filePath,
                                                  String parameterName,
                                                  String fileName, String contentType)
            throws FileNotFoundException, IllegalArgumentException {

        UploadFile file = new UploadFile(filePath);
        filePath = file.getPath();

        if (parameterName == null || "".equals(parameterName)) {
            throw new IllegalArgumentException("Please specify parameterName value for file: "
                                               + filePath);
        }

        file.setProperty(MultipartUploadTask.PROPERTY_PARAM_NAME, parameterName);

        if (contentType == null || contentType.isEmpty()) {
            contentType = file.getContentType(context);
            Logger.debug(LOG_TAG, "Auto-detected MIME type for " + filePath
                    + " is: " + contentType);
        } else {
            Logger.debug(LOG_TAG, "Content Type set for " + filePath
                    + " is: " + contentType);
        }

        file.setProperty(MultipartUploadTask.PROPERTY_CONTENT_TYPE, contentType);

        if (fileName == null || "".equals(fileName)) {
            fileName = file.getName(context);
            Logger.debug(LOG_TAG, "Using original file name: " + fileName);
        } else {
            Logger.debug(LOG_TAG, "Using custom file name: " + fileName);
        }

        file.setProperty(MultipartUploadTask.PROPERTY_REMOTE_FILE_NAME, fileName);

        params.files.add(file);
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
