package com.example.uploadservice.s3

import android.content.Context
import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.ServerResponse
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.Exception

class S3ClientWrapper (private val uploadId: String,
                       observer: Observer,
                       context: Context,
                       identityPoolId: String,
                       region: Regions) : Closeable {

    private val credentialsProvider = CognitoCachingCredentialsProvider(context, identityPoolId, region)
    private val s3: AmazonS3Client = AmazonS3Client(credentialsProvider, Region.getRegion(region))
    private val transferUtility = TransferUtility.builder().s3Client(s3).context(context).build()
    private lateinit var transferObserver : TransferObserver

    interface Observer {
        fun onStateChanged(client: S3ClientWrapper, id: Int, state: TransferState?)
        fun onProgressChanged(client: S3ClientWrapper, id: Int, bytesCurrent: Long, bytesTotal: Long)
        fun onError(client: S3ClientWrapper, id: Int, ex: Exception?)
    }
    
    private val transferListener = object : TransferListener {
        override fun onStateChanged(id: Int, state: TransferState?) {
            observer.onStateChanged(this@S3ClientWrapper, id, state)
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            observer.onProgressChanged(this@S3ClientWrapper, id, bytesCurrent, bytesTotal)
        }

        override fun onError(id: Int, ex: Exception?) {
            observer.onError(this@S3ClientWrapper, id, ex)
        }
    }

    @Throws(IOException::class)
    fun uploadFile(
            context: Context,
            bucketName :String,
            serverSubPath: String,
            file: UploadFile) {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Starting S3 upload of: ${file.handler.name(context)}"
        }

        TransferNetworkLossHandler.getInstance(context)
        transferObserver = transferUtility.upload(
                bucketName,
                serverSubPath,
                File(file.path),
                CannedAccessControlList.Private
        )
        transferObserver.setTransferListener(transferListener)
    }
    
    override fun close() {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Closing S3 Client"
        }
        /*transferUtility.pause(transferObserver.id)*/
    }

}