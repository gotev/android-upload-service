package net.gotev.uploadservice.schemehandlers

import android.content.Context
import java.io.InputStream

/**
 * Scheme handlers allows to get files from different sources (e.g. file system, content resolver)
 * by decoupling common characteristics from their implementations
 * @author stephentuso
 * @author gotev
 */
interface SchemeHandler {
    fun init(path: String)
    fun fileLength(context: Context): Long
    fun stream(context: Context): InputStream
    fun contentType(context: Context): String
    fun fileName(context: Context): String
}
