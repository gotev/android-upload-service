package net.gotev.uploadservice.observer.task

import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
interface UploadTaskObserver {
    fun initialize(info: UploadInfo)
    fun onProgress(info: UploadInfo)
    fun onSuccess(info: UploadInfo, response: ServerResponse)
    fun onError(info: UploadInfo, exception: Throwable)
    fun onCompleted(info: UploadInfo)
}
