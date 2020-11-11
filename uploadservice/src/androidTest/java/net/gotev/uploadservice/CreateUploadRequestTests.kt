package net.gotev.uploadservice

import android.content.Context
import android.os.Parcel
import androidx.test.platform.app.InstrumentationRegistry
import net.gotev.uploadservice.persistence.PersistableData
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
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
    fun multipartUploadRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val multipartUploadData = MultipartUploadRequest(context, "https://my.server.com")
            .addFileToUpload(
                filePath = "/path/to/file",
                parameterName = "file$",
                fileName = "testing$",
                contentType = "application/octet-stream"
            ).addHeader(
                headerName = "myHeader$",
                headerValue = "myHeaderValue$"
            ).addParameter(
                paramName = "myParam$",
                paramValue = "myParamValue$"
            ).toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, multipartUploadData)
    }

    @Test
    fun multipartUploadRequest2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val multipartUploadData = MultipartUploadRequest(context, "https://my.server.com")
            .addFileToUpload(
                filePath = "/path/to/file",
                parameterName = "file",
                fileName = "testing",
                contentType = "application/octet-stream"
            ).addHeader(
                headerName = "myHeader",
                headerValue = "myHeaderValue"
            ).toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, multipartUploadData)
    }

    @Test
    fun multipartUploadRequest3() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val multipartUploadData = MultipartUploadRequest(context, "https://my.server.com")
            .addFileToUpload(
                filePath = "/path/to/file",
                parameterName = "file",
                fileName = "testing",
                contentType = "application/octet-stream"
            ).toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, multipartUploadData)
    }

    @Test
    fun binaryUploadRequest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val binaryUploadData = BinaryUploadRequest(context, "https://my.server.com")
            .setFileToUpload("/path/to/file")
            .addHeader(
                headerName = "headerName$",
                headerValue = "headerValue$"
            ).toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, binaryUploadData)
    }

    @Test
    fun binaryUploadRequest2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val binaryUploadData = BinaryUploadRequest(context, "https://my.server.com")
            .setFileToUpload("/path/to/file")
            .toPersistableData()

        assertRecreatedUploadRequestIsEqualTo(context, binaryUploadData)
    }
}
