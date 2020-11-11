package net.gotev.uploadservice.ftp

import org.junit.Assert.assertEquals
import org.junit.Test

class DataTypesPersistableDataTests {
    @Test
    fun ftpParams() {
        val params = FTPUploadTaskParameters(
            port = 123,
            username = "hey",
            password = "ho",
            connectTimeout = 10,
            socketTimeout = 15,
            compressedFileTransfer = true,
            createdDirectoriesPermissions = null,
            useSSL = true,
            implicitSecurity = false,
            secureSocketProtocol = "someProto"
        )

        val data = params.toPersistableData()

        assertEquals(params, FTPUploadTaskParameters.createFromPersistableData(data))
    }

    @Test
    fun ftpParams2() {
        val params = FTPUploadTaskParameters(
            port = 123,
            username = "hey",
            password = "ho",
            connectTimeout = 10,
            socketTimeout = 15,
            compressedFileTransfer = true,
            createdDirectoriesPermissions = "775",
            useSSL = true,
            implicitSecurity = false,
            secureSocketProtocol = "someProto"
        )

        val data = params.toPersistableData()

        assertEquals(params, FTPUploadTaskParameters.createFromPersistableData(data))
    }
}
