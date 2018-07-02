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
public class HttpJsonRequest extends HttpUploadRequest<HttpJsonRequest> {

    private static final String LOG_TAG = HttpJsonRequest.class.getSimpleName();
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
    public HttpJsonRequest(final Context context, final String uploadId, final String serverUrl)
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
    public HttpJsonRequest(final Context context, final String serverUrl)
        throws MalformedURLException, IllegalArgumentException {
        this(context, null, serverUrl);
    }

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(HttpJsonTask.PARAM_UTF8_CHARSET, isUtf8Charset);
    }

    @Override
    protected Class<? extends UploadTask> getTaskClass() {
        return HttpJsonTask.class;
    }

    /**
     * Sets the charset for this multipart request to UTF-8. If not set, the standard US-ASCII
     * charset will be used.
     * @return request instance
     */
    public HttpJsonRequest setUtf8Charset() {
        isUtf8Charset = true;
        return this;
    }
}
