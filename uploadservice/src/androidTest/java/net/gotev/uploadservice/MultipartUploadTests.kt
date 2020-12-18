package net.gotev.uploadservice

import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.gotev.uploadservice.testcore.UploadServiceTestSuite
import net.gotev.uploadservice.testcore.assertBodySizeIsLowerThanDeclaredContentLength
import net.gotev.uploadservice.testcore.assertContentTypeIsMultipartFormData
import net.gotev.uploadservice.testcore.assertDeclaredContentLengthMatchesPostBodySize
import net.gotev.uploadservice.testcore.assertEmptyBodyAndHttpCodeIs
import net.gotev.uploadservice.testcore.assertFile
import net.gotev.uploadservice.testcore.assertHeader
import net.gotev.uploadservice.testcore.assertHttpMethodIs
import net.gotev.uploadservice.testcore.assertParameter
import net.gotev.uploadservice.testcore.baseUrl
import net.gotev.uploadservice.testcore.createTestFile
import net.gotev.uploadservice.testcore.getBlockingResponse
import net.gotev.uploadservice.testcore.multipartBodyParts
import net.gotev.uploadservice.testcore.readFile
import net.gotev.uploadservice.testcore.requireCancelledByUser
import net.gotev.uploadservice.testcore.requireOtherError
import net.gotev.uploadservice.testcore.requireServerError
import net.gotev.uploadservice.testcore.requireSuccessful
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class MultipartUploadTests : UploadServiceTestSuite() {

    private fun createMultipartUploadRequest() =
        MultipartUploadRequest(appContext, mockWebServer.baseUrl)
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
            .addFileToUpload(appContext.createTestFile("testFile"), "videofile")
            .addFileToUpload(
                appContext.createTestFile("testFile2"),
                "videofile2",
                contentType = "video/mp4"
            )
            .setMaxRetries(0)

    private fun RecordedRequest.verifyMultipartUploadRequestHeadersAndBody() {
        assertHttpMethodIs("POST")
        assertDeclaredContentLengthMatchesPostBodySize()
        assertContentTypeIsMultipartFormData()
        assertHeader("Authorization", "Bearer bearerToken")
        assertHeader("User-Agent", "SomeUserAgent")

        multipartBodyParts.apply {
            assertEquals("number of parts is wrong", 9, size)
            assertParameter("privacy", "1")
            assertParameter("nsfw", "false")
            assertParameter("name", "myfilename")
            assertParameter("commentsEnabled", "true")
            assertParameter("downloadEnabled", "true")
            assertParameter("waitTranscoding", "true")
            assertParameter("channelId", "123456")
            assertFile(
                parameterName = "videofile",
                fileContent = appContext.readFile("testFile"),
                filename = "testFile",
                contentType = "application/octet-stream"
            )
            assertFile(
                parameterName = "videofile2",
                fileContent = appContext.readFile("testFile2"),
                filename = "testFile2",
                contentType = "video/mp4"
            )
        }
    }

    @Test
    fun successfulMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createMultipartUploadRequest()

        val response = uploadRequest.getBlockingResponse(appContext).requireSuccessful()

        response.assertEmptyBodyAndHttpCodeIs(200)

        mockWebServer.takeRequest().verifyMultipartUploadRequestHeadersAndBody()
    }

    @Test
    fun successfulMultipartUploadAfterOneRetry() {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createMultipartUploadRequest()
            .setMaxRetries(1)

        val response = uploadRequest.getBlockingResponse(appContext).requireSuccessful()

        response.assertEmptyBodyAndHttpCodeIs(200)

        mockWebServer.takeRequest() // discard the first request being made
        mockWebServer.takeRequest().verifyMultipartUploadRequestHeadersAndBody()
    }

    @Test
    fun serverErrorMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val uploadRequest = createMultipartUploadRequest()

        val response = uploadRequest.getBlockingResponse(appContext).requireServerError()

        response.assertEmptyBodyAndHttpCodeIs(400)

        mockWebServer.takeRequest().verifyMultipartUploadRequestHeadersAndBody()
    }

    @Test
    fun serverInterruptedMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY))

        val uploadRequest = createMultipartUploadRequest()

        val exception = uploadRequest.getBlockingResponse(appContext).requireOtherError()

        assertTrue(
            "A subclass of IOException has to be thrown. Got ${exception::class.java}",
            exception is IOException
        )
    }

    @Test
    fun userCancelledMultipartUpload() {
        mockWebServer.enqueue(
            MockResponse()
                .throttleBody(100, 10, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
        )

        val uploadRequest = createMultipartUploadRequest()

        uploadRequest.getBlockingResponse(
            appContext,
            doOnFirstProgress = { info ->
                // cancel upload on first progress
                UploadService.stopUpload(info.uploadId)
            }
        ).requireCancelledByUser()

        with(mockWebServer.takeRequest()) {
            assertHttpMethodIs("POST")
            assertContentTypeIsMultipartFormData()
            assertBodySizeIsLowerThanDeclaredContentLength()
            assertHeader("Authorization", "Bearer bearerToken")
            assertHeader("User-Agent", "SomeUserAgent")
        }
    }
}
