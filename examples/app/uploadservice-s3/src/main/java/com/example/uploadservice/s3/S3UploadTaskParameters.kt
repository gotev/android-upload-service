package com.example.uploadservice.s3

import android.os.Parcelable
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData
import java.io.File
import java.util.*

@Parcelize
data class s3UploadTaskParameters (
        var region: String = Regions.US_EAST_1.name,
        var identityPoolId: String = "",
        var bucket_name: String = "",
        var bucket_key: String = "com.aws.s3.BUCKET_NAME",
        var Identity_key: String = "com.aws.s3.IDENTITY_POOL_ID",
        var file : File

) : Parcelable, Persistable {
    companion object : Persistable.Creator<s3UploadTaskParameters> {
        /**
         * The default FTP connection timeout in milliseconds.
         */

        private object CodingKeys {
            const val region = "region"
            const val identityPoolId = "identityPoolId"
            const val bucket_name = "bucket_name"
            const val bucket_key = "bucket_key"
            const val Identity_key = "Identity_key"
            const val file = "file"
        }

        override fun createFromPersistableData(data: PersistableData) = s3UploadTaskParameters(
                region = data.getString(CodingKeys.region),
                identityPoolId = data.getString(CodingKeys.identityPoolId),
                bucket_name = data.getString(CodingKeys.bucket_name),
                bucket_key = data.getString(CodingKeys.bucket_key),
                Identity_key = data.getString(CodingKeys.Identity_key),
                file = data.getFile(CodingKeys.file)
        )
    }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.region, region)
        putString(CodingKeys.identityPoolId, identityPoolId)
        putString(CodingKeys.bucket_name, bucket_name)
        putString(CodingKeys.bucket_key, bucket_key)
        putString(CodingKeys.Identity_key, Identity_key)
        putFile(CodingKeys.file, file)
    }
}