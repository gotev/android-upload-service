package net.gotev.uploadservice.ftp

import android.content.Context
import android.os.Parcel
import androidx.test.platform.app.InstrumentationRegistry
import net.gotev.uploadservice.CreateUploadRequest
import net.gotev.uploadservice.persistence.PersistableData
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateUploadRequestTests {

    private fun assertRecreatedUploadRequestIsEqualTo(context: Context, data: PersistableData) {
        val persistedData = CreateUploadRequest.fromPersistableData(context, data)
            .toPersistableData()

        val persistedJson = CreateUploadRequest.fromJson(context, data.toJson())
            .toPersistableData()

        val parcel = Parcel.obtain().apply {
            data.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val persistedParcel = CreateUploadRequest.fromParcel(context, parcel)
            .toPersistableData()

        assertEquals(data, persistedData)
        assertEquals(data, persistedJson)
        assertEquals(data, persistedParcel)
    }

    @Test
    fun ftpUploadRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val ftpData = FTPUploadRequest(context, "https:://my.server.url", 1234)
            .addFileToUpload(
                filePath = "/path/to/file",
                remotePath = "/remote/path",
                permissions = UnixPermissions("644")
            )
            .setConnectTimeout(2000)
            .setSocketTimeout(10000)
            .setCreatedDirectoriesPermissions(UnixPermissions("777"))
            .useSSL(true)
            .setSecureSocketProtocol("TLS")
            .setUsernameAndPassword(username = "user", password = "pass")
            .setSecurityModeImplicit(true)
            .useCompressedFileTransferMode(true)
            .setMaxRetries(2)
            .toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, ftpData)
    }

    @Test
    fun ftpUploadRequest2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val ftpData = FTPUploadRequest(context, "https:://my.server.url", 1234)
            .addFileToUpload(
                filePath = "/path/to/file",
                remotePath = "/remote/path",
                permissions = UnixPermissions("644")
            )
            .addFileToUpload(
                filePath = "/path/to/file/2/",
                remotePath = "/remote/path/2/",
                permissions = UnixPermissions("777")
            )
            .toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, ftpData)
    }

    @Test
    fun ftpUploadRequest3() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val ftpData = FTPUploadRequest(context, "https:://my.server.url", 1234)
            .addFileToUpload(
                filePath = "/path/to/file",
                remotePath = "/remote/path",
                permissions = UnixPermissions("644")
            )
            .toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, ftpData)
    }
}
