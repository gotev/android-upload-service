package net.gotev.uploadservicedemo

import android.content.Context
import android.util.Log
import net.gotev.uploadservice.UploadServiceBroadcastReceiver
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class GlobalBroadcastReceiver : UploadServiceBroadcastReceiver() {
    override fun onProgress(context: Context?, uploadInfo: UploadInfo?) {
        Log.e("RECEIVER", "Progress: $uploadInfo")
    }

    override fun onError(context: Context?, uploadInfo: UploadInfo?, serverResponse: ServerResponse?, exception: Throwable?) {
        Log.e("RECEIVER", "Error", exception)
    }

    override fun onCompleted(context: Context?, uploadInfo: UploadInfo?, serverResponse: ServerResponse?) {
        Log.e("RECEIVER", "Response: $serverResponse")
    }

    override fun onCancelled(context: Context?, uploadInfo: UploadInfo?) {
        Log.e("CANCELLED", "Cancelled: $uploadInfo")
    }
}
