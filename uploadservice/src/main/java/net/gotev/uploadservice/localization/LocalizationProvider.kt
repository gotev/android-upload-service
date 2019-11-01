package net.gotev.uploadservice.localization

import net.gotev.uploadservice.Placeholders
import net.gotev.uploadservice.data.UploadElapsedTime
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadRate

abstract class LocalizationProvider {
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
            .replace(Placeholders.ELAPSED_TIME, uploadElapsedTime(uploadInfo.elapsedTime))
            .replace(Placeholders.PROGRESS, uploadProgress(uploadInfo.progressPercent))
            .replace(Placeholders.UPLOAD_RATE, uploadRate(uploadInfo.uploadRate))
            .replace(Placeholders.UPLOADED_FILES, uploadedFiles(uploadedFiles))
            .replace(Placeholders.REMAINING_FILES, remainingFiles(remainingFiles))
            .replace(Placeholders.TOTAL_FILES, totalFiles(totalFiles))
    }
}
