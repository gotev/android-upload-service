package net.gotev.uploadservice.data

import android.os.Parcelable
import java.util.ArrayList
import java.util.Date
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UploadInfo @JvmOverloads constructor(
    /**
     * Upload unique ID.
     */
    val uploadId: String,

    /**
     * Upload task's start timestamp in milliseconds.
     */
    val startTime: Long = 0,

    /**
     * Bytes upload so far.
     */
    val uploadedBytes: Long = 0,

    /**
     * Upload task total bytes.
     */
    val totalBytes: Long = 0,

    /**
     * Number of retries that has been made during the upload process.
     * If no retries has been made, this value will be zero.
     */
    val numberOfRetries: Int = 0,

    /**
     * List of all the files present in this upload.
     */
    val files: ArrayList<UploadFile> = ArrayList()
) : Parcelable {

    @IgnoredOnParcel
    private val currentTime: Long = Date().time

    /**
     * Gets upload task's elapsed time in milliseconds.
     * @return long value
     */
    @IgnoredOnParcel
    val elapsedTime: Long
        get() = currentTime - startTime

    /**
     * Gets the average upload rate in Kb/s (Kilo bit per second).
     * @return upload rate
     */
    @IgnoredOnParcel
    val uploadRate: Double
        get() {
            // wait at least a second to stabilize the upload rate a little bit
            if (elapsedTime < 1000) return 0.0

            return uploadedBytes.toDouble() / 1024 * 8 / (elapsedTime / 1000)
        }

    /**
     * Gets the upload progress in percent (from 0 to 100).
     * @return integer value
     */
    @IgnoredOnParcel
    val progressPercent: Int
        get() = if (totalBytes == 0L) 0 else (uploadedBytes * 100 / totalBytes).toInt()

    @IgnoredOnParcel
    val successfullyUploadedFiles: Int
        get() = files.count { it.successfullyUploaded }
}
