package com.example.s3

import android.content.Context
import com.amazonaws.regions.Regions
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask
import java.io.File

class S3UploadRequest(context: Context,
                      bucket_name: String,
                      identityPoolId: String,
                      region: Regions,
                      uploadFilepath: String,
) : UploadRequest<S3UploadRequest>(context, "serverUrl") {

    val addressLastSegment = "/" + File(uploadFilepath).name

    protected val s3params = S3UploadTaskParameters(uploadFilepath, addressLastSegment,
            bucket_name , identityPoolId, region.getName());



    override val taskClass: Class<out UploadTask>
        get() = S3UploadTask::class.java

    override fun getAdditionalParameters() = s3params.toPersistableData()

    fun setSubDirectory(subDirectory: String): S3UploadRequest {
        s3params.serverSubpath = subDirectory + addressLastSegment
        return this
    }


    override fun startUpload(): String {
        return super.startUpload()
    }
}
