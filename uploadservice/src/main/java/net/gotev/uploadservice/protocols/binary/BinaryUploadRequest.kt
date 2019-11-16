package net.gotev.uploadservice.protocols.binary

import android.content.Context
import net.gotev.uploadservice.HttpUploadRequest
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.logger.UploadServiceLogger.NA
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Binary file upload request. The binary upload uses a single file as the raw body of the
 * upload request.
 * @param context application context
 * @param serverUrl URL of the server side script that will handle the multipart form upload.
 * E.g.: http://www.yourcompany.com/your/script
 */
class BinaryUploadRequest(context: Context, serverUrl: String) :
    HttpUploadRequest<BinaryUploadRequest>(context, serverUrl) {

    override val taskClass: Class<out UploadTask>
        get() = BinaryUploadTask::class.java

    /**
     * Sets the file used as raw body of the upload request.
     *
     * @param path path to the file that you want to upload
     * @throws FileNotFoundException if the file to upload does not exist
     * @return [BinaryUploadRequest]
     */
    @Throws(IOException::class)
    fun setFileToUpload(path: String): BinaryUploadRequest {
        files.clear()
        files.add(UploadFile(path))
        return this
    }

    override fun addParameter(paramName: String, paramValue: String): BinaryUploadRequest {
        logDoesNotSupportParameters()
        return this
    }

    override fun addArrayParameter(paramName: String, vararg array: String): BinaryUploadRequest {
        logDoesNotSupportParameters()
        return this
    }

    override fun addArrayParameter(paramName: String, list: List<String>): BinaryUploadRequest {
        logDoesNotSupportParameters()
        return this
    }

    override fun startUpload(): String {
        require(files.isNotEmpty()) { "Set the file to be used in the request body first!" }
        return super.startUpload()
    }

    private fun logDoesNotSupportParameters() {
        UploadServiceLogger.error(javaClass.simpleName, NA) {
            "This upload method does not support adding parameters"
        }
    }
}
