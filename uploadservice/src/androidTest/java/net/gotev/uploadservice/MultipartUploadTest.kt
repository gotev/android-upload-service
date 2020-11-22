package net.gotev.uploadservice

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.gotev.uploadservice.utils.createTestFile
import net.gotev.uploadservice.utils.createTestNotificationChannel
import net.gotev.uploadservice.utils.startAndObserveGlobally
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import javax.net.ssl.HttpsURLConnection

class MultipartUploadTest {

    private val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(InetAddress.getByName("localhost").canonicalHostName)
        .build()

    private val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()

    private val clientCertificates = HandshakeCertificates.Builder()
        .addTrustedCertificate(localhostCertificate.certificate)
        .build()

    private val mockWebServer = MockWebServer().apply {
        useHttps(serverCertificates.sslSocketFactory(), false)
    }

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
    }

    @Before
    fun setup() {
        mockWebServer.start(8080)

        HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificates.sslSocketFactory())
        UploadServiceConfig.initialize(context, context.createTestNotificationChannel(), true)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun multipartUpload() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val uploadRequest = MultipartUploadRequest(context, mockWebServer.url("/").toString())
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
            .addFileToUpload(context.createTestFile(), "videofile")
            .setMaxRetries(0)

        uploadRequest.startAndObserveGlobally(context).let { (serverResponse, exception) ->
            assertNotNull(serverResponse)
            assertNull(exception)

            assertEquals(200, serverResponse!!.code)
            assertTrue(serverResponse.body.isEmpty())
        }

        val httpRequest = mockWebServer.takeRequest()
        assertEquals("POST", httpRequest.method)
        assertEquals(httpRequest.headers["Content-Length"]!!.toLong(), httpRequest.bodySize)

        assertEquals("Bearer bearerToken", httpRequest.headers["Authorization"])
        assertEquals("SomeUserAgent", httpRequest.headers["User-Agent"])
        assertTrue(httpRequest.headers["Content-Type"]?.startsWith("multipart/form-data; boundary=-------UploadService") ?: false)
    }
}
