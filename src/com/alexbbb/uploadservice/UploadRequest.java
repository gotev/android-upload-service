package com.alexbbb.uploadservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;

/**
 * Represents an upload request.
 *
 * @author alexbbb (Alex Gotev)
 *
 */
public class UploadRequest {

    private UploadNotificationConfig notificationConfig;
    private final Context context;
    private final String url;
    private final ArrayList<FileToUpload> filesToUpload;
    private final ArrayList<NameValue> headers;
    private final ArrayList<NameValue> parameters;

    public UploadRequest(final Context context, final String serverUrl) {
        this.context = context;
        notificationConfig = new UploadNotificationConfig();
        url = serverUrl;
        filesToUpload = new ArrayList<FileToUpload>();
        headers = new ArrayList<NameValue>();
        parameters = new ArrayList<NameValue>();
    }

    public void setNotificationConfig(final int iconResourceID,
                                      final String title,
                                      final String message,
                                      final String completed,
                                      final String error,
                                      final boolean autoClearOnSuccess) {
        notificationConfig = new UploadNotificationConfig(iconResourceID,
                                                          title, message,
                                                          completed, error,
                                                          autoClearOnSuccess);
    }

    public void validate() throws IllegalArgumentException, MalformedURLException {
        if (url == null || "".equals(url)) {
            throw new IllegalArgumentException("Request URL cannot be either null or empty");
        }

        if (!url.startsWith("http")) {
            throw new IllegalArgumentException("Specify either http:// or https:// as protocol");
        }

        //Check if the URL is valid
        new URL(url);

        if (filesToUpload.isEmpty()) {
            throw new IllegalArgumentException("You have to add at least one file to upload");
        }
    }

    public void addFileToUpload(final String path,
                                final String parameterName,
                                final String contentType) {
        filesToUpload.add(new FileToUpload(path, parameterName, contentType));
    }

    public void addHeader(final String headerName, final String headerValue) {
        headers.add(new NameValue(headerName, headerValue));
    }

    public void addParameter(final String paramName, final String paramValue) {
        parameters.add(new NameValue(paramName, paramValue));
    }

    protected String getServerUrl() {
        return url;
    }

    protected ArrayList<FileToUpload> getFilesToUpload() {
        return filesToUpload;
    }

    protected ArrayList<NameValue> getHeaders() {
        return headers;
    }

    protected ArrayList<NameValue> getParameters() {
        return parameters;
    }

    protected UploadNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    protected Context getContext() {
        return context;
    }
}
