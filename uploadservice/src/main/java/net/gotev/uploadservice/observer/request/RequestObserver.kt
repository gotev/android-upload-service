package net.gotev.uploadservice.observer.request

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadStatus

class RequestObserver(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val delegate: RequestObserverDelegate
) : BroadcastReceiver(), LifecycleObserver {

    private var subscribedUploadID: String? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val safeIntent = intent ?: return
        if (safeIntent.action != UploadServiceConfig.broadcastStatusAction) return
        val data = BroadcastData.fromIntent(safeIntent) ?: return

        val uploadInfo = data.uploadInfo

        if (!shouldAcceptEventFrom(uploadInfo)) {
            return
        }

        when (data.status) {
            UploadStatus.InProgress -> delegate.onProgress(context, uploadInfo)
            UploadStatus.Error -> delegate.onError(context, uploadInfo, data.exception!!)
            UploadStatus.Success -> delegate.onSuccess(context, uploadInfo, data.serverResponse!!)
            UploadStatus.Completed -> delegate.onCompleted(context, uploadInfo)
        }
    }

    /**
     * Method called every time a new event arrives from an upload task, to decide whether or not
     * to process it. If this request observer subscribed a particular upload task, it will listen
     * only to it
     *
     * @param uploadInfo upload info to
     * @return true to accept the event, false to discard it
     */
    private fun shouldAcceptEventFrom(uploadInfo: UploadInfo): Boolean {
        val uploadId = subscribedUploadID ?: return true
        return uploadId == uploadInfo.uploadId
    }

    /**
     * Register this upload receiver to listen for events.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun register() {
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(this, UploadServiceConfig.broadcastStatusIntentFilter)

        subscribedUploadID?.let {
            if (!UploadService.taskList.contains(it)) {
                delegate.onCompletedWhileNotObserving()
            }
        }
    }

    /**
     * Unregister this upload receiver from listening events.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unregister() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
    }

    /**
     * Subscribe to get only the events from the given upload request. Otherwise, it will listen to
     * all the upload requests.
     */
    fun subscribe(request: UploadRequest<*>) {
        subscribedUploadID = request.startUpload()
    }
}
