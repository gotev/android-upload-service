package net.gotev.uploadservice.observer.task

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

class BroadcastEmitter(private val context: Context) : UploadTaskObserver {

    private fun send(data: BroadcastData) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(data.toIntent())
    }

    override fun initialize(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig?) {}

    override fun onProgress(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig?) {
        send(BroadcastData(UploadStatus.IN_PROGRESS, info))
    }

    override fun onSuccess(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig?, response: ServerResponse) {
        send(BroadcastData(UploadStatus.SUCCESS, info, response))
    }

    override fun onCompleted(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig?) {
        send(BroadcastData(UploadStatus.COMPLETED, info))
    }

    override fun onError(info: UploadInfo, notificationId: Int, notificationConfig: UploadNotificationConfig?, exception: Throwable) {
        send(BroadcastData(UploadStatus.ERROR, info, null, exception))
    }
}
