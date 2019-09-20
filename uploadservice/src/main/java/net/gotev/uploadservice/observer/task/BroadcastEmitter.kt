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

    override fun initialize(info: UploadInfo) {}

    override fun onProgress(info: UploadInfo) {
        BroadcastData(UploadStatus.IN_PROGRESS, info).send(context)
    }

    override fun onSuccess(info: UploadInfo, response: ServerResponse) {
        BroadcastData(UploadStatus.SUCCESS, info, response).send(context)
    }

    override fun onCompleted(info: UploadInfo) {
        BroadcastData(UploadStatus.COMPLETED, info).send(context)
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        BroadcastData(UploadStatus.ERROR, info, null, exception).send(context)
    }
}
