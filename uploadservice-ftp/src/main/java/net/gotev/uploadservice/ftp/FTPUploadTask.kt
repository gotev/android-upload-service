package net.gotev.uploadservice.ftp

import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.ServerResponse

/**
 * Implements the FTP upload logic.
 * @author Aleksandar Gotev
 */
class FTPUploadTask : UploadTask(), FTPClientWrapper.Observer {

    private val ftpParams: FTPUploadTaskParameters
        get() = params.additionalParameters as FTPUploadTaskParameters

    @Throws(Exception::class)
    override fun upload(httpStack: HttpStack) {

        val ftpParams = ftpParams

        FTPClientWrapper(
            uploadId = params.id,
            useSSL = ftpParams.useSSL,
            sslProtocol = ftpParams.secureSocketProtocol,
            implicitSecurity = ftpParams.implicitSecurity,
            connectTimeout = ftpParams.connectTimeout,
            observer = this
        ).use { ftpClient ->
            ftpClient.connect(
                server = params.serverUrl,
                port = ftpParams.port,
                username = ftpParams.username,
                password = ftpParams.password,
                socketTimeout = ftpParams.socketTimeout,
                compressedFileTransfer = ftpParams.compressedFileTransfer
            )

            // this is needed to calculate the total bytes and the uploaded bytes, because if the
            // request fails, the upload method will be called again
            // (until max retries is reached) to retry the upload, so it's necessary to
            // know at which status we left, to be able to properly notify further progress.
            calculateUploadedAndTotalBytes()

            val baseWorkingDir = ftpClient.currentWorkingDirectory
            UploadServiceLogger.debug(javaClass.simpleName, params.id) {
                "FTP default working directory is: $baseWorkingDir"
            }

            for (file in params.files) {
                if (!shouldContinue)
                    break

                if (file.successfullyUploaded)
                    continue

                ftpClient.uploadFile(
                    context,
                    baseWorkingDir,
                    file,
                    ftpParams.createdDirectoriesPermissions
                )
                file.successfullyUploaded = true
            }

            // Broadcast completion only if the user has not cancelled the operation.
            if (shouldContinue) {
                onResponseReceived(ServerResponse.successfulEmpty())
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

    override fun onTransfer(
        client: FTPClientWrapper,
        totalBytesTransferred: Long,
        bytesTransferred: Int,
        streamSize: Long
    ) {
        onProgress(bytesTransferred.toLong())

        if (!shouldContinue) {
            client.close()
        }
    }
}
