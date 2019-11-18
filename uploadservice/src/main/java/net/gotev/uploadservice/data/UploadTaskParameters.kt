package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UploadTaskParameters(
    val id: String,
    val serverUrl: String,
    val maxRetries: Int,
    val autoDeleteSuccessfullyUploadedFiles: Boolean,
    val notificationConfig: UploadNotificationConfig,
    val files: ArrayList<UploadFile>,
    val additionalParameters: Parcelable? = null
) : Parcelable
