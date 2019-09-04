package net.gotev.uploadservice.tasklistener

import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
// TODO: broadcast only implementation
interface UploadTaskListener {
    fun initialize(info: UploadInfo)
    fun onProgress(info: UploadInfo)
    fun onCompleted(info: UploadInfo, response: ServerResponse)
    fun onCancelled(info: UploadInfo)
    fun onError(info: UploadInfo, exception: Throwable)
}
