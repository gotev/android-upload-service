package net.gotev.uploadservice.schemehandlers

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import net.gotev.uploadservice.extensions.APPLICATION_OCTET_STREAM
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.logger.UploadServiceLogger.NA
import java.io.File
import java.io.IOException

internal class ContentResolverSchemeHandler : SchemeHandler {

    private lateinit var uri: Uri

    override fun init(path: String) {
        uri = Uri.parse(path)
    }

    override fun size(context: Context): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndex(OpenableColumns.SIZE))
            } else {
                null
            }
        } ?: run {
            UploadServiceLogger.error(javaClass.simpleName, NA) { "no cursor data for $uri, returning size 0" }
            // TODO: investigate what happens when size is 0
            0L
        }
    }

    override fun stream(context: Context) = context.contentResolver.openInputStream(uri)
        ?: throw IOException("can't open input stream for $uri")

    override fun contentType(context: Context): String {
        val type = context.contentResolver.getType(uri)

        return if (type.isNullOrBlank()) {
            APPLICATION_OCTET_STREAM
        } else {
            type
        }
    }

    override fun name(context: Context): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        } ?: uri.toString().split(File.separator).last()
    }

    override fun delete(context: Context) = try {
        context.contentResolver.delete(uri, null, null) > 0
    } catch (exc: Throwable) {
        UploadServiceLogger.error(javaClass.simpleName, NA, exc) { "File deletion error" }
        false
    }
}
