package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Represents a generic HTTP upload request.<br>
 * Subclass to create your own custom HTTP upload request.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public abstract class HttpUploadRequest<B extends HttpUploadRequest<B>>
        extends UploadRequest<B> {

    protected final HttpUploadTaskParameters httpParams = new HttpUploadTaskParameters();

    /**
     * Creates a new http upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. If is null or empty, a random
     *                 UUID will be automatically generated. It's used in the broadcast receiver
     *                 when receiving updates.
     * @param serverUrl URL of the server side script that handles the request
     * @throws IllegalArgumentException if one or more arguments are not valid
     * @throws MalformedURLException if the server URL is not valid
     */
    public HttpUploadRequest(final Context context, final String uploadId, final String serverUrl)
        throws MalformedURLException, IllegalArgumentException{
        super(context, uploadId, serverUrl);

        if (!params.serverUrl.startsWith("http://")
                && !params.serverUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(params.serverUrl);
    }

    @Override
    protected void initializeIntent(Intent intent) {
        super.initializeIntent(intent);
        intent.putExtra(HttpUploadTaskParameters.PARAM_HTTP_TASK_PARAMETERS, httpParams);
    }

    /**
     * Adds a header to this upload request.
     *
     * @param headerName header name
     * @param headerValue header value
     * @return self instance
     */
    public B addHeader(final String headerName, final String headerValue) {
        httpParams.addHeader(headerName, headerValue);
        return self();
    }

    /**
     * Sets the HTTP Basic Authentication header.
     * @param username HTTP Basic Auth username
     * @param password HTTP Basic Auth password
     * @return self instance
     */
    public B setBasicAuth(final String username, final String password) {
        String auth = Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);
        httpParams.addHeader("Authorization", "Basic " + auth);
        return self();
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     * @return self instance
     */
    public B addParameter(final String paramName, final String paramValue) {
        httpParams.addParameter(paramName, paramValue);
        return self();
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param array values
     * @return self instance
     */
    public B addArrayParameter(final String paramName, final String... array) {
        for (String value : array) {
            httpParams.addParameter(paramName, value);
        }
        return self();
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param list values
     * @return self instance
     */
    public B addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            httpParams.addParameter(paramName, value);
        }
        return self();
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     *
     * @param method new HTTP method to use
     * @return self instance
     */
    public B setMethod(final String method) {
        httpParams.method = method;
        return self();
    }

    /**
     * Sets the custom user agent to use for this upload request.
     * Note! If you set the "User-Agent" header by using the "addHeader" method,
     * that setting will be overwritten by the value set with this method.
     *
     * @param customUserAgent custom user agent string
     * @return self instance
     */
    public B setCustomUserAgent(String customUserAgent) {
        if (customUserAgent != null && !customUserAgent.isEmpty()) {
            httpParams.customUserAgent = customUserAgent;
        }
        return self();
    }

    /**
     * Sets if this upload request is using fixed length streaming mode.
     * If it uses fixed length streaming mode, then the value returned by
     * {@link HttpUploadTask#getBodyLength()} will be automatically used to properly set the
     * underlying {@link java.net.HttpURLConnection}, otherwise chunked streaming mode will be used.
     * @param fixedLength true to use fixed length streaming mode (this is the default setting) or
     *                    false to use chunked streaming mode.
     * @return self instance
     */
    public B setUsesFixedLengthStreamingMode(boolean fixedLength) {
        httpParams.usesFixedLengthStreamingMode = fixedLength;
        return self();
    }
}
