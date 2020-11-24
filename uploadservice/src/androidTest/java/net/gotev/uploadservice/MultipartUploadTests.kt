package net.gotev.uploadservice

import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.gotev.uploadservice.utils.UploadRequestStatus
import net.gotev.uploadservice.utils.appContext
import net.gotev.uploadservice.utils.assertContentTypeIsMultipartFormData
import net.gotev.uploadservice.utils.assertDeclaredContentLengthMatchesPostBodySize
import net.gotev.uploadservice.utils.assertFile
import net.gotev.uploadservice.utils.assertHeader
import net.gotev.uploadservice.utils.assertHttpMethodIs
import net.gotev.uploadservice.utils.assertParameter
import net.gotev.uploadservice.utils.baseUrl
import net.gotev.uploadservice.utils.classEquals
import net.gotev.uploadservice.utils.createTestFile
import net.gotev.uploadservice.utils.createTestNotificationChannel
import net.gotev.uploadservice.utils.deleteTestNotificationChannel
import net.gotev.uploadservice.utils.getBlockingResponse
import net.gotev.uploadservice.utils.multipartBodyParts
import net.gotev.uploadservice.utils.newSSLMockWebServer
import net.gotev.uploadservice.utils.readFile
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class MultipartUploadTests {

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

    private fun createMultipartUploadRequest() =
        MultipartUploadRequest(appContext, mockWebServer.baseUrl)
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
            .addFileToUpload(appContext.createTestFile("testFile"), "videofile")
            .setMaxRetries(0)

    @Test
    fun successfulMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = createMultipartUploadRequest()

        val responseStatus = uploadRequest.getBlockingResponse(appContext)

        responseStatus.classEquals(UploadRequestStatus.Successful::class)
        val response = (responseStatus as UploadRequestStatus.Successful).response

        assertEquals(200, response.code)
        assertTrue("body should be empty!", response.body.isEmpty())

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertDeclaredContentLengthMatchesPostBodySize()
            assertContentTypeIsMultipartFormData()
            assertHeader("Authorization", "Bearer bearerToken")
            assertHeader("User-Agent", "SomeUserAgent")

            multipartBodyParts.apply {
                assertEquals("number of parts is wrong", 8, size)
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
                    filename = "testFile"
                )
            }
        }
    }

    @Test
    fun serverErrorMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400))

        val uploadRequest = createMultipartUploadRequest()

        val responseStatus = uploadRequest.getBlockingResponse(appContext)

        responseStatus.classEquals(UploadRequestStatus.ServerError::class)
        val response = (responseStatus as UploadRequestStatus.ServerError).response

        assertEquals(400, response.code)
        assertTrue("body should be empty!", response.body.isEmpty())

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertDeclaredContentLengthMatchesPostBodySize()
            assertContentTypeIsMultipartFormData()
            assertHeader("Authorization", "Bearer bearerToken")
            assertHeader("User-Agent", "SomeUserAgent")
        }
    }

    @Test
    fun serverInterruptedMultipartUpload() {
        mockWebServer.enqueue(
            MockResponse()
                .throttleBody(100, 10, TimeUnit.MILLISECONDS)
                .setResponseCode(200)
        )

        val uploadRequest = createMultipartUploadRequest()

        var shutdownIsTriggered = false

        val response = uploadRequest.getBlockingResponse(appContext, doOnProgress = { _ ->
            // shutdown server on first progress
            if (!shutdownIsTriggered) {
                shutdownIsTriggered = true
                mockWebServer.shutdown()
            }
        })

        response.classEquals(UploadRequestStatus.OtherError::class)
        val exception = (response as UploadRequestStatus.OtherError).exception
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

        var cancellationIsTriggered = false

        val response = uploadRequest.getBlockingResponse(appContext, doOnProgress = { uploadInfo ->
            // cancel upload on first progress
            if (!cancellationIsTriggered) {
                cancellationIsTriggered = true
                UploadService.stopUpload(uploadInfo.uploadId)
            }
        })

        response.classEquals(UploadRequestStatus.CancelledByUser::class)

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertContentTypeIsMultipartFormData()
            assertHeader("Authorization", "Bearer bearerToken")
            assertHeader("User-Agent", "SomeUserAgent")
        }
    }
}
