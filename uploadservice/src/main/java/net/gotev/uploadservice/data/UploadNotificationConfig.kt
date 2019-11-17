package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class UploadNotificationConfig(
    val notificationChannelId: String,
    // TODO: study how to apply this to notification channels
    val isRingToneEnabled: Boolean,
    val progress: UploadNotificationStatusConfig,
    val success: UploadNotificationStatusConfig,
    val error: UploadNotificationStatusConfig,
    val cancelled: UploadNotificationStatusConfig
) : Parcelable
