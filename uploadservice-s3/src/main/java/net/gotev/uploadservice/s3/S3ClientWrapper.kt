package net.gotev.uploadservice.s3

import android.content.Context
import android.content.Intent
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferService
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.logger.UploadServiceLogger
import java.io.Closeable
import java.io.File
import java.lang.Exception

class S3ClientWrapper(
    private val uploadId: String,
    observer: Observer,
    context: Context,
    identityPoolId: String,
    region: Regions
) : Closeable {

    private val credentialsProvider = CognitoCachingCredentialsProvider(context, identityPoolId, region)
    private val s3: AmazonS3Client = AmazonS3Client(credentialsProvider, Region.getRegion(region))
    private val transferUtility = TransferUtility.builder().s3Client(s3).context(context).build()
    private lateinit var transferObserver: TransferObserver
    private lateinit var uploadingFile: UploadFile

    init {
        context.startService(Intent(context, TransferService::class.java))
    }

    interface Observer {
        fun onStateChanged(client: S3ClientWrapper, uploadFile: UploadFile, id: Int, state: TransferState?)
        fun onProgressChanged(client: S3ClientWrapper, id: Int, bytesCurrent: Long, bytesTotal: Long)
        fun onError(client: S3ClientWrapper, id: Int, ex: Exception?)
    }

    private val transferListener = object : TransferListener {
        override fun onStateChanged(id: Int, state: TransferState?) {
            observer.onStateChanged(this@S3ClientWrapper, uploadingFile, id, state)
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            observer.onProgressChanged(this@S3ClientWrapper, id, bytesCurrent, bytesTotal)
        }

        override fun onError(id: Int, ex: Exception?) {
            observer.onError(this@S3ClientWrapper, id, ex)
        }
    }

    @Throws(Exception::class)
    fun uploadFile(
        context: Context,
        bucketName: String,
        serverSubPath: String,
        uploadFile: UploadFile,
        cannedAccessControlList: CannedAccessControlList
    ) {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Starting S3 upload of: ${uploadFile.handler.name(context)}"
        }
        uploadingFile = uploadFile
        val file = File(uploadFile.path)
        transferObserver = transferUtility.upload(
            bucketName,
            serverSubPath + "/" + file.name,
            file,
            cannedAccessControlList
        )
        transferObserver.setTransferListener(transferListener)
    }

    override fun close() { }

    fun stopUpload() {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) { "Stopping S3 Upload" }
        transferUtility.pause(transferObserver.id)
        transferObserver.cleanTransferListener()
    }
}
