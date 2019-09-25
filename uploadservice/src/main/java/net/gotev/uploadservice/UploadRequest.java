package net.gotev.uploadservice;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import net.gotev.uploadservice.data.UploadFile;
import net.gotev.uploadservice.data.UploadTaskParameters;
import net.gotev.uploadservice.logger.UploadServiceLogger;
import net.gotev.uploadservice.observer.request.SingleRequestObserver;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Base class to extend to create an upload request. If you are implementing an HTTP based upload,
 * extend {@link HttpUploadRequest} instead.
 */
public abstract class UploadRequest<B extends UploadRequest<B>> {
    private static final String LOG_TAG = UploadRequest.class.getSimpleName();

    protected final Context context;
    private String uploadId;
    protected String serverUrl;
    protected int maxRetries = UploadServiceConfig.INSTANCE.getRetryPolicy().getDefaultMaxRetries();
    protected boolean autoDeleteSuccessfullyUploadedFiles = false;
    protected UploadNotificationConfig notificationConfig;
    protected ArrayList<UploadFile> files = new ArrayList<>();

    /**
     * Creates a new upload request.
     *
     * @param context application context
     * @param uploadId unique ID to assign to this upload request. If is null or empty, a random
     *                 UUID will be automatically generated. It's used in the broadcast receiver
     *                 when receiving updates.
     * @param serverUrl URL of the server side script that handles the request
     * @throws IllegalArgumentException if one or more arguments are not valid
     */
    public UploadRequest(final Context context, final String uploadId, final String serverUrl)
        throws IllegalArgumentException {

        if (context == null)
            throw new IllegalArgumentException("Context MUST not be null!");

        if (serverUrl == null || "".equals(serverUrl)) {
            throw new IllegalArgumentException("Server URL cannot be null or empty");
        }

        this.context = context;

        if (uploadId == null || uploadId.isEmpty()) {
            UploadServiceLogger.INSTANCE.debug(LOG_TAG, () -> "null or empty upload ID. Generating it");
            this.uploadId = UUID.randomUUID().toString();
        } else {
            UploadServiceLogger.INSTANCE.debug(LOG_TAG, () -> "setting provided upload ID");
            this.uploadId = uploadId;
        }

        this.serverUrl = serverUrl;
        UploadServiceLogger.INSTANCE.debug(LOG_TAG, () -> "Created new upload request to "
                     + serverUrl + " with ID: " + this.uploadId);
    }

    /**
     * Start the background file upload service.
     * @return the uploadId string. If you have passed your own uploadId in the constructor, this
     *         method will return that same uploadId, otherwise it will return the automatically
     *         generated uploadId
     */
    public String startUpload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationConfig == null) {
            throw new IllegalArgumentException("Android Oreo requires a notification configuration for the service to run. https://developer.android.com/reference/android/content/Context.html#startForegroundService(android.content.Intent)");
        }

        final Intent intent = new Intent(context, UploadService.class);

        UploadServiceKt.setupTask(intent, getTaskClass().getName(), new UploadTaskParameters(
                uploadId,
                serverUrl,
                maxRetries,
                autoDeleteSuccessfullyUploadedFiles,
                notificationConfig,
                files,
                createAdditionalParameters()
        ));

        intent.setAction(UploadServiceConfig.INSTANCE.getUploadAction());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        return uploadId;
    }

    /**
     * Subscribe to events of this upload request
     * @param observer observer to listen for events.
     */
    public void subscribe(SingleRequestObserver observer) {
        observer.subscribe(this);
    }

    protected abstract Parcelable createAdditionalParameters();

    @SuppressWarnings("unchecked")
    protected final B self() {
        return (B)this;
    }

    /**
     * Sets custom notification configuration.
     * If you don't want to display a notification in Notification Center, either pass null
     * as argument or don't call this method.
     *
     * @param config the upload configuration object or null if you don't want a notification
     *               to be displayed
     * @return self instance
     */
    public B setNotificationConfig(UploadNotificationConfig config) {
        this.notificationConfig = config;
        return self();
    }

    /**
     * Sets the automatic file deletion after successful upload.
     * @param autoDeleteFiles true to auto delete files included in the
     *                        request when the upload is completed successfully.
     *                        By default this setting is set to false, and nothing gets deleted.
     * @return self instance
     */
    public B setAutoDeleteFilesAfterSuccessfulUpload(boolean autoDeleteFiles) {
        this.autoDeleteSuccessfullyUploadedFiles = autoDeleteFiles;
        return self();
    }

    /**
     * Sets the maximum number of retries that the library will try if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return self instance
     */
    public B setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return self();
    }

    /**
     * Implement in subclasses to specify the class which will handle the the upload task.
     * The class must be a subclass of {@link UploadTask}.
     * @return class
     */
    protected abstract @NonNull Class<? extends UploadTask> getTaskClass();
}
