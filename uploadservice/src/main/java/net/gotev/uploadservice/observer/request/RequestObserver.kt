package net.gotev.uploadservice.observer.request

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.data.UploadInfo

class RequestObserver @JvmOverloads constructor(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    delegate: RequestObserverDelegate,
    shouldAcceptEventsFrom: (uploadInfo: UploadInfo) -> Boolean = { true }
) : BaseRequestObserver(context, delegate, shouldAcceptEventsFrom), LifecycleObserver {

    private var subscribedUploadID: String? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    /**
     * Register this upload receiver to listen for events.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun register() {
        super.register()

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
    override fun unregister() {
        super.unregister()
    }

    /**
     * Subscribe to get only the events from the given upload request. Otherwise, it will listen to
     * all the upload requests.
     */
    fun subscribe(request: UploadRequest<*>) {
        subscribedUploadID = request.startUpload()
        shouldAcceptEventsFrom = { uploadInfo ->
            subscribedUploadID?.let { it == uploadInfo.uploadId } ?: false
        }
    }
}
