package net.gotev.uploadservice.data

import android.os.Parcelable
import java.util.LinkedHashMap
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData
import net.gotev.uploadservice.schemehandlers.SchemeHandler

@Parcelize
data class UploadFile @JvmOverloads constructor(
    val path: String,
    val properties: LinkedHashMap<String, String> = LinkedHashMap()
) : Parcelable, Persistable {

    companion object : Persistable.Creator<UploadFile> {
        private const val successfulUpload = "successful_upload"

        private object CodingKeys {
            const val path = "path"
            const val properties = "props"
        }

        override fun createFromPersistableData(data: PersistableData) = UploadFile(
            path = data.getString(CodingKeys.path),
            properties = LinkedHashMap<String, String>().apply {
                val bundle = data.getData(CodingKeys.properties).toBundle()
                bundle.keySet().forEach { propKey ->
                    put(propKey, bundle.getString(propKey)!!)
                }
            }
        )
    }

    @IgnoredOnParcel
    val handler: SchemeHandler by lazy {
        UploadServiceConfig.getSchemeHandler(path)
    }

    @IgnoredOnParcel
    var successfullyUploaded: Boolean
        get() = properties[successfulUpload]?.toBoolean() ?: false
        set(value) {
            properties[successfulUpload] = value.toString()
        }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.path, path)
        putData(
            CodingKeys.properties,
            PersistableData().apply {
                properties.entries.forEach { (propKey, propVal) ->
                    putString(propKey, propVal)
                }
            }
        )
    }
}
