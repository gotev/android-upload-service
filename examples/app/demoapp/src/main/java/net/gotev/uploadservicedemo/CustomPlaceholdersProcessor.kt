package net.gotev.uploadservicedemo

import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.placeholders.DefaultPlaceholdersProcessor

class CustomPlaceholdersProcessor : DefaultPlaceholdersProcessor() {
    companion object {
        const val FILENAME_PLACEHOLDER = "[[FILENAME]]"
    }

    override fun processPlaceholders(message: String?, uploadInfo: UploadInfo): String {
        val processedMessage = super.processPlaceholders(message, uploadInfo)

        // if you have more than one file, change this accordingly to your needs
        val fileName =
            uploadInfo.files.firstOrNull()?.properties?.get("multipartRemoteFileName") ?: ""

        return processedMessage.replace(FILENAME_PLACEHOLDER, fileName)
    }
}
