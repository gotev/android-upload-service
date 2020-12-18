package net.gotev.uploadservice

import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import net.gotev.uploadservice.testcore.UploadServiceTestSuite
import net.gotev.uploadservice.testcore.assertBodySizeIsLowerThanDeclaredContentLength
import net.gotev.uploadservice.testcore.assertDeclaredContentLengthMatchesPostBodySize
import net.gotev.uploadservice.testcore.assertEmptyBodyAndHttpCodeIs
import net.gotev.uploadservice.testcore.assertHeader
import net.gotev.uploadservice.testcore.assertHttpMethodIs
import net.gotev.uploadservice.testcore.baseUrl
import net.gotev.uploadservice.testcore.createTestFile
import net.gotev.uploadservice.testcore.getBlockingResponse
import net.gotev.uploadservice.testcore.readFile
import net.gotev.uploadservice.testcore.requireCancelledByUser
import net.gotev.uploadservice.testcore.requireOtherError
import net.gotev.uploadservice.testcore.requireServerError
import net.gotev.uploadservice.testcore.requireSuccessful
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class BinaryUploadTests : UploadServiceTestSuite() {

    private fun createBinaryUploadRequest() =
        BinaryUploadRequest(appContext, mockWebServer.baseUrl)
            .setBearerAuth("bearerToken")
            .setUsesFixedLengthStreamingMode(true)
            .addHeader("User-Agent", "SomeUserAgent")
            .setFileToUpload(appContext.createTestFile("testFile"))
            .setMaxRetries(0)

    private fun RecordedRequest.verifyBinaryUploadRequestHeaders() {
        assertHttpMethodIs("POST")
        assertHeader("Content-Type", "application/octet-stream")
        assertHeader("Authorization", "Bearer bearerToken")
        assertHeader("User-Agent", "SomeUserAgent")
    }

    private fun RecordedRequest.verifyBinaryUploadRequestHeadersAndBody() {
        verifyBinaryUploadRequestHeaders()
        assertDeclaredContentLengthMatchesPostBodySize()

        assertTrue(
            "File content does not match",
            appContext.readFile("testFile").contentEquals(body.readByteArray())
        )
    }

    @Test
    fun successfulBinaryUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createBinaryUploadRequest()

        val response = uploadRequest.getBlockingResponse(appContext).requireSuccessful()

        response.assertEmptyBodyAndHttpCodeIs(200)

        mockWebServer.takeRequest().verifyBinaryUploadRequestHeadersAndBody()
    }

    @Test
    fun successfulBinaryUploadWithContentTypeOverride() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createBinaryUploadRequest()
            .addHeader("Content-Type", "video/mp4")

        val response = uploadRequest.getBlockingResponse(appContext).requireSuccessful()

        response.assertEmptyBodyAndHttpCodeIs(200)

        with(mockWebServer.takeRequest()) {
            assertHttpMethodIs("POST")
            assertHeader("Content-Type", "video/mp4")
            assertHeader("Authorization", "Bearer bearerToken")
            assertHeader("User-Agent", "SomeUserAgent")
            assertDeclaredContentLengthMatchesPostBodySize()

            assertTrue(
                "File content does not match",
                appContext.readFile("testFile").contentEquals(body.readByteArray())
            )
        }
    }

    @Test
    fun successfulBinaryUploadAfterOneRetry() {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY))
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createBinaryUploadRequest()
            .setMaxRetries(1)

        val response = uploadRequest.getBlockingResponse(appContext).requireSuccessful()

        response.assertEmptyBodyAndHttpCodeIs(200)

        mockWebServer.takeRequest() // discard the first request being made
        mockWebServer.takeRequest().verifyBinaryUploadRequestHeadersAndBody()
    }

    @Test
    fun serverErrorBinaryUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val uploadRequest = createBinaryUploadRequest()

        val response = uploadRequest.getBlockingResponse(appContext).requireServerError()

        response.assertEmptyBodyAndHttpCodeIs(400)

        mockWebServer.takeRequest().verifyBinaryUploadRequestHeadersAndBody()
    }

    @Test
    fun serverInterruptedBinaryUpload() {
        mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_DURING_REQUEST_BODY))

        val uploadRequest = createBinaryUploadRequest()

        val exception = uploadRequest.getBlockingResponse(appContext).requireOtherError()

        assertTrue(
            "A subclass of IOException has to be thrown. Got ${exception::class.java}",
            exception is IOException
        )
    }

    @Test
    fun userCancelledBinaryUpload() {
        mockWebServer.enqueue(
            MockResponse()
                .throttleBody(100, 10, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
        )

        val uploadRequest = createBinaryUploadRequest()

        uploadRequest.getBlockingResponse(
            appContext,
            doOnFirstProgress = { info ->
                // cancel upload on first progress
                UploadService.stopUpload(info.uploadId)
            }
        ).requireCancelledByUser()

        with(mockWebServer.takeRequest()) {
            verifyBinaryUploadRequestHeaders()
            assertBodySizeIsLowerThanDeclaredContentLength()
        }
    }
}
