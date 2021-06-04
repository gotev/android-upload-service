package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData
import java.util.ArrayList

/**
 * Class which contains specific parameters for HTTP uploads.
 */
@Parcelize
data class HttpUploadTaskParameters(
    var method: String = "POST",
    var usesFixedLengthStreamingMode: Boolean = true,
    val requestHeaders: ArrayList<NameValue> = ArrayList(5),
    val requestParameters: ArrayList<NameValue> = ArrayList(5)
) : Parcelable, Persistable {

    companion object : Persistable.Creator<HttpUploadTaskParameters> {
        private object CodingKeys {
            const val method = "method"
            const val fixedLength = "fixedLength"
            const val headers = "headers"
            const val parameters = "params"
        }

        private fun List<PersistableData>.toNameValueArrayList() =
            ArrayList(map { NameValue.createFromPersistableData(it) })

        override fun createFromPersistableData(data: PersistableData) = HttpUploadTaskParameters(
            method = data.getString(CodingKeys.method),
            usesFixedLengthStreamingMode = data.getBoolean(CodingKeys.fixedLength),
            requestHeaders = try {
                data.getArrayData(CodingKeys.headers).toNameValueArrayList()
            } catch (exc: Throwable) {
                ArrayList()
            },
            requestParameters = try {
                data.getArrayData(CodingKeys.parameters).toNameValueArrayList()
            } catch (exc: Throwable) {
                ArrayList()
            }
        )
    }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.method, method)
        putBoolean(CodingKeys.fixedLength, usesFixedLengthStreamingMode)
        putArrayData(CodingKeys.headers, requestHeaders.map { it.toPersistableData() })
        putArrayData(CodingKeys.parameters, requestParameters.map { it.toPersistableData() })
    }
}
