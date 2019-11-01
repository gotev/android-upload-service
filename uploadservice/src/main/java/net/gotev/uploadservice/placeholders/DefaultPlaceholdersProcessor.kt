package net.gotev.uploadservice.placeholders

import net.gotev.uploadservice.data.UploadElapsedTime
import net.gotev.uploadservice.data.UploadRate

open class DefaultPlaceholdersProcessor : PlaceholdersProcessor() {
    override fun uploadElapsedTime(uploadElapsedTime: UploadElapsedTime) = when {
        uploadElapsedTime.minutes == 0 -> "${uploadElapsedTime.seconds} sec"
        else -> "${uploadElapsedTime.minutes} min ${uploadElapsedTime.seconds} sec"
    }

    override fun uploadRate(uploadRate: UploadRate): String {
        val suffix = when (uploadRate.unit) {
            UploadRate.UploadRateUnit.bitPerSecond -> "b/s"
            UploadRate.UploadRateUnit.kiloBitPerSecond -> "kb/s"
            UploadRate.UploadRateUnit.megaBitPerSecond -> "Mb/s"
        }

        return "${uploadRate.value} $suffix"
    }

    override fun uploadProgress(percent: Int) = "$percent %"

    override fun uploadedFiles(uploadedFiles: Int) = "$uploadedFiles"

    override fun remainingFiles(remainingFiles: Int) = "$remainingFiles"

    override fun totalFiles(totalFiles: Int) = "$totalFiles"
}
