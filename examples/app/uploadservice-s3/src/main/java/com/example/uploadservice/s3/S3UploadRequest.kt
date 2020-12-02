package com.example.uploadservice.s3

import android.content.Context
import com.amazonaws.regions.Regions
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile
import java.io.File
import java.io.FileNotFoundException


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
        require(files.isNotEmpty()) { "Add at least one file to start S3 upload!" }
        return super.startUpload()
    }

    @Throws(FileNotFoundException::class)
    @JvmOverloads
    fun addFileToUpload(filePath: String): S3UploadRequest {
        files.add(UploadFile(filePath))
        return this
    }
}
