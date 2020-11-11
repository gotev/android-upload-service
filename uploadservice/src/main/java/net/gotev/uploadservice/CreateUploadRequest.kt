package net.gotev.uploadservice

import android.content.Context
import android.os.Parcel
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.persistence.PersistableData

class CreateUploadRequest private constructor(
    context: Context,
    private val params: UploadTaskParameters
) : UploadRequest<CreateUploadRequest>(context, "https://empty") {

    companion object {
        /**
         * Creates an upload request from raw JSON Data
         * created using [UploadRequest.toPersistableData]
         *
         * Since [UploadNotificationConfig] is not included as it's not persistable, when the
         * upload request gets recreated, [UploadServiceConfig.notificationConfigFactory]
         * is used to get an [UploadNotificationConfig]. You can override it using
         * [setNotificationConfig] method.
         */
        @JvmStatic
        fun fromJson(context: Context, json: String): CreateUploadRequest {
            return fromPersistableData(context, PersistableData.fromJson(json))
        }

        /**
         * Creates an upload request from a Parcel created using [UploadRequest.toPersistableData]
         *
         * Since [UploadNotificationConfig] is not included as it's not persistable, when the
         * upload request gets recreated, [UploadServiceConfig.notificationConfigFactory]
         * is used to get an [UploadNotificationConfig]. You can override it using
         * [setNotificationConfig] method.
         */
        @JvmStatic
        fun fromParcel(context: Context, parcel: Parcel): CreateUploadRequest {
            return fromPersistableData(context, PersistableData.createFromParcel(parcel))
        }

        /**
         * Creates an upload request from persistable data
         * created using [UploadRequest.toPersistableData]
         *
         * Since [UploadNotificationConfig] is not included as it's not persistable, when the
         * upload request gets recreated, [UploadServiceConfig.notificationConfigFactory]
         * is used to get an [UploadNotificationConfig]. You can override it using
         * [setNotificationConfig] method.
         */
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
