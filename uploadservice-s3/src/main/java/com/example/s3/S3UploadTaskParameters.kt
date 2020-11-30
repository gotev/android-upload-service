package com.example.s3

import android.os.Parcelable
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData
import java.io.File

@Parcelize
data class s3UploadTaskParameters (
        var credentialsProvider: CognitoCachingCredentialsProvider,
        var region: Region,
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
            const val credentialsProvider = "credentialsProvider"
            const val region = "region"
            const val bucket_name = "bucket_name"
            const val bucket_key = "bucket_key"
            const val Identity_key = "Identity_key"
            const val file = "file"
        }

        override fun createFromPersistableData(data: PersistableData) = s3UploadTaskParameters(
                credentialsProvider = data.getCognitoCachingCredentialsProvider(CodingKeys.credentialsProvider),
                region = data.getRegion(CodingKeys.region),
                bucket_name = data.getString(CodingKeys.bucket_name),
                bucket_key = data.getString(CodingKeys.bucket_key),
                Identity_key = data.getString(CodingKeys.Identity_key),
                file = data.getFile(CodingKeys.file)
        )
    }

    override fun toPersistableData() = PersistableData().apply {
        putCognitoCachingCredentialsProvider(CodingKeys.credentialsProvider, credentialsProvider)
        putRegion(CodingKeys.region, region)
        putString(CodingKeys.bucket_name, bucket_name)
        putString(CodingKeys.bucket_key, bucket_key)
        putString(CodingKeys.Identity_key, Identity_key)
        putFile(CodingKeys.file, file)
    }
}
