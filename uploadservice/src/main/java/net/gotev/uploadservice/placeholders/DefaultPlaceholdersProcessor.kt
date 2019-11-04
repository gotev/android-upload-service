package net.gotev.uploadservice.placeholders

import net.gotev.uploadservice.data.UploadElapsedTime
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadRate

open class DefaultPlaceholdersProcessor : PlaceholdersProcessor {
    open fun uploadElapsedTime(uploadElapsedTime: UploadElapsedTime) = when {
        uploadElapsedTime.minutes == 0 -> "${uploadElapsedTime.seconds} sec"
        else -> "${uploadElapsedTime.minutes} min ${uploadElapsedTime.seconds} sec"
    }

    open fun uploadRate(uploadRate: UploadRate): String {
        val suffix = when (uploadRate.unit) {
            UploadRate.UploadRateUnit.BitPerSecond -> "b/s"
            UploadRate.UploadRateUnit.KilobitPerSecond -> "kb/s"
            UploadRate.UploadRateUnit.MegabitPerSecond -> "Mb/s"
        }

        return "${uploadRate.value} $suffix"
    }

    open fun uploadProgress(percent: Int) = "$percent %"

    open fun uploadedFiles(uploadedFiles: Int) = "$uploadedFiles"

    open fun remainingFiles(remainingFiles: Int) = "$remainingFiles"

    open fun totalFiles(totalFiles: Int) = "$totalFiles"

    override fun processPlaceholders(message: String?, uploadInfo: UploadInfo): String {
        val safeMessage = message ?: return ""

        val uploadedFiles = uploadInfo.successfullyUploadedFiles
        val totalFiles = uploadInfo.files.size
        val remainingFiles = totalFiles - uploadedFiles

        return safeMessage
            .replace(Placeholder.ElapsedTime.value, uploadElapsedTime(uploadInfo.elapsedTime))
            .replace(Placeholder.UploadRate.value, uploadRate(uploadInfo.uploadRate))
            .replace(Placeholder.Progress.value, uploadProgress(uploadInfo.progressPercent))
            .replace(Placeholder.UploadedFiles.value, uploadedFiles(uploadedFiles))
            .replace(Placeholder.RemainingFiles.value, remainingFiles(remainingFiles))
            .replace(Placeholder.TotalFiles.value, totalFiles(totalFiles))
    }
}
