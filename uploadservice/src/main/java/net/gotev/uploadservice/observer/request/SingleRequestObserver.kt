package net.gotev.uploadservice.observer.request

import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.data.UploadInfo

abstract class SingleRequestObserver : RequestObserver() {
    private var uploadID: String? = null

    fun subscribe(request: UploadRequest<*>) {
        uploadID = request.startUpload()
    }

    final override fun shouldAcceptEventFrom(uploadInfo: UploadInfo): Boolean {
        if (uploadID == null) return false
        return uploadInfo.uploadId == uploadID
    }
}
