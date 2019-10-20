package net.gotev.uploadservice.ftp

import android.content.Context
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.extensions.setOrRemove

// properties associated to each file
private const val PROPERTY_REMOTE_PATH = "ftpRemotePath"
private const val PROPERTY_PERMISSIONS = "ftpPermissions"

internal var UploadFile.remotePath: String?
    get() = properties[PROPERTY_REMOTE_PATH]
    set(value) {
        properties.setOrRemove(PROPERTY_REMOTE_PATH, value)
    }

internal var UploadFile.permissions: String?
    get() = properties[PROPERTY_PERMISSIONS]
    set(value) {
        properties.setOrRemove(PROPERTY_PERMISSIONS, value)
    }

/**
 * Checks if the remote file path contains also the remote file name. If it's not specified,
 * the name of the local file will be used.
 * @param file file to upload
 * @return remote file name
 */
internal fun UploadFile.getRemoteFileName(context: Context): String? {
    val remotePath = remotePath ?: return null

    // if the remote path ends with /
    // it means that the remote path specifies only the directory structure, so
    // get the remote file name from the local file
    if (remotePath.endsWith("/")) {
        return handler.name(context)
    }

    // if the remote path contains /, but it's not the last character
    // it means that I have something like: /path/to/myfilename
    // so the remote file name is the last path element (myfilename in this example)
    if (remotePath.contains("/")) {
        // TODO check if this is correct
        val tmp = remotePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return tmp[tmp.size - 1]
    }

    // if the remote path does not contain /, it means that it specifies only
    // the remote file name
    return remotePath
}
