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

    /**
     * Gets upload task's elapsed time in milliseconds.
     * @return long value
     */
    @IgnoredOnParcel
    val elapsedTime: MinutesSeconds
        get() {
            var seconds = ((Date().time - startTime) / 1000).toInt()
            val minutes = seconds / 60
            seconds -= 60 * minutes

            return MinutesSeconds(minutes, seconds)
        }

    /**
     * Gets the average upload rate in Kb/s (Kilo bit per second).
     * @return upload rate
     */
    @IgnoredOnParcel
    val uploadRate: UploadRate
        get() {
            val elapsedSeconds = elapsedTime.totalSeconds

            // wait at least a second to stabilize the upload rate a little bit
            val kiloBitSecond = if (elapsedSeconds < 1)
                0.0
            else
                uploadedBytes.toDouble() / 1024 * 8 / elapsedSeconds

            return when {
                kiloBitSecond < 1 -> UploadRate(
                    value = (kiloBitSecond * 1000).toInt(),
                    unit = UploadRate.UploadRateUnit.bitSecond
                )

                kiloBitSecond > 1024 -> UploadRate(
                    value = (kiloBitSecond / 1024).toInt(),
                    unit = UploadRate.UploadRateUnit.megaBitSecond
                )

                else -> UploadRate(
                    value = kiloBitSecond.toInt(),
                    unit = UploadRate.UploadRateUnit.kiloBitSecond
                )
            }
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
