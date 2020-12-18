package net.gotev.uploadservice.protocols.multipart

import android.content.Context
import java.io.FileNotFoundException
import net.gotev.uploadservice.HttpUploadRequest
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile

/**
 * HTTP/Multipart upload request. This is the most common way to upload files on a server.
 * It's the same kind of request that browsers do when you use the &lt;form&gt; tag
 * @param context application context
 * @param serverUrl URL of the server side script that will handle the multipart form upload.
 * E.g.: http://www.yourcompany.com/your/script
 */
class MultipartUploadRequest(context: Context, serverUrl: String) :
    HttpUploadRequest<MultipartUploadRequest>(context, serverUrl) {

    override val taskClass: Class<out UploadTask>
        get() = MultipartUploadTask::class.java

    /**
     * Adds a file to this upload request.
     *
     * @param filePath path to the file that you want to upload
     * @param parameterName Name of the form parameter that will contain file's data
     * @param fileName File name seen by the server side script. If null, the original file name
     * will be used
     * @param contentType Content type of the file. If null or empty, the mime type will be
     * automatically detected. If fore some reasons autodetection fails,
     * `application/octet-stream` will be used by default
     * @return [MultipartUploadRequest]
     */
    @Throws(FileNotFoundException::class)
    @JvmOverloads
    fun addFileToUpload(
        filePath: String,
        parameterName: String,
        fileName: String? = null,
        contentType: String? = null
    ): MultipartUploadRequest {
        require(filePath.isNotBlank() && parameterName.isNotBlank()) {
            "Please specify valid filePath and parameterName. They cannot be blank."
        }

        files.add(
            UploadFile(filePath).apply {
                this.parameterName = parameterName

                this.contentType = if (contentType.isNullOrBlank()) {
                    handler.contentType(context)
                } else {
                    contentType
                }

                remoteFileName = if (fileName.isNullOrBlank()) {
                    handler.name(context)
                } else {
                    fileName
                }
            }
        )

        return this
    }
}
