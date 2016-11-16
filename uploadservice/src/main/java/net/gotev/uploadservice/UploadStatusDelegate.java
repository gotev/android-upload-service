package net.gotev.uploadservice;

import android.content.Context;

/**
 * Defines the methods that has to be implemented by a class who wants to listen for upload status
 * events.
 *
 * @author Aleksandar Gotev
 */
public interface UploadStatusDelegate {
    /**
     * Called when the upload progress changes. Override this method to add your own logic.
     *
     * @param context context
     * @param uploadInfo upload status information
     */
    void onProgress(final Context context, final UploadInfo uploadInfo);

    /**
     * Called when an error happens during the upload. Override this method to add your own logic.
     *
     * @param context context
     * @param uploadInfo upload status information
     * @param exception exception that caused the error
     */
    void onError(final Context context, final UploadInfo uploadInfo, final Exception exception);

    /**
     * Called when the upload is completed successfully. Override this method to add your own logic.
     *
     * @param context context
     * @param uploadInfo upload status information
     * @param serverResponse response got from the server
     */
    void onCompleted(final Context context, final UploadInfo uploadInfo, final ServerResponse serverResponse);

    /**
     * Called when the upload is cancelled. Override this method to add your own logic.
     *
     * @param context context
     * @param uploadInfo upload status information
     */
    void onCancelled(final Context context, final UploadInfo uploadInfo);
}
