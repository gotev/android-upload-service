package net.gotev.uploadservice.data

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.UploadInfo
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.network.ServerResponse

/**
 * Class which contains all the data passed in broadcast intents to notify task progress, errors,
 * completion or cancellation.
 *
 * @author gotev (Aleksandar Gotev)
 */
@Parcelize
internal data class BroadcastData @JvmOverloads constructor(
        val status: UploadStatus,
        val uploadInfo: UploadInfo,
        val serverResponse: ServerResponse? = null,
        val exception: Exception? = null
) : Parcelable {
    companion object {
        private const val paramName = "broadcastData"

        fun fromIntent(intent: Intent): BroadcastData? {
            return intent.getParcelableExtra(paramName)
        }
    }

    fun send(context: Context) {
        context.sendBroadcast(Intent(UploadServiceConfig.broadcastAction).apply {
            setPackage(UploadServiceConfig.namespace)
            putExtra(paramName, this@BroadcastData)
        })
    }
}
