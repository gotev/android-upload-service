package com.example.uploadservice.s3

import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.network.HttpStack

class s3UploadTask : UploadTask() {

    private val s3params by lazy {
        s3UploadTaskParameters.createFromPersistableData(params.additionalParameters)
    }

    override fun upload(httpStack: HttpStack) {
        val s3params = s3params
        val credentialsProvider = CognitoCachingCredentialsProvider(context, s3params.identityPoolId, Regions.fromName(s3params.region))

        val s3 = AmazonS3Client(credentialsProvider, Region.getRegion(Regions.fromName(s3params.region)))
        val transferUtility = TransferUtility.builder().s3Client(s3).context(context).build()


        val observer = transferUtility.upload(
                s3params.bucket_name,
                params.serverUrl,
                s3params.file,
                CannedAccessControlList.Private
        )

        observer.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState) {
                if (state == TransferState.COMPLETED) {

                } else if (state == TransferState.FAILED) {

                } else {

                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                val progress = bytesCurrent.toDouble() / bytesTotal * 100
            }

            override fun onError(id: Int, ex: Exception) {
            }
        })
    }
}