package net.gotev.uploadservice

import android.content.Context
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.gotev.uploadservice.utils.UploadRequestStatus
import net.gotev.uploadservice.utils.appContext
import net.gotev.uploadservice.utils.assertContentTypeIsMultipartFormData
import net.gotev.uploadservice.utils.assertDeclaredContentLengthMatchesPostBodySize
import net.gotev.uploadservice.utils.assertHttpMethodIs
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
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertDeclaredContentLengthMatchesPostBodySize()
            assertContentTypeIsMultipartFormData()
            assertEquals("Bearer bearerToken", headers["Authorization"])
            assertEquals("SomeUserAgent", headers["User-Agent"])
        }
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

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertDeclaredContentLengthMatchesPostBodySize()
            assertContentTypeIsMultipartFormData()
            assertEquals("Bearer bearerToken", headers["Authorization"])
            assertEquals("SomeUserAgent", headers["User-Agent"])
        }
    }

    @Test
    fun userCancelledMultipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadId = UUID.randomUUID().toString()

        val uploadRequest = MultipartUploadRequest(appContext, mockWebServer.baseUrl)
            .setUploadID(uploadId)
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

        var resultingException: Throwable? = null

        val lock = CountDownLatch(1)

        val observer = GlobalRequestObserver(appContext, object : RequestObserverDelegate {
            var cancellationIsTriggered = false

            override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                // cancel upload on first progress
                if (uploadInfo.progressPercent > 0 && !cancellationIsTriggered) {
                    cancellationIsTriggered = true
                    UploadService.stopUpload(uploadId)
                }
            }

            override fun onSuccess(
                context: Context,
                uploadInfo: UploadInfo,
                serverResponse: ServerResponse
            ) {
            }

            override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
                resultingException = exception
            }

            override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                lock.countDown()
            }

            override fun onCompletedWhileNotObserving() {
                lock.countDown()
            }
        }, shouldAcceptEventsFrom = { it.uploadId == uploadId })

        observer.register()

        uploadRequest.startUpload()

        lock.await(5000, TimeUnit.MILLISECONDS)

        assertTrue(resultingException is UserCancelledUploadException)

        mockWebServer.takeRequest().apply {
            assertHttpMethodIs("POST")
            assertContentTypeIsMultipartFormData()
            assertEquals("Bearer bearerToken", headers["Authorization"])
            assertEquals("SomeUserAgent", headers["User-Agent"])
        }
    }
}
