package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * Represents a generic HTTP upload request.<br>
 * Subclass to create your own custom upload request.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public abstract class HttpUploadRequest {

    private static final String LOG_TAG = HttpUploadRequest.class.getSimpleName();

    private final Context context;
    protected final TaskParameters params = new TaskParameters();

    /**
     * Creates a new http upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. If is null or empty, a random
     *                 UUID will be automatically generated. It's used in the broadcast receiver
     *                 when receiving updates.
     * @param serverUrl URL of the server side script that handles the request
     */
    public HttpUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        if (context == null)
            throw new IllegalArgumentException("Context MUST not be null!");

        this.context = context;

        if (uploadId == null || uploadId.isEmpty()) {
            Logger.debug(LOG_TAG, "null or empty upload ID. Generating it");
            params.setId(UUID.randomUUID().toString());
        } else {
            Logger.debug(LOG_TAG, "setting provided upload ID");
            params.setId(uploadId);
        }

        params.setUrl(serverUrl);
        Logger.debug(LOG_TAG, "Created new upload request to "
                     + params.getUrl() + " with ID: " + params.getId());
    }

    /**
     * Start the background file upload service.
     * @return the uploadId string
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public final String startUpload() throws IllegalArgumentException, MalformedURLException {
        this.validate();
        final Intent intent = new Intent(this.getContext(), UploadService.class);
        this.initializeIntent(intent);
        intent.setAction(UploadService.getActionUpload());
        getContext().startService(intent);
        return params.getId();
    }

    /**
     * Write any upload request data to the intent used to start the upload service.<br>
     * Override this method in subclasses to add your own custom parameters to the upload task.
     *
     * @param intent the intent used to start the upload service
     */
    protected void initializeIntent(Intent intent) {
        intent.putExtra(UploadService.PARAM_TASK_PARAMETERS, params);

        Class taskClass = getTaskClass();
        if (taskClass == null)
            throw new RuntimeException("The request must specify a task class!");

        intent.putExtra(UploadService.PARAM_TASK_CLASS, taskClass.getName());
    }

    /**
     * Sets custom notification configuration.
     * If you don't want to display a notification in Notification Center, either pass null
     * as argument or don't call this method.
     *
     * @param config the upload configuration object or null if you don't want a notification
     *               to be displayed
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setNotificationConfig(UploadNotificationConfig config) {
        params.setNotificationConfig(config);
        return this;
    }

    /**
     * Sets the automatic file deletion after successful upload.
     * @param autoDeleteFiles true to auto delete files included in the
     *                        request when the upload is completed successfully.
     *                        By default this setting is set to false, and nothing gets deleted.
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        params.setAutoDeleteSuccessfullyUploadedFiles(autoDeleteFiles);
        return this;
    }

    /**
     * Validates the upload request and throws exceptions if one or more parameters are
     * not properly set.
     *
     * @throws IllegalArgumentException if request protocol or URL are not correctly set
     * @throws MalformedURLException if the provided server URL is not valid
     */
    protected void validate() throws IllegalArgumentException, MalformedURLException {
        if (params.getUrl() == null || "".equals(params.getUrl())) {
            throw new IllegalArgumentException("Request URL cannot be null or empty");
        }

        if (!params.getUrl().startsWith("http://") && !params.getUrl().startsWith("https://")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(params.getUrl());
    }

    /**
     * Adds a header to this upload request.
     *
     * @param headerName header name
     * @param headerValue header value
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest addHeader(final String headerName, final String headerValue) {
        params.addRequestHeader(headerName, headerValue);
        return this;
    }

    /**
     * Sets the HTTP Basic Authentication header.
     * @param username HTTP Basic Auth username
     * @param password HTTP Basic Auth password
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setBasicAuth(final String username, final String password) {
        String auth = Base64.encodeToString((username + ":" + password).getBytes(), Base64.DEFAULT);
        params.addRequestHeader("Authorization", "Basic " + auth);
        return this;
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest addParameter(final String paramName, final String paramValue) {
        params.addRequestParameter(paramName, paramValue);
        return this;
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param array values
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest addArrayParameter(final String paramName, final String... array) {
        for (String value : array) {
            params.addRequestParameter(paramName, value);
        }
        return this;
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param list values
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest addArrayParameter(final String paramName, final List<String> list) {
        for (String value : list) {
            params.addRequestParameter(paramName, value);
        }
        return this;
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     *
     * @param method new HTTP method to use
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setMethod(final String method) {
        params.setMethod(method);
        return this;
    }

    /**
     * Sets the custom user agent to use for this upload request.
     * Note! If you set the "User-Agent" header by using the "addHeader" method,
     * that setting will be overwritten by the value set with this method.
     *
     * @param customUserAgent custom user agent string
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setCustomUserAgent(String customUserAgent) {
        params.setCustomUserAgent(customUserAgent);
        return this;
    }

    /**
     * @return Gets the application context.
     */
    protected final Context getContext() {
        return context;
    }

    /**
     * Sets the maximum number of retries that the library will do if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setMaxRetries(int maxRetries) {
        params.setMaxRetries(maxRetries);
        return this;
    }

    /**
     * Sets if this upload request is using fixed length streaming mode.
     * If it uses fixed length streaming mode, then the value returned by
     * {@link HttpUploadTask#getBodyLength()} will be automatically used to properly set the
     * underlying {@link java.net.HttpURLConnection}, otherwise chunked streaming mode will be used.
     * @param fixedLength true to use fixed length streaming mode (this is the default setting) or
     *                    false to use chunked streaming mode.
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setUsesFixedLengthStreamingMode(boolean fixedLength) {
        params.setUsesFixedLengthStreamingMode(fixedLength);
        return this;
    }

    /**
     * Implement in subclasses to specify the class which will handle the the upload task.
     * The class must be a subclass of {@link HttpUploadTask}.
     * @return class
     */
    protected abstract Class getTaskClass();
}
