package net.gotev.uploadservicedemo

import android.content.Context
import android.util.Log
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.UploadServiceBroadcastReceiver
import net.gotev.uploadservice.network.ServerResponse

/**
 * @author Aleksandar Gotev
 */
class GlobalBroadcastReceiver : UploadServiceBroadcastReceiver() {
    override fun onCompleted(context: Context?, uploadInfo: UploadInfo?, serverResponse: ServerResponse?) {
        Log.e("RECEIVER", "Response: $serverResponse")
    }
}
