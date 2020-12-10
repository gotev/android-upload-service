package net.gotev.uploadservice.s3

import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.CannedAccessControlList
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.ServerResponse

class S3UploadTask() : UploadTask(), S3ClientWrapper.Observer {

    private var uploadBytes: Long = 0

    private val s3params by lazy {
        S3UploadTaskParameters.createFromPersistableData(params.additionalParameters)
    }
    @Throws(Exception::class)
    override fun upload(httpStack: HttpStack) {
        val s3params = s3params
        S3ClientWrapper(uploadId = params.id,
                context = context,
                identityPoolId = s3params.identityPoolId,
                region = Regions.fromName(s3params.region),
                observer = this
        ).use { s3Client ->


            // this is needed to calculate the total bytes and the uploaded bytes, because if the
            // request fails, the upload method will be called again
            // (until max retries is reached) to retry the upload, so it's necessary to
            // know at which status we left, to be able to properly notify further progress.
            calculateUploadedAndTotalBytes();


            for (file in params.files) {
                if (!shouldContinue)
                    break

                if (file.successfullyUploaded)
                    continue

                s3Client.uploadFile(context, s3params.bucketName, s3params.serverSubpath, file, CannedAccessControlList.valueOf(s3params.cannedAccessControlList))
            }
        }
    }

    /**
     * Calculates the total bytes of this upload task.
     * This the sum of all the lengths of the successfully uploaded files and also the pending
     * ones.
     */
    private fun calculateUploadedAndTotalBytes() {
        resetUploadedBytes()
        uploadBytes = 0
        var totalUploaded: Long = 0

        for (file in successfullyUploadedFiles) {
            totalUploaded += file.handler.size(context)
        }

        totalBytes = totalUploaded

        for (file in params.files) {
            totalBytes += file.handler.size(context)
        }

        onProgress(totalUploaded)
    }

    @Throws(Exception::class)
    override fun onStateChanged(client: S3ClientWrapper, uploadFile: UploadFile , id: Int, state: TransferState?) {
        if (state == TransferState.COMPLETED) {
            if (shouldContinue) {
                params.files.filter { it.equals(uploadFile) }.first().successfullyUploaded = true
                onResponseReceived(ServerResponse.successfulEmpty())
            }
        } else {
            UploadServiceLogger.debug(javaClass.simpleName, params.id ) { "state of file " + uploadFile.path + " changed to" + (state?.name ?: "unknown!!!") }
        }
    }

    override fun onProgressChanged(client: S3ClientWrapper, id: Int, bytesCurrent: Long, bytesTotal: Long) {
        onProgress(bytesCurrent - uploadBytes)
        uploadBytes = bytesCurrent
        if (!shouldContinue) {
            client.stopUpload()
            exceptionHandling(Exception("User cancelled upload!"))
        }
    }

    override fun onError(client: S3ClientWrapper, id: Int, ex: java.lang.Exception?) {
        UploadServiceLogger.debug(javaClass.simpleName, params.id) { ex.toString() }
        onResponseReceived(ServerResponse.errorEmpty())
        exceptionHandling(Exception(ex))
    }
}
