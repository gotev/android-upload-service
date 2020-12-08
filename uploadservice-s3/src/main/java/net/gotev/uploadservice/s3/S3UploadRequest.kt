package net.gotev.uploadservice.s3
import android.content.Context
import com.amazonaws.regions.Regions
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile
import java.io.FileNotFoundException


class S3UploadRequest(context: Context,
                      bucketName: String,
                      identityPoolId: String,
                      region: Regions,
) : UploadRequest<S3UploadRequest>(context, "serverUrl") {


    protected val s3params = S3UploadTaskParameters(
            bucketName = bucketName,
            identityPoolId = identityPoolId,
            region = region.getName()
    );



    override val taskClass: Class<out UploadTask>
        get() = S3UploadTask::class.java

    override fun getAdditionalParameters() = s3params.toPersistableData()

    /* if this is not set, The uploaded file path will be stored on the root directory of the server
    * if you are uploading `/path/to/myfile.txt`, you will have `myfile.txt`
    * inside the default remote working directory.
    *
    * If this is set, your uploaded file will put to the relative subdirectories
    * so for example if you want to upload to '/images/usa/vacations.zip`,
    * set this to '/images/usa'. Note: no slash should be added at the end
    * If the directory(ies) does not exists, it would automatically create them
     */
    fun setSubDirectory(subDirectory: String): S3UploadRequest {
        s3params.serverSubpath = subDirectory
        return this
    }

    /**
     * This would call the upload method of the upload task class (S3UploadTask)
     */
    override fun startUpload(): String {
        require(files.isNotEmpty()) { "Add at least one file to start S3 upload!" }
        return super.startUpload()
    }

    @Throws(FileNotFoundException::class)
    fun addFileToUpload(filePath: String): S3UploadRequest {
        files.add(UploadFile(filePath))
        return this
    }
}
