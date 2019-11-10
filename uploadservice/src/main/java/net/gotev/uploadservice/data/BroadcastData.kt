package net.gotev.uploadservice.data

import android.content.Intent
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.network.ServerResponse

@Parcelize
internal data class BroadcastData @JvmOverloads constructor(
    val status: UploadStatus,
    val uploadInfo: UploadInfo,
    val serverResponse: ServerResponse? = null,
    val exception: Throwable? = null
) : Parcelable {
    companion object {
        private const val paramName = "broadcastData"

        fun fromIntent(intent: Intent): BroadcastData? {
            return intent.getParcelableExtra(paramName)
        }
    }

    fun toIntent() = Intent(UploadServiceConfig.broadcastStatusAction).apply {
        setPackage(UploadServiceConfig.namespace)
        putExtra(paramName, this@BroadcastData)
    }
}
