package net.gotev.uploadservicedemo

import android.content.Context
import android.util.Log
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate

/**
 * @author Aleksandar Gotev
 */
class GlobalRequestObserverDelegate : RequestObserverDelegate {
    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        Log.e("RECEIVER", "Progress: $uploadInfo")
    }

    override fun onSuccess(context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse) {
        Log.e("RECEIVER", "Success: $serverResponse")
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        when (exception) {
            is UserCancelledUploadException -> {
                Log.e("RECEIVER", "Error, user cancelled upload: $uploadInfo")
            }

            is UploadError -> {
                Log.e("RECEIVER", "Error, upload error: ${exception.serverResponse}")
            }

            else -> {
                Log.e("RECEIVER", "Error: $uploadInfo", exception)
            }
        }
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
        Log.e("RECEIVER", "Completed: $uploadInfo")
    }

    override fun onCompletedWhileNotObserving() {
        Log.e("RECEIVER", "Completed while not observing")
    }
}
