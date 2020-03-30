package net.gotev.uploadservice.observer.task

import android.content.Context
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

class BroadcastEmitter(private val context: Context) : UploadTaskObserver {

    private fun send(data: BroadcastData) {
        context.sendBroadcast(data.toIntent())
    }

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
        send(BroadcastData(UploadStatus.InProgress, info))
    }

    override fun onSuccess(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        response: ServerResponse
    ) {
        send(BroadcastData(UploadStatus.Success, info, response))
    }

    override fun onCompleted(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig
    ) {
        send(BroadcastData(UploadStatus.Completed, info))
    }

    override fun onError(
        info: UploadInfo,
        notificationId: Int,
        notificationConfig: UploadNotificationConfig,
        exception: Throwable
    ) {
        send(BroadcastData(UploadStatus.Error, info, null, exception))
    }
}
