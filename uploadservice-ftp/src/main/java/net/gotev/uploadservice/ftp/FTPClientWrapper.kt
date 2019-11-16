package net.gotev.uploadservice.ftp

import android.content.Context
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.logger.UploadServiceLogger
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.io.CopyStreamEvent
import org.apache.commons.net.io.CopyStreamListener
import java.io.Closeable
import java.io.IOException

class FTPClientWrapper(
    private val uploadId: String,
    observer: Observer,
    connectTimeout: Int,
    useSSL: Boolean = false,
    sslProtocol: String = "TLS",
    implicitSecurity: Boolean = false
) : Closeable {

    interface Observer {
        fun onTransfer(
            client: FTPClientWrapper,
            totalBytesTransferred: Long,
            bytesTransferred: Int,
            streamSize: Long
        )
    }

    private val streamListener = object : CopyStreamListener {
        override fun bytesTransferred(event: CopyStreamEvent?) {
        }

        override fun bytesTransferred(
            totalBytesTransferred: Long,
            bytesTransferred: Int,
            streamSize: Long
        ) {
            observer.onTransfer(
                this@FTPClientWrapper,
                totalBytesTransferred,
                bytesTransferred,
                streamSize
            )
        }
    }

    private val ftpClient: FTPClient = if (!useSSL) {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) { "Creating plain FTP client" }
        FTPClient()
    } else {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Creating FTP over SSL (FTPS) client with $sslProtocol protocol and " +
                if (implicitSecurity)
                    "implicit security"
                else
                    "explicit security"
        }

        FTPSClient(sslProtocol, implicitSecurity)
    }.apply {
        bufferSize = UploadServiceConfig.bufferSizeBytes
        copyStreamListener = streamListener
        defaultTimeout = connectTimeout
        this.connectTimeout = connectTimeout
        autodetectUTF8 = true
    }

    private fun internalConnect(server: String, port: Int) {
        ftpClient.connect(server, port)

        if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
            throw IOException("Can't connect to $server:$port. Response: ${ftpClient.replyString}")
        }

        // If FTPS, perform https://tools.ietf.org/html/rfc4217#page-17
        (ftpClient as? FTPSClient)?.apply {
            execPBSZ(0)
            execPROT("P")
        }
    }

    private fun internalLogin(server: String, port: Int, username: String, password: String) {
        if (!ftpClient.login(username, password)) {
            throw IOException(
                "Login error on $server:$port with username: $username. " +
                    "Check your credentials and try again."
            )
        }
    }

    private fun setupKeepAlive(socketTimeout: Int) {
        // to prevent the socket timeout on the control socket during file transfer,
        // set the control keep alive timeout to a half of the socket timeout
        val controlKeepAliveTimeout = socketTimeout / 2 / 1000

        ftpClient.apply {
            soTimeout = socketTimeout
            this.controlKeepAliveTimeout = controlKeepAliveTimeout.toLong()
            controlKeepAliveReplyTimeout = controlKeepAliveTimeout * 1000
        }

        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Socket timeout set to ${socketTimeout}ms. " +
                "Enabled control keep alive every ${controlKeepAliveTimeout}s"
        }
    }

    private fun setupConnection(compressedFileTransfer: Boolean) {
        ftpClient.apply {
            enterLocalPassiveMode()
            setFileType(FTP.BINARY_FILE_TYPE)
            setFileTransferMode(
                if (compressedFileTransfer)
                    FTP.COMPRESSED_TRANSFER_MODE
                else
                    FTP.STREAM_TRANSFER_MODE
            )
        }
    }

    fun connect(
        server: String,
        port: Int,
        username: String,
        password: String,
        socketTimeout: Int,
        compressedFileTransfer: Boolean
    ) {
        internalConnect(server, port)
        internalLogin(server, port, username, password)
        setupKeepAlive(socketTimeout)
        setupConnection(compressedFileTransfer)

        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Successfully connected to $server:$port as $username"
        }
    }

    val currentWorkingDirectory: String
        get() = ftpClient.printWorkingDirectory()

    fun setPermission(remoteFileName: String, permissions: String): Boolean {
        if (remoteFileName.isBlank() || permissions.isBlank())
            return false

        // http://stackoverflow.com/questions/12741938/how-can-i-change-permissions-of-a-file-on-a-ftp-server-using-apache-commons-net
        try {
            val success = ftpClient.sendSiteCommand("chmod $permissions $remoteFileName")

            if (success) {
                UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
                    "Permissions for: $remoteFileName set to: $permissions"
                }
            } else {
                UploadServiceLogger.error(javaClass.simpleName, uploadId) {
                    "Error while setting permissions for $remoteFileName to: $permissions. " +
                        "Check if your FTP user can set file permissions!"
                }
            }
            return success
        } catch (exc: Throwable) {
            UploadServiceLogger.error(javaClass.simpleName, uploadId, exc) {
                "Error while setting permissions for $remoteFileName to: $permissions. " +
                    "Check if your FTP user can set file permissions!"
            }
            return false
        }
    }

    /**
     * Creates a nested directory structure on a FTP server and enters into it.
     * @param dirPath Path of the directory, i.e /projects/java/ftp/demo
     * @param permissions UNIX permissions to apply to created directories. If null, the FTP
     * server defaults will be applied, because no UNIX permissions will be
     * explicitly set
     * @throws IOException if any error occurred during client-server communication
     */
    @Throws(IOException::class)
    fun makeDirectories(dirPath: String, permissions: String? = null) {
        if (!dirPath.contains("/")) return

        val pathElements =
            dirPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (pathElements.size == 1) return

        // if the string ends with / it means that the dir path contains only directories,
        // otherwise if it does not contain /, the last element of the path is the file name,
        // so it must be ignored when creating the directory structure
        val lastElement = if (dirPath.endsWith("/"))
            pathElements.size
        else
            pathElements.size - 1

        for (i in 0 until lastElement) {
            val singleDir = pathElements[i]
            if (singleDir.isEmpty()) continue

            if (!ftpClient.changeWorkingDirectory(singleDir)) {
                if (ftpClient.makeDirectory(singleDir)) {
                    UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
                        "Created remote directory: $singleDir"
                    }
                    permissions?.let { setPermission(singleDir, it) }
                    ftpClient.changeWorkingDirectory(singleDir)
                } else {
                    throw IOException("Unable to create remote directory: $singleDir")
                }
            }
        }
    }

    @Throws(IOException::class)
    fun uploadFile(
        context: Context,
        baseWorkingDir: String,
        file: UploadFile,
        permissions: String? = null
    ) {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Starting FTP upload of: ${file.handler.name(context)} to: ${file.remotePath}"
        }

        var remoteDestination = file.remotePath ?: run {
            UploadServiceLogger.error(javaClass.simpleName, uploadId) {
                "Skipping ${file.path} because no remote path has been defined"
            }
            return
        }

        if (remoteDestination.startsWith(baseWorkingDir)) {
            remoteDestination = remoteDestination.substring(baseWorkingDir.length)
        }

        makeDirectories(remoteDestination, permissions)

        file.handler.stream(context).use { localStream ->
            val remoteFileName = file.getRemoteFileName(context)
                ?: throw IOException("can't get remote file name for ${file.path}")

            if (!ftpClient.storeFile(remoteFileName, localStream)) {
                throw IOException(
                    "Error while uploading: ${file.handler.name(context)} " +
                        "to: ${file.remotePath}"
                )
            }

            file.permissions?.let { setPermission(remoteFileName, it) }
        }

        // get back to base working directory
        if (!ftpClient.changeWorkingDirectory(baseWorkingDir)) {
            UploadServiceLogger.error(javaClass.simpleName, uploadId) {
                "Can't change working directory to: $baseWorkingDir"
            }
        }
    }

    override fun close() {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "Closing FTP Client"
        }

        if (ftpClient.isConnected) {
            try {
                UploadServiceLogger.debug(javaClass.simpleName, uploadId) { "Logout from FTP server" }
                ftpClient.logout()
            } catch (exc: Throwable) {
                UploadServiceLogger.error(javaClass.simpleName, uploadId, exc) {
                    "Error while closing FTP connection"
                }
            }

            try {
                UploadServiceLogger.debug(javaClass.simpleName, uploadId) { "Disconnect from FTP server" }
                ftpClient.disconnect()
            } catch (exc: Throwable) {
                UploadServiceLogger.error(javaClass.simpleName, uploadId, exc) {
                    "Error while disconnecting from FTP connection"
                }
            }
        }
    }
}
