package com.alexbbb.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Represents a generic HTTP upload request.
 *
 * @author alexbbb (Alex Gotev)
 * @author eliasnaur
 * @author cankov
 */
abstract class HttpUploadRequest {

    private UploadNotificationConfig notificationConfig;
    private String method = "POST";
    private final Context context;
    private String customUserAgent;
    private int maxRetries;
    private final String uploadId;
    private final String url;
    private final ArrayList<NameValue> headers;
    private boolean autoDeleteFilesAfterSuccessfulUpload = false;

    /**
     * Creates a new multipart upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. If is null or empty, a random
     *                 UUID will be automatically generated. It's used in the broadcast receiver
     *                 when receiving updates.
     * @param serverUrl URL of the server side script that handles the multipart form upload
     */
    public HttpUploadRequest(final Context context, final String uploadId, final String serverUrl) {
        this.context = context;

        if (uploadId == null || uploadId.isEmpty()) {
            this.uploadId = UUID.randomUUID().toString();
        } else {
            this.uploadId = uploadId;
        }

        notificationConfig = null;
        url = serverUrl;
        headers = new ArrayList<>();
        maxRetries = 0;
    }

    /**
     * Start the background file upload service.
     * @return the uploadId string
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public String startUpload() throws IllegalArgumentException, MalformedURLException {
        this.validate();
        final Intent intent = new Intent(this.getContext(), UploadService.class);
        this.initializeIntent(intent);
        intent.setAction(UploadService.getActionUpload());
        getContext().startService(intent);
        return uploadId;
    }

    /**
     * Write any upload request data to the intent used to start the upload service.
     *
     * @param intent the intent used to start the upload service
     */
    protected void initializeIntent(Intent intent) {
        intent.setAction(UploadService.getActionUpload());
        intent.putExtra(UploadService.PARAM_NOTIFICATION_CONFIG, getNotificationConfig());
        intent.putExtra(UploadService.PARAM_ID, getUploadId());
        intent.putExtra(UploadService.PARAM_URL, getServerUrl());
        intent.putExtra(UploadService.PARAM_METHOD, getMethod());
        intent.putExtra(UploadService.PARAM_CUSTOM_USER_AGENT, getCustomUserAgent());
        intent.putExtra(UploadService.PARAM_MAX_RETRIES, getMaxRetries());
        intent.putExtra(UploadService.PARAM_AUTO_DELETE_FILES, autoDeleteFilesAfterSuccessfulUpload);
        intent.putParcelableArrayListExtra(UploadService.PARAM_REQUEST_HEADERS, getHeaders());
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
        notificationConfig = config;
        return this;
    }

    /**
     * Sets the automatic file deletion after successful upload.
     * @param autoDeleteFilesAfterSuccessfulUpload true to auto delete files included in the
     *                                             request when the upload is completed successfully.
     *                                             By default this setting is set to false, and
     *                                             nothing gets deleted.
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFilesAfterSuccessfulUpload) {
        this.autoDeleteFilesAfterSuccessfulUpload = autoDeleteFilesAfterSuccessfulUpload;
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
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        // Check if the URL is valid
        new URL(url);
    }

    /**
     * Adds a header to this upload request.
     *
     * @param headerName header name
     * @param headerValue header value
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest addHeader(final String headerName, final String headerValue) {
        headers.add(new NameValue(headerName, headerValue));
        return this;
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     *
     * @param method new HTTP method to use
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setMethod(final String method) {
        if (method != null && method.length() > 0)
            this.method = method;

        return this;
    }

    /**
     * Gets the HTTP method to use.
     *
     * @return HTTP method
     */
    protected String getMethod() {
        return method;
    }

    /**
     * @return Gets the upload ID of this request.
     */
    protected String getUploadId() {
        return uploadId;
    }

    /**
     * @return Gets the URL of the server side script that will handle the multipart form upload.
     */
    protected String getServerUrl() {
        return url;
    }

    /**
     * @return Gets the list of the headers.
     */
    protected ArrayList<NameValue> getHeaders() {
        return headers;
    }

    /**
     * @return Gets the upload notification configuration.
     */
    protected UploadNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    /**
     * @return Gets the application context.
     */
    protected Context getContext() {
        return context;
    }

    /**
     * Gets the custom user agent defined for this upload request.
     *
     * @return string representing the user agent or null if it's not defined
     */
    protected final String getCustomUserAgent() {
        return customUserAgent;
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
        this.customUserAgent = customUserAgent;
        return this;
    }

    /**
     * @return Get the maximum number of retries that the library will do if an error occurs,
     * before returning an error.
     */
    protected final int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets the maximum number of retries that the library will do if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return {@link HttpUploadRequest}
     */
    public HttpUploadRequest setMaxRetries(int maxRetries) {
        if (maxRetries < 0)
            this.maxRetries = 0;
        else
            this.maxRetries = maxRetries;

        return this;
    }
}
