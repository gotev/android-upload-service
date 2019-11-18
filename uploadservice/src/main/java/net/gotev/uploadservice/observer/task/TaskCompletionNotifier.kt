package net.gotev.uploadservice.observer.task

import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.network.ServerResponse

class TaskCompletionNotifier(private val service: UploadService) : UploadTaskObserver {
    override fun onStart(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
    }

    override fun onProgress(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
    }

    override fun onSuccess(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        response: ServerResponse
    ) {
    }

    override fun onError(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        exception: Throwable
    ) {
    }

    override fun onCompleted(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
        service.taskCompleted(info.uploadId)
    }
}
