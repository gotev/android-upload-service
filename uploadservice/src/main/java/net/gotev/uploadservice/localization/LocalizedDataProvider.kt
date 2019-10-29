package net.gotev.uploadservice.localization

import net.gotev.uploadservice.data.MinutesSeconds
import net.gotev.uploadservice.data.UploadRate

interface LocalizedDataProvider {
    fun minutesSeconds(minutesSeconds: MinutesSeconds): String
    fun uploadDate(uploadRate: UploadRate): String
    fun percent(percent: Int): String
    fun successfullyUploadedFiles(uploadedFiles: Int): String
    fun totalFiles(totalFiles: Int): String
}
