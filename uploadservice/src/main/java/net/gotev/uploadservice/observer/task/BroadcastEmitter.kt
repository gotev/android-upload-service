package net.gotev.uploadservice.observer.task

import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class BroadcastEmitter(private val service: UploadService) : UploadTaskObserver {

    private fun ServerResponse.status() = if (isSuccessful)
        UploadStatus.COMPLETED
    else
        UploadStatus.ERROR

    override fun initialize(info: UploadInfo) {}

    override fun onProgress(info: UploadInfo) {
        BroadcastData(UploadStatus.IN_PROGRESS, info).send(service)
    }

    override fun onCompleted(info: UploadInfo, response: ServerResponse) {
        BroadcastData(response.status(), info, response).send(service)
    }

    override fun onCancelled(info: UploadInfo) {
        BroadcastData(UploadStatus.CANCELLED, info).send(service)
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        BroadcastData(UploadStatus.ERROR, info, null, exception).send(service)
    }
}
