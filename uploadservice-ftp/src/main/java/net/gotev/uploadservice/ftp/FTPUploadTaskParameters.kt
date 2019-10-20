package net.gotev.uploadservice.ftp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * FTP upload parameters.
 * @author Aleksandar Gotev
 */
@Parcelize
data class FTPUploadTaskParameters(
    var port: Int,
    var username: String = "anonymous",
    var password: String = "",
    var connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
    var socketTimeout: Int = DEFAULT_SOCKET_TIMEOUT,
    @get:JvmName("isCompressedFileTransfer")
    var compressedFileTransfer: Boolean = false,
    var createdDirectoriesPermissions: String? = null,
    var useSSL: Boolean = false,
    @get:JvmName("isImplicitSecurity")
    var implicitSecurity: Boolean = false,
    var secureSocketProtocol: String = DEFAULT_SECURE_SOCKET_PROTOCOL
) : Parcelable {
    companion object {
        /**
         * The default FTP connection timeout in milliseconds.
         */
        const val DEFAULT_CONNECT_TIMEOUT = 15000

        /**
         * The default FTP socket timeout in milliseconds.
         */
        const val DEFAULT_SOCKET_TIMEOUT = 30000

        /**
         * The default protocol to use when FTP over SSL is enabled (FTPS).
         */
        const val DEFAULT_SECURE_SOCKET_PROTOCOL = "TLS"
    }
}
