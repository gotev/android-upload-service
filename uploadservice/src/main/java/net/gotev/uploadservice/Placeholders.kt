package net.gotev.uploadservice

import net.gotev.uploadservice.data.UploadInfo

object Placeholders {

    /**
     * Placeholder to display the total elapsed upload time in minutes and seconds.
     * E.g.: 34s, 4m 33s, 45m 21s
     */
    const val ELAPSED_TIME = "[[ELAPSED_TIME]]"

    /**
     * Placeholder to display the average upload rate. E.g.: 6 Mbit/s, 634 Kbit/s, 232 bit/s
     */
    const val UPLOAD_RATE = "[[UPLOAD_RATE]]"

    /**
     * Placeholder to display the integer progress percent from 0 to 100. E.g.: 75%
     */
    const val PROGRESS = "[[PROGRESS]]"

    /**
     * Placeholder to display the number of successfully uploaded files.
     * Bear in mind that in case of HTTP/Multipart or Binary uploads which does not support
     * resume, if the request gets restarted due to an error, the number of uploaded files will
     * be reset to zero.
     */
    const val UPLOADED_FILES = "[[UPLOADED_FILES]]"

    /**
     * Placeholder to display the total number of files to upload.
     */
    const val TOTAL_FILES = "[[TOTAL_FILES]]"

    /**
     * Replace placeholders in a string.
     * @param string string in which to replace placeholders
     * @param uploadInfo upload information data
     * @return string with replaced placeholders
     */
    fun replace(string: String?, uploadInfo: UploadInfo): String {
        val safeString = string ?: return ""

        return safeString.replace(ELAPSED_TIME, formatter.formatElapsedTime(uploadInfo.elapsedTime))
            .replace(PROGRESS, formatter.formatProgress(uploadInfo.progressPercent))
            .replace(UPLOAD_RATE, formatter.formatUploadRate(uploadInfo.uploadRate))
            .replace(UPLOADED_FILES, formatter.formatUploadedFiles(uploadInfo.successfullyUploadedFiles))
            .replace(TOTAL_FILES, formatter.formatTotalFiles(uploadInfo.files.size))
    }

    @JvmStatic
    var formatter: Formatter = DefaultFormatter()

    interface Formatter {
        /**
         * Gets the elapsed time as a string.
         * @param elapsedTime elapsed time in milliseconds
         * @return string representation of the elapsed time
         */
        fun formatElapsedTime(elapsedTime: Long): String

        /**
         * Gets the upload progress as a string.
         * @param progress upload progress in percents
         * @return string representation of the upload progress
         */
        fun formatProgress(progress: Int): String

        /**
         * Returns a string representation of the upload rate.
         * @param uploadRate upload rate in Kb/s (Kilo bit per second).
         * @return string representation of the upload rate
         */
        fun formatUploadRate(uploadRate: Double): String

        /**
         * Gets the uploaded files count as a string.
         * It can be used to pluralize strings (e.g. 1 file, 2 files).
         * @param count count of uploaded files
         * @return string representation of the uploaded files count
         */
        fun formatUploadedFiles(count: Int): String

        /**
         * Gets the total files count as a string.
         * It can be used to pluralize strings (e.g. 1 file, 2 files).
         * @param count total count of files
         * @return string representation of the total files count
         */
        fun formatTotalFiles(count: Int): String
    }

    open class DefaultFormatter : Formatter {
        /**
         * Gets the elapsed time as a string, expressed in seconds if the value is `< 60`,
         * or expressed in minutes and seconds if the value is `>=` 60.
         * @param elapsedTime The elapsed time in milliseconds
         * @return string representation of the elapsed time
         */
        override fun formatElapsedTime(elapsedTime: Long): String {
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

        override fun formatProgress(progress: Int): String {
            return "$progress%"
        }

        /**
         * Returns a string representation of the upload rate, expressed in the most convenient unit of
         * measurement (Mbit/s if the value is `>=` 1024, B/s if the value is `< 1`, otherwise Kbit/s).
         * @param uploadRate upload rate in Kb/s (Kilo bit per second).
         * @return string representation of the upload rate (e.g. 234 Kbit/s)
         */
        override fun formatUploadRate(uploadRate: Double): String {
            if (uploadRate < 1) {
                return "${(uploadRate * 1000).toInt()} bit/s"
            }

            if (uploadRate >= 1024) {
                return "${(uploadRate / 1024).toInt()} Mb/s"
            }

            return "${uploadRate.toInt()} Kb/s"
        }

        override fun formatUploadedFiles(count: Int): String {
            return count.toString()
        }

        override fun formatTotalFiles(count: Int): String {
            return count.toString()
        }
    }
}
