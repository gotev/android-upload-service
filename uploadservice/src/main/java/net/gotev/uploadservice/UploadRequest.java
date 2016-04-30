package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;

import java.net.MalformedURLException;
import java.util.UUID;

/**
 * Base class to extend to create an upload request. If you are implementing an HTTP based upload,
 * extend {@link HttpUploadRequest} instead.
 *
 * @author Aleksandar Gotev
 */
public abstract class UploadRequest {
    private static final String LOG_TAG = UploadRequest.class.getSimpleName();

    protected final Context context;
    protected final UploadTaskParameters params = new UploadTaskParameters();

    /**
     * Creates a new upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. If is null or empty, a random
     *                 UUID will be automatically generated. It's used in the broadcast receiver
     *                 when receiving updates.
     * @param serverUrl URL of the server side script that handles the request
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl) {
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

        params.setServerUrl(serverUrl);
        Logger.debug(LOG_TAG, "Created new upload request to "
                     + params.getServerUrl() + " with ID: " + params.getId());
    }

    /**
     * Start the background file upload service.
     * @return the uploadId string
     * @throws IllegalArgumentException if one or more arguments passed are invalid
     * @throws MalformedURLException if the server URL is not valid
     */
    public final String startUpload() throws IllegalArgumentException, MalformedURLException {
        this.validate();
        final Intent intent = new Intent(context, UploadService.class);
        this.initializeIntent(intent);
        intent.setAction(UploadService.getActionUpload());
        context.startService(intent);
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
     * Validates the upload request and throws exceptions if one or more parameters are
     * not properly set.
     *
     * @throws IllegalArgumentException if request protocol or URL are not correctly set
     * @throws MalformedURLException if the provided server URL is not valid
     */
    protected void validate() throws IllegalArgumentException, MalformedURLException {
        if (params.getServerUrl() == null || "".equals(params.getServerUrl())) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }

        if (params.getFiles().isEmpty())
            throw new IllegalArgumentException("You have to add at least one file to upload");
    }

    /**
     * Sets custom notification configuration.
     * If you don't want to display a notification in Notification Center, either pass null
     * as argument or don't call this method.
     *
     * @param config the upload configuration object or null if you don't want a notification
     *               to be displayed
     * @return {@link UploadRequest}
     */
    public UploadRequest setNotificationConfig(UploadNotificationConfig config) {
        params.setNotificationConfig(config);
        return this;
    }

    /**
     * Sets the automatic file deletion after successful upload.
     * @param autoDeleteFiles true to auto delete files included in the
     *                        request when the upload is completed successfully.
     *                        By default this setting is set to false, and nothing gets deleted.
     * @return {@link UploadRequest}
     */
    public UploadRequest setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        params.setAutoDeleteSuccessfullyUploadedFiles(autoDeleteFiles);
        return this;
    }

    /**
     * Sets the maximum number of retries that the library will try if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return {@link UploadRequest}
     */
    public UploadRequest setMaxRetries(int maxRetries) {
        params.setMaxRetries(maxRetries);
        return this;
    }

    /**
     * Implement in subclasses to specify the class which will handle the the upload task.
     * The class must be a subclass of {@link UploadTask}.
     * @return class
     */
    protected abstract Class<? extends UploadTask> getTaskClass();
}
