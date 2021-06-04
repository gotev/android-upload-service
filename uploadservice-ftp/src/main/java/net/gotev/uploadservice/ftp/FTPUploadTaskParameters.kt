package net.gotev.uploadservice.ftp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData

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
) : Parcelable, Persistable {
    companion object : Persistable.Creator<FTPUploadTaskParameters> {
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

        private object CodingKeys {
            const val port = "port"
            const val username = "user"
            const val password = "pwd"
            const val connectTimeout = "cTimeout"
            const val socketTimeout = "soTimeout"
            const val compressed = "compressed"
            const val permissions = "perms"
            const val ssl = "ssl"
            const val implicit = "implicit"
            const val sslProtocol = "sslProto"
        }

        override fun createFromPersistableData(data: PersistableData) = FTPUploadTaskParameters(
            port = data.getInt(CodingKeys.port),
            username = data.getString(CodingKeys.username),
            password = data.getString(CodingKeys.password),
            connectTimeout = data.getInt(CodingKeys.connectTimeout),
            socketTimeout = data.getInt(CodingKeys.socketTimeout),
            compressedFileTransfer = data.getBoolean(CodingKeys.compressed),
            createdDirectoriesPermissions = try {
                data.getString(CodingKeys.permissions)
            } catch (exc: Throwable) {
                null
            },
            useSSL = data.getBoolean(CodingKeys.ssl),
            implicitSecurity = data.getBoolean(CodingKeys.implicit),
            secureSocketProtocol = data.getString(CodingKeys.sslProtocol)
        )
    }

    override fun toPersistableData() = PersistableData().apply {
        putInt(CodingKeys.port, port)
        putString(CodingKeys.username, username)
        putString(CodingKeys.password, password)
        putInt(CodingKeys.connectTimeout, connectTimeout)
        putInt(CodingKeys.socketTimeout, socketTimeout)
        putBoolean(CodingKeys.compressed, compressedFileTransfer)
        createdDirectoriesPermissions?.let { putString(CodingKeys.permissions, it) }
        putBoolean(CodingKeys.ssl, useSSL)
        putBoolean(CodingKeys.implicit, implicitSecurity)
        putString(CodingKeys.sslProtocol, secureSocketProtocol)
    }
}
