package net.gotev.uploadservice.ftp

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import net.gotev.uploadservice.UploadRequest
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadFile

/**
 * Creates a new FTP Upload Request.
 * @param context application context
 * @param serverUrl server IP address or hostname
 * @param port FTP port
 */
class FTPUploadRequest(context: Context, serverUrl: String, port: Int) :
    UploadRequest<FTPUploadRequest>(context, serverUrl) {
    protected val ftpParams = FTPUploadTaskParameters(port)

    override val taskClass: Class<out UploadTask>
        get() = FTPUploadTask::class.java

    init {
        require(port > 0) { "Specify valid FTP port!" }
    }

    override fun getAdditionalParameters() = ftpParams.toPersistableData()

    /**
     * Set the credentials used to login on the FTP Server.
     * @param username account username
     * @param password account password
     * @return [FTPUploadRequest]
     */
    fun setUsernameAndPassword(username: String, password: String): FTPUploadRequest {
        require(username.isNotBlank()) { "Specify FTP account username!" }
        require(password.isNotBlank()) { "Specify FTP account password!" }

        ftpParams.username = username
        ftpParams.password = password
        return this
    }

    /**
     * Add a file to be uploaded.
     * @param filePath path to the local file on the device
     * @param remotePath if null, The uploaded file name will be the same as the local file name, so
     * if you are uploading `/path/to/myfile.txt`, you will have `myfile.txt`
     * inside the default remote working directory.
     *
     * If not null, it's the absolute path (or relative path to the default remote working directory)
     * of the file on the FTP server. Valid paths are for example:
     * `/path/to/myfile.txt`, `relative/path/` or `myfile.zip`.
     * If any of the directories of the specified remote path does not exist,
     * they will be automatically created. You can also set with which permissions
     * to create them by using
     * [FTPUploadRequest.setCreatedDirectoriesPermissions]
     * method.
     * <br></br><br></br>
     * Remember that if the remote path ends with `/`, the remote file name
     * will be the same as the local file, so for example if I'm uploading
     * `/home/alex/photos.zip` into `images/` remote path, I will have
     * `photos.zip` into the remote `images/` directory.
     * <br></br><br></br>
     * If the remote path does not end with `/`, the last path segment
     * will be used as the remote file name, so for example if I'm uploading
     * `/home/alex/photos.zip` into `images/vacations.zip`, I will
     * have `vacations.zip` into the remote `images/` directory.
     * @param permissions UNIX permissions for the uploaded file
     * @return [FTPUploadRequest]
     * @throws FileNotFoundException if the local file does not exist
     */
    @Throws(FileNotFoundException::class)
    @JvmOverloads
    fun addFileToUpload(
        filePath: String,
        remotePath: String? = null,
        permissions: UnixPermissions? = null
    ): FTPUploadRequest {
        files.add(
            UploadFile(filePath).apply {
                this.remotePath = if (remotePath.isNullOrBlank()) {
                    File(filePath).name
                } else {
                    remotePath
                }

                this.permissions = permissions?.toString()
            }
        )
        return this
    }

    /**
     * Sets the FTP connection timeout.
     * The default value is defined in [FTPUploadTaskParameters.DEFAULT_CONNECT_TIMEOUT].
     * @param milliseconds timeout in milliseconds
     * @return [FTPUploadRequest]
     */
    fun setConnectTimeout(milliseconds: Int): FTPUploadRequest {
        require(milliseconds >= 2000) { "Set at least 2000ms connect timeout!" }

        ftpParams.connectTimeout = milliseconds
        return this
    }

    /**
     * Sets FTP socket timeout. This affects login, logout and change working directory timeout.
     * The default value is defined in [FTPUploadTaskParameters.DEFAULT_SOCKET_TIMEOUT].
     * @param milliseconds timeout in milliseconds
     * @return [FTPUploadRequest]
     */
    fun setSocketTimeout(milliseconds: Int): FTPUploadRequest {
        require(milliseconds >= 2000) { "Set at least 2000ms socket timeout!" }

        ftpParams.socketTimeout = milliseconds
        return this
    }

    /**
     * Sets if the compressed file transfer mode should be used. If your server supports it, this
     * will allow you to use less bandwidth to transfer files, however some additional processing
     * has to be made on your device. By default compressed file transfer mode is disabled and if
     * enabled it works only if it's both supported and enabled on your FTP server.
     * @param value true to enable compressed file transfer mode, false to disable it and use the
     * default streaming mode
     * @return [FTPUploadRequest]
     */
    fun useCompressedFileTransferMode(value: Boolean): FTPUploadRequest {
        ftpParams.compressedFileTransfer = value
        return this
    }

    /**
     * Sets the UNIX permissions to set to newly created directories (if any). This may happen if
     * you upload files to directories which does not exist on your FTP server. They will be
     * automatically created. If you never call this method,
     * the default permissions for new folders set on your FTP server will be applied.
     * @param permissions UNIX permissions to set to newly created directories
     * @return [FTPUploadRequest]
     */
    fun setCreatedDirectoriesPermissions(permissions: UnixPermissions): FTPUploadRequest {
        ftpParams.createdDirectoriesPermissions = permissions.toString()
        return this
    }

    /**
     * Enables or disables FTP over SSL processing (FTPS). By default SSL is disabled.
     * @param useSSL true to enable SSL, false to disable it
     * @return [FTPUploadRequest]
     */
    fun useSSL(useSSL: Boolean): FTPUploadRequest {
        ftpParams.useSSL = useSSL
        return this
    }

    /**
     * Sets FTPS security mode. By default the security mode is explicit. This flag is used
     * only if [FTPUploadRequest.useSSL] is set to true.
     * @see [FTPS Security modes](https://en.wikipedia.org/wiki/FTPS.Methods_of_invoking_security)
     *
     * @param isImplicit true sets security mode to implicit, false sets it to explicit.
     * @return [FTPUploadRequest]
     */
    fun setSecurityModeImplicit(isImplicit: Boolean): FTPUploadRequest {
        ftpParams.implicitSecurity = isImplicit
        return this
    }

    /**
     * Sets the secure socket protocol to use when [FTPUploadRequest.useSSL]
     * is set to true. The default protocol is TLS. Supported protocols are SSL and TLS.
     * @param protocol TLS or SSL (TLS is the default)
     * @return [FTPUploadRequest]
     */
    fun setSecureSocketProtocol(protocol: String): FTPUploadRequest {
        ftpParams.secureSocketProtocol = protocol
        return this
    }

    override fun startUpload(): String {
        require(files.isNotEmpty()) { "Add at least one file to start FTP upload!" }

        return super.startUpload()
    }
}
