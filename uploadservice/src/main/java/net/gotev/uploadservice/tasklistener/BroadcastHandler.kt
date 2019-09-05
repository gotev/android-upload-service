package net.gotev.uploadservice.tasklistener

import android.os.Handler
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.data.BroadcastData
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadStatus
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class BroadcastHandler(private val service: UploadService,
                       private val uploadId: String) : UploadTaskListener {

    private val mainThreadHandler by lazy {
        Handler(service.mainLooper)
    }

    override fun initialize(info: UploadInfo) {}

    override fun onProgress(info: UploadInfo) {
        val delegate = UploadService.getUploadStatusDelegate(uploadId)

        if (delegate != null) {
            mainThreadHandler.post { delegate.onProgress(service, info) }
        } else {
            BroadcastData(UploadStatus.IN_PROGRESS, info).send(service)
        }
    }

    override fun onCompleted(info: UploadInfo, response: ServerResponse) {
        val delegate = UploadService.getUploadStatusDelegate(uploadId)

        if (delegate != null) {
            mainThreadHandler.post {
                if (response.isSuccessful) {
                    delegate.onCompleted(service, info, response)
                } else {
                    delegate.onError(service, info, response, null)
                }
            }
        } else {
            BroadcastData(
                    if (response.isSuccessful) UploadStatus.COMPLETED else UploadStatus.ERROR,
                    info,
                    response
            ).send(service)
        }
    }

    override fun onCancelled(info: UploadInfo) {
        val delegate = UploadService.getUploadStatusDelegate(uploadId)

        if (delegate != null) {
            mainThreadHandler.post { delegate.onCancelled(service, info) }
        } else {
            BroadcastData(UploadStatus.CANCELLED, info).send(service)
        }
    }

    override fun onError(info: UploadInfo, exception: Throwable) {
        val delegate = UploadService.getUploadStatusDelegate(uploadId)

        if (delegate != null) {
            mainThreadHandler.post { delegate.onError(service, info, null, exception) }
        } else {
            BroadcastData(UploadStatus.ERROR, info, null, exception).send(service)
        }
    }
}
