package net.gotev.uploadservice

import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.gotev.uploadservice.utils.UploadRequestStatus
import net.gotev.uploadservice.utils.appContext
import net.gotev.uploadservice.utils.baseUrl
import net.gotev.uploadservice.utils.createTestFile
import net.gotev.uploadservice.utils.createTestNotificationChannel
import net.gotev.uploadservice.utils.deleteTestNotificationChannel
import net.gotev.uploadservice.utils.newSSLMockWebServer
import net.gotev.uploadservice.utils.getBlockingResponse
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MultipartUploadTest {

    private val mockWebServer = newSSLMockWebServer()

    @Before
    fun setup() {
        mockWebServer.start(8080)

        UploadServiceConfig.initialize(appContext, appContext.createTestNotificationChannel(), true)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        appContext.deleteTestNotificationChannel()
        UploadService.stop(appContext, true)
    }

    @Test
    fun successfulMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = MultipartUploadRequest(appContext, mockWebServer.baseUrl)
            .setMethod("POST")
            .setBearerAuth("bearerToken")
            .setUsesFixedLengthStreamingMode(true)
            .addHeader("User-Agent", "SomeUserAgent")
            .addParameter("privacy", "1")
            .addParameter("nsfw", "false")
            .addParameter("name", "myfilename")
            .addParameter("commentsEnabled", "true")
            .addParameter("downloadEnabled", "true")
            .addParameter("waitTranscoding", "true")
            .addParameter("channelId", "123456")
            .addFileToUpload(appContext.createTestFile(), "videofile")
            .setMaxRetries(0)

        val responseStatus = uploadRequest.getBlockingResponse(appContext)

        assertTrue(responseStatus is UploadRequestStatus.Successful)
        val response = (responseStatus as UploadRequestStatus.Successful).response

        assertEquals(200, response.code)
        assertTrue(response.body.isEmpty())

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals(httpRequest.headers["Content-Length"]!!.toLong(), httpRequest.bodySize)

        assertEquals("Bearer bearerToken", httpRequest.headers["Authorization"])
        assertEquals("SomeUserAgent", httpRequest.headers["User-Agent"])
        assertTrue(
            httpRequest.headers["Content-Type"]?.startsWith("multipart/form-data; boundary=-------UploadService")
                ?: false
        )
    }

    @Test
    fun serverErrorMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val uploadRequest = MultipartUploadRequest(appContext, mockWebServer.baseUrl)
            .setMethod("POST")
            .setBearerAuth("bearerToken")
            .setUsesFixedLengthStreamingMode(true)
            .addHeader("User-Agent", "SomeUserAgent")
            .addParameter("privacy", "1")
            .addParameter("nsfw", "false")
            .addParameter("name", "myfilename")
            .addParameter("commentsEnabled", "true")
            .addParameter("downloadEnabled", "true")
            .addParameter("waitTranscoding", "true")
            .addParameter("channelId", "123456")
            .addFileToUpload(appContext.createTestFile(), "videofile")
            .setMaxRetries(0)

        val responseStatus = uploadRequest.getBlockingResponse(appContext)

        assertTrue(responseStatus is UploadRequestStatus.ServerError)
        val response = (responseStatus as UploadRequestStatus.ServerError).response

        assertEquals(400, response.code)
        assertTrue(response.body.isEmpty())

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals(httpRequest.headers["Content-Length"]!!.toLong(), httpRequest.bodySize)

        assertEquals("Bearer bearerToken", httpRequest.headers["Authorization"])
        assertEquals("SomeUserAgent", httpRequest.headers["User-Agent"])
        assertTrue(
            httpRequest.headers["Content-Type"]?.startsWith("multipart/form-data; boundary=-------UploadService")
                ?: false
        )
    }
}
