package net.gotev.uploadservice.observer.task

import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class TaskCompletionNotifier(private val service: UploadService): UploadTaskObserver {
    override fun initialize(info: UploadInfo) {
    }

    override fun onProgress(info: UploadInfo) {
    }

    override fun onCompleted(info: UploadInfo, response: ServerResponse) {
        service.taskCompleted(info.uploadId)
    }

    override fun onCancelled(info: UploadInfo) {
        service.taskCompleted(info.uploadId)
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        service.taskCompleted(info.uploadId)
    }
}
