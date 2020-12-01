package com.example.uploadservice.s3

import android.util.Log
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.ServerResponse
import java.io.File

class S3UploadTask : UploadTask() {

    private val s3params by lazy {
        S3UploadTaskParameters.createFromPersistableData(params.additionalParameters)
    }

    override fun upload(httpStack: HttpStack) {
        val s3params = s3params
        val credentialsProvider = CognitoCachingCredentialsProvider(context, s3params.identityPoolId, Regions.fromName(s3params.region))
        val s3 = AmazonS3Client(credentialsProvider, Region.getRegion(Regions.fromName(s3params.region)))
        s3.getUrl(s3params.bucket_name, s3params.serverSubpath)
        val transferUtility = TransferUtility.builder().s3Client(s3).context(context).build()
        val file = File(s3params.uploadFilepath)
        require(file.exists()) { "Error! Please choose a valid file for upload" }
        
        val observer = transferUtility.upload(
                s3params.bucket_name,
                s3params.serverSubpath,
                file,
                CannedAccessControlList.Private
        )
        TransferNetworkLossHandler.getInstance(context)

        observer.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {
                    onResponseReceived(ServerResponse.successfulEmpty())
                    Log.i("UtilityObserver","Upload Finished!");
                } else if (state == TransferState.FAILED) {
                    Log.i("UtilityObserver","Upload Failed!");
                } else {
                    Log.i("UtilityObserver","Unkown!");
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                resetUploadedBytes()
                totalBytes = bytesTotal
                onProgress(bytesCurrent)
                val progress = bytesCurrent.toDouble() / bytesTotal * 100
                Log.i("UtilityObserver","Progress: " + progress);
            }

            override fun onError(id: Int, ex: Exception) {
                Log.e("UtilityObserver","Error Occured");
            }
        })
    }
}