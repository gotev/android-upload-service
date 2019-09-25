package net.gotev.uploadservice.schemehandlers

import android.content.Context
import java.io.InputStream

interface SchemeHandler {
    /**
     * Initialize instance with file path.
     */
    fun init(path: String)

    /**
     * Gets file size in bytes.
     */
    fun size(context: Context): Long

    /**
     * Gets file input stream to read it.
     */
    fun stream(context: Context): InputStream

    /**
     * Gets file content type.
     */
    fun contentType(context: Context): String

    /**
     * Gets file name.
     */
    fun name(context: Context): String

    /**
     * Deletes the file.
     */
    fun delete(context: Context): Boolean
}
