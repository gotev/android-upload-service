package net.gotev.uploadservice.observer.request

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

/**
 * Broadcast receiver to subclass to create a receiver for [UploadService] events.
 *
 * It provides the boilerplate code to properly handle broadcast messages coming from the
 * upload service and dispatch them to the proper handler method.
 *
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 * @author mabdurrahman
 */
abstract class RequestObserver : BroadcastReceiver() {

    final override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        if (safeIntent.action != UploadServiceConfig.broadcastAction) return
        val data = BroadcastData.fromIntent(safeIntent) ?: return

        val uploadInfo = data.uploadInfo

        if (!shouldAcceptEventFrom(uploadInfo)) {
            return
        }

        when (data.status) {
            UploadStatus.IN_PROGRESS -> onProgress(context, uploadInfo)
            UploadStatus.ERROR -> onError(context, uploadInfo, data.exception!!)
            UploadStatus.SUCCESS -> onSuccess(context, uploadInfo, data.serverResponse!!)
            UploadStatus.COMPLETED -> onCompleted(context, uploadInfo)
        }
    }

    /**
     * Method called every time a new event arrives from an upload task, to decide whether or not
     * to process it. This is useful if you want to filter events based on custom business logic.
     *
     * @param uploadInfo upload info to
     * @return true to accept the event, false to discard it
     */
    open fun shouldAcceptEventFrom(uploadInfo: UploadInfo): Boolean {
        return true
    }

    /**
     * Register this upload receiver.<br></br>
     * If you use this receiver in an [android.app.Activity], you have to call this method inside
     * [android.app.Activity.onResume], after `super.onResume();`.<br></br>
     * If you use it in a [android.app.Service], you have to
     * call this method inside [android.app.Service.onCreate], after `super.onCreate();`.
     *
     * @param context context in which to register this receiver
     */
    fun register(context: Context) {
        context.registerReceiver(this, UploadServiceConfig.broadcastIntentFilter)
    }

    /**
     * Unregister this upload receiver.<br></br>
     * If you use this receiver in an [android.app.Activity], you have to call this method inside
     * [android.app.Activity.onPause], after `super.onPause();`.<br></br>
     * If you use it in a [android.app.Service], you have to
     * call this method inside [android.app.Service.onDestroy].
     *
     * @param context context in which to unregister this receiver
     */
    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }

    /**
     * Called when the upload progress changes.
     *
     * @param context    context
     * @param uploadInfo upload status information
     */
    abstract fun onProgress(context: Context, uploadInfo: UploadInfo)

    /**
     * Called when the upload is completed successfully.
     *
     * @param context        context
     * @param uploadInfo     upload status information
     * @param serverResponse response got from the server
     */
    abstract fun onSuccess(context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse)

    /**
     * Called when an error happens during the upload.
     *
     * @param context context
     * @param uploadInfo upload status information
     * @param exception exception that caused the error
     */
    abstract fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable)

    /**
     * Called when the upload is completed wither with success or error.
     *
     * @param context context
     * @param uploadInfo upload status information
     */
    abstract fun onCompleted(context: Context, uploadInfo: UploadInfo)
}
