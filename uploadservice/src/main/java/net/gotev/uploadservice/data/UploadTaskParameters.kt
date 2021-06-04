package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData

@Parcelize
data class UploadTaskParameters(
    val taskClass: String,
    val id: String,
    val serverUrl: String,
    val maxRetries: Int,
    val autoDeleteSuccessfullyUploadedFiles: Boolean,
    val files: ArrayList<UploadFile>,
    val additionalParameters: PersistableData
) : Parcelable, Persistable {
    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.taskClass, taskClass)
        putString(CodingKeys.id, id)
        putString(CodingKeys.serverUrl, serverUrl)
        putInt(CodingKeys.maxRetries, maxRetries)
        putBoolean(CodingKeys.autoDeleteFiles, autoDeleteSuccessfullyUploadedFiles)
        putArrayData(CodingKeys.files, files.map { it.toPersistableData() })
        putData(CodingKeys.params, additionalParameters)
    }

    companion object : Persistable.Creator<UploadTaskParameters> {
        private object CodingKeys {
            const val taskClass = "taskClass"
            const val id = "id"
            const val serverUrl = "serverUrl"
            const val maxRetries = "maxRetries"
            const val autoDeleteFiles = "autoDeleteFiles"
            const val files = "files"
            const val params = "params"
        }

        override fun createFromPersistableData(data: PersistableData) = UploadTaskParameters(
            taskClass = data.getString(CodingKeys.taskClass),
            id = data.getString(CodingKeys.id),
            serverUrl = data.getString(CodingKeys.serverUrl),
            maxRetries = data.getInt(CodingKeys.maxRetries),
            autoDeleteSuccessfullyUploadedFiles = data.getBoolean(CodingKeys.autoDeleteFiles),
            files = ArrayList(data.getArrayData(CodingKeys.files).map { UploadFile.createFromPersistableData(it) }),
            additionalParameters = data.getData(CodingKeys.params)
        )
    }
}
