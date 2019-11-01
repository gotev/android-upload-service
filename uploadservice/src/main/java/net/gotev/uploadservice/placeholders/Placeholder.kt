package net.gotev.uploadservice.placeholders

enum class Placeholder(val value: String) {
    /**
     * Placeholder to display the total elapsed upload time in minutes and seconds.
     * E.g.: 34s, 4m 33s, 45m 21s
     */
    ElapsedTime("[[ELAPSED_TIME]]"),

    /**
     * Placeholder to display the average upload rate. E.g.: 6 Mbit/s, 634 Kbit/s, 232 bit/s
     */
    UploadRate("[[UPLOAD_RATE]]"),

    /**
     * Placeholder to display the integer progress percent from 0 to 100. E.g.: 75%
     */
    Progress("[[PROGRESS]]"),

    /**
     * Placeholder to display the number of successfully uploaded files.
     * Bear in mind that in case of HTTP/Multipart or Binary uploads which does not support
     * resume, if the request gets restarted due to an error, the number of uploaded files will
     * be reset to zero.
     */
    UploadedFiles("[[UPLOADED_FILES]]"),

    /**
     * Placeholder to display the number of remaining files to upload.
     */
    RemainingFiles("[[REMAINING_FILES]]"),

    /**
     * Placeholder to display the total number of files to upload.
     */
    TotalFiles("[[TOTAL_FILES]]");

    override fun toString(): String {
        return value
    }
}
