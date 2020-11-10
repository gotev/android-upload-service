package net.gotev.uploadservice

import android.content.Context
import android.os.Parcel
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.persistence.PersistableData

class CreateUploadRequest private constructor(
    context: Context,
    private val params: UploadTaskParameters
) : UploadRequest<CreateUploadRequest>(context, "https://empty") {

    companion object {
        @JvmStatic
        fun fromJson(context: Context, json: String): CreateUploadRequest {
            return fromPersistableData(context, PersistableData.fromJson(json))
        }

        @JvmStatic
        fun fromParcel(context: Context, parcel: Parcel): CreateUploadRequest {
            return fromPersistableData(context, PersistableData.createFromParcel(parcel))
        }

        @JvmStatic
        fun fromPersistableData(context: Context, data: PersistableData): CreateUploadRequest {
            val uploadTaskParameters = UploadTaskParameters.createFromPersistableData(data)

            return CreateUploadRequest(context, uploadTaskParameters)
        }
    }

    init {
        setUploadID(params.id)
        autoDeleteSuccessfullyUploadedFiles = params.autoDeleteSuccessfullyUploadedFiles
        files.addAll(params.files)
        maxRetries = params.maxRetries
        serverUrl = params.serverUrl
    }

    @Suppress("UNCHECKED_CAST")
    override val taskClass: Class<out UploadTask>
        get() = Class.forName(params.taskClass) as Class<out UploadTask>

    override fun getAdditionalParameters() = params.additionalParameters
}