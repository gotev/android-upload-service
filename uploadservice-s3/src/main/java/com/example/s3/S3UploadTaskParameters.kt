package com.example.s3

import android.os.Parcelable
import com.amazonaws.regions.Regions
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData
import java.io.File

@Parcelize
data class S3UploadTaskParameters (
        var uploadFilepath : String,
        var serverSubpath: String = "",
        var bucket_name: String,
        var identityPoolId: String,
        var region: String = Regions.US_EAST_1.name,
        var bucket_key: String = "com.aws.s3$bucket_name",
        var Identity_key: String = "com.aws.s3$identityPoolId",
) : Parcelable, Persistable {
    companion object : Persistable.Creator<S3UploadTaskParameters> {
        /**
         * The default FTP connection timeout in milliseconds.
         */

        private object CodingKeys {
            const val uploadFilepath = "uploadFilepath"
            const val serverSubpath = "serverSubpath"
            const val bucket_name = "bucket_name"
            const val identityPoolId = "identityPoolId"
            const val region = "region"
            const val bucket_key = "bucket_key"
            const val Identity_key = "Identity_key"
        }

        override fun createFromPersistableData(data: PersistableData) = S3UploadTaskParameters(
                uploadFilepath = data.getString(CodingKeys.uploadFilepath),
                serverSubpath = data.getString(CodingKeys.serverSubpath),
                bucket_name = data.getString(CodingKeys.bucket_name),
                identityPoolId = data.getString(CodingKeys.identityPoolId),
                region = data.getString(CodingKeys.region),
                bucket_key = data.getString(CodingKeys.bucket_key),
                Identity_key = data.getString(CodingKeys.Identity_key),
        )
    }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.uploadFilepath, uploadFilepath)
        putString(CodingKeys.serverSubpath, serverSubpath)
        putString(CodingKeys.bucket_name, bucket_name)
        putString(CodingKeys.identityPoolId, identityPoolId)
        putString(CodingKeys.region, region)
        putString(CodingKeys.bucket_key, bucket_key)
        putString(CodingKeys.Identity_key, Identity_key)
    }
}