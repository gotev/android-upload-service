package net.gotev.uploadservice.observer.request

import android.content.Context
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse

interface RequestObserverDelegate {
    /**
     * Called when the upload progress changes.
     *
     * @param context context
     * @param uploadInfo upload status information
     */
    fun onProgress(context: Context, uploadInfo: UploadInfo)

    /**
     * Called when the upload is completed successfully.
     *
     * @param context context
     * @param uploadInfo upload status information
     * @param serverResponse response got from the server
     */
    fun onSuccess(context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse)

    /**
     * Called when an error happens during the upload.
     *
     * @param context context
     * @param uploadInfo upload status information
     * @param exception exception that caused the error
     */
    fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable)

    /**
     * Called when the upload is completed wither with success or error.
     *
     * @param context context
     * @param uploadInfo upload status information
     */
    fun onCompleted(context: Context, uploadInfo: UploadInfo)

    /**
     * Called only when listening to a single upload ID and you register the request observer,
     * but the upload ID is not present in UploadService's task list, meaning it has completed.
     * In this case, you cannot know with which state it finished (success or error).
     *
     * Useful when used in activities and the following scenario applies:
     * - user triggers an upload in an activity which shows the progress
     * - user navigates away from that activity and comes back later after the upload completed and
     *   you need to to some stuff to adjust UI properly
     */
    fun onCompletedWhileNotObserving()
}
