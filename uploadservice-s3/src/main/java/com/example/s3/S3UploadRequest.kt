package com.example.s3

import android.content.Context
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask

class S3UploadRequest(context: Context,
                      uploadFilepath: String,
                      serverSubpath: String,
                      bucket_name: String,
                      identityPoolId: String,
                      region: String,
                      ) : UploadRequest<S3UploadRequest>(context, "serverURL") {
    protected val s3params = S3UploadTaskParameters(uploadFilepath, serverSubpath,
            bucket_name , identityPoolId, region);


    override val taskClass: Class<out UploadTask>
        get() = S3UploadTask::class.java

    override fun getAdditionalParameters() = s3params.toPersistableData()


    override fun startUpload(): String {
        /*require(file.isNotEmpty()) { "Add at least one file to start FTP upload!" }*/
        return super.startUpload()
    }
}