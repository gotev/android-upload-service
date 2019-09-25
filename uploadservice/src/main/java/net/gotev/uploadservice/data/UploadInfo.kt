package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Contains upload information and statistics.
 * @author Aleksandar Gotev
 */
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
     * Gets the elapsed time as a string, expressed in seconds if the value is `< 60`,
     * or expressed in minutes and seconds if the value is `>=` 60.
     * @return string representation of the elapsed time
     */
    @IgnoredOnParcel
    val elapsedTimeString: String
        get() {
            var elapsedSeconds = (elapsedTime / 1000).toInt()

            if (elapsedSeconds == 0) return "0 sec"

            val minutes = elapsedSeconds / 60
            elapsedSeconds -= 60 * minutes

            return if (minutes == 0) {
                "$elapsedSeconds sec"
            } else {
                "$minutes min $elapsedSeconds sec"
            }

        }

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
     * Returns a string representation of the upload rate, expressed in the most convenient unit of
     * measurement (Mbit/s if the value is `>=` 1024, B/s if the value is `< 1`, otherwise Kbit/s)
     * @return string representation of the upload rate (e.g. 234 Kbit/s)
     */
    @IgnoredOnParcel
    val uploadRateString: String
        get() {
            if (uploadRate < 1) {
                return "${(uploadRate * 1000).toInt()} bit/s"
            }

            if (uploadRate >= 1024) {
                return "${(uploadRate / 1024).toInt()} Mb/s"
            }

            return "${uploadRate.toInt()} Kb/s"
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
