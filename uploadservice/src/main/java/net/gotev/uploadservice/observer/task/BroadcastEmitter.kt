package net.gotev.uploadservice.observer.task

import android.content.Context
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class BroadcastEmitter(private val context: Context) : UploadTaskObserver {

    private fun ServerResponse.status() = if (isSuccessful)
        UploadStatus.COMPLETED
    else
        UploadStatus.ERROR

    override fun initialize(info: UploadInfo) {}

    override fun onProgress(info: UploadInfo) {
        BroadcastData(UploadStatus.IN_PROGRESS, info).send(context)
    }

    override fun onCompleted(info: UploadInfo, response: ServerResponse) {
        BroadcastData(response.status(), info, response).send(context)
    }

    override fun onCancelled(info: UploadInfo) {
        BroadcastData(UploadStatus.CANCELLED, info).send(context)
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        BroadcastData(UploadStatus.ERROR, info, null, exception).send(context)
    }
}
