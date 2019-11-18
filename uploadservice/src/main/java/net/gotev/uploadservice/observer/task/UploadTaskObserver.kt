package net.gotev.uploadservice.observer.task

import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.network.ServerResponse

interface UploadTaskObserver {
    fun onStart(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig)

    fun onProgress(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    )

    fun onSuccess(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        response: ServerResponse
    )

    fun onError(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        exception: Throwable
    )

    fun onCompleted(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    )
}
