package com.example.s3

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Region
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask
import java.io.File

class s3UploadRequest(context: Context, serverUrl: String,
                      credentialsProvider: CognitoCachingCredentialsProvider,
                      region: Region,
                      bucket_name: String,
                      bucket_key: String,
                      Identity_key: String,
                      file: File,
                      ) : UploadRequest<s3UploadRequest>(context, serverUrl) {
    protected val s3params = s3UploadTaskParameters(credentialsProvider, region, bucket_name, bucket_key, Identity_key, file);
    
    
    override val taskClass: Class<out UploadTask>
        get() = s3UploadTask::class.java

    override fun getAdditionalParameters() = s3params.toPersistableData()
    

    override fun startUpload(): String {
        require(files.isNotEmpty()) { "Add at least one file to start FTP upload!" }
        return super.startUpload()
    }
}