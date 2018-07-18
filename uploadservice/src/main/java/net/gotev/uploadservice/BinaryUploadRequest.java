package net.gotev.uploadservice;

import android.content.Context;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Binary file upload request. The binary upload uses a single file as the raw body of the
 * upload request.
 *
 * @author cankov
 * @author gotev (Aleksandar Gotev)
 */
public class BinaryUploadRequest extends HttpUploadRequest<BinaryUploadRequest> {

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
     * @throws IllegalArgumentException if one or more arguments are not valid
     * @throws MalformedURLException if the server URL is not valid
     */
    public BinaryUploadRequest(final Context context, final String uploadId, final String serverUrl)
        throws IllegalArgumentException, MalformedURLException {
        super(context, uploadId, serverUrl);
    }

    /**
     * Creates a new binaryupload request and automatically generates an upload id, that will
     * be returned when you call {@link HttpUploadRequest#startUpload()}.
     *
     * @param context application context
     * @param serverUrl URL of the server side script that will handle the multipart form upload.
     *                  E.g.: http://www.yourcompany.com/your/script
     * @throws IllegalArgumentException if one or more arguments are not valid
     * @throws MalformedURLException if the server URL is not valid
     */
    public BinaryUploadRequest(final Context context, final String serverUrl)
        throws MalformedURLException, IllegalArgumentException {
        this(context, null, serverUrl);
    }

    @Override
    protected Class<? extends UploadTask> getTaskClass() {
        return BinaryUploadTask.class;
    }

    /**
     * Sets the file used as raw body of the upload request.
     *
     * @param path path to the file that you want to upload
     * @throws FileNotFoundException if the file to upload does not exist
     * @return {@link BinaryUploadRequest}
     */
    public BinaryUploadRequest setFileToUpload(String path) throws FileNotFoundException {
        params.files.clear();
        params.files.add(new UploadFile(path));
        return this;
    }

    @Override
    public BinaryUploadRequest addParameter(String paramName, String paramValue) {
        logDoesNotSupportParameters();
        return this;
    }

    @Override
    public BinaryUploadRequest addArrayParameter(String paramName, String... array) {
        logDoesNotSupportParameters();
        return this;
    }

    @Override
    public BinaryUploadRequest addArrayParameter(String paramName, List<String> list) {
        logDoesNotSupportParameters();
        return this;
    }

    @Override
    public String startUpload() {
        if (params.files.isEmpty())
            throw new IllegalArgumentException("Set the file to be used in the request body first!");

        return super.startUpload();
    }

    private void logDoesNotSupportParameters() {
        Logger.error(getClass().getSimpleName(),
                     "This upload method does not support adding parameters");
    }
}
