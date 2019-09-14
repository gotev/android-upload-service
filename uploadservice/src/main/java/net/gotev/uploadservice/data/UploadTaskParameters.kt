package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.UploadNotificationConfig

/**
 * Class which contains all the basic parameters passed to the upload task.
 * If you want to add more parameters, which are specific to your implementation, you should not
 * extends this class, but instead create a new class which implements [Parcelable] and
 * define a constant string which indicates the key used to store the serialized form of the class
 * into the intent. Look at [HttpUploadTaskParameters] for an example.
 *
 * @author gotev (Aleksandar Gotev)
 */
@Parcelize
data class UploadTaskParameters(
        val id: String,
        val serverUrl: String,
        val maxRetries: Int,
        val autoDeleteSuccessfullyUploadedFiles: Boolean,
        val notificationConfig: UploadNotificationConfig?,
        val files: ArrayList<UploadFile>,
        val additionalParams: Parcelable? = null
) : Parcelable
