package net.gotev.uploadservice.utils

import android.app.Application
import android.content.Context
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

sealed class UploadRequestStatus {
    class Successful(val response: ServerResponse) : UploadRequestStatus()
    class ServerError(val response: ServerResponse) : UploadRequestStatus()
    class OtherError(val exception: Throwable) : UploadRequestStatus()
}

fun UploadRequest<*>.runBlocking(context: Application): UploadRequestStatus {
    val lock = CountDownLatch(1)

    var resultingException: Throwable? = null
    var resultingServerResponse: ServerResponse? = null

    val uploadID = UUID.randomUUID().toString()

    setUploadID(uploadID)

    val observer = GlobalRequestObserver(context, object : RequestObserverDelegate {
        override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        }

        override fun onSuccess(
            context: Context,
            uploadInfo: UploadInfo,
            serverResponse: ServerResponse
        ) {
            resultingServerResponse = serverResponse
        }

        override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
            resultingException = exception
        }

        override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
            lock.countDown()
        }

        override fun onCompletedWhileNotObserving() {
            lock.countDown()
        }
    }, shouldAcceptEventsFrom = { it.uploadId == uploadID })

    observer.register()

    startUpload()

    lock.await(5000, TimeUnit.MILLISECONDS)

    observer.unregister()

    val response = resultingServerResponse
    val exception = resultingException

    if (response != null) {
        return UploadRequestStatus.Successful(response)
    }

    if (exception != null && exception is UploadError) {
        return UploadRequestStatus.ServerError(exception.serverResponse)
    }

    return UploadRequestStatus.OtherError(exception!!)
}
