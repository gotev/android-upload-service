package net.gotev.uploadservice.placeholders

import net.gotev.uploadservice.data.UploadElapsedTime
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadRate

abstract class PlaceholdersProcessor {
    abstract fun uploadElapsedTime(uploadElapsedTime: UploadElapsedTime): String
    abstract fun uploadRate(uploadRate: UploadRate): String
    abstract fun uploadProgress(percent: Int): String
    abstract fun uploadedFiles(uploadedFiles: Int): String
    abstract fun remainingFiles(remainingFiles: Int): String
    abstract fun totalFiles(totalFiles: Int): String

    /**
     * Replace placeholders in a message string.
     * @param message string in which to replace placeholders
     * @param uploadInfo upload information data
     * @return string with replaced placeholders
     */
    open fun processPlaceholders(message: String?, uploadInfo: UploadInfo): String {
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
