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

        return safeString.replace(ELAPSED_TIME, uploadInfo.elapsedTimeString)
                .replace(PROGRESS, "${uploadInfo.progressPercent}%")
                .replace(UPLOAD_RATE, uploadInfo.uploadRateString)
                .replace(UPLOADED_FILES, uploadInfo.successfullyUploadedFiles.toString())
                .replace(TOTAL_FILES, uploadInfo.files.size.toString())
    }
}
