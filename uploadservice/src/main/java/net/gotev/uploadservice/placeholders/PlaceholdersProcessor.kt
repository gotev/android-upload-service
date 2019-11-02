package net.gotev.uploadservice.placeholders

import net.gotev.uploadservice.data.UploadInfo

interface PlaceholdersProcessor {
    /**
     * Replace placeholders in a message string.
     * @param message string in which to replace placeholders
     * @param uploadInfo upload information data
     * @return string with replaced placeholders
     */
    fun processPlaceholders(message: String?, uploadInfo: UploadInfo): String
}
