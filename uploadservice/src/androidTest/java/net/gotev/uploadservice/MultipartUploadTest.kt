package net.gotev.uploadservice

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class MultipartUploadTest {

    private fun Context.createTestFile(name: String = "testFile"): String {
        openFileOutput(name, Context.MODE_PRIVATE).use { fileOutput ->
            (1..100).forEach { number ->
                fileOutput.write("$number${System.currentTimeMillis()}".toByteArray())
            }
        }

        return getFileStreamPath(name).absolutePath
    }

    private fun Context.createNotificationChannel(id: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(id, "Upload Service Demo", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(InetAddress.getByName("localhost").canonicalHostName)
        .build()

    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()

    val clientCertificates = HandshakeCertificates.Builder()
        .addTrustedCertificate(localhostCertificate.certificate)
        .build()

    private val mockWebServer = MockWebServer().apply {
        useHttps(serverCertificates.sslSocketFactory(), false)
    }

    @Before
    fun setup() {
        mockWebServer.start(8080)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    fun UploadRequest<*>.testObserve(context: Application): Pair<ServerResponse?, Throwable?> {
        val lock = CountDownLatch(1)

        var resultingException: Throwable? = null
        var resultingServerResponse: ServerResponse? = null

        val uploadID = UUID.randomUUID().toString()

        setUploadID(uploadID)

        val observer = GlobalRequestObserver(context, object : RequestObserverDelegate {
            override fun onProgress(context: Context, uploadInfo: UploadInfo) {
            }

            override fun onSuccess(
                context: Context,
                uploadInfo: UploadInfo,
                serverResponse: ServerResponse
            ) {
                resultingServerResponse = serverResponse
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
        }, shouldAcceptEventsFrom = { it.uploadId == uploadID })

        observer.register()

        startUpload()

        lock.await(5000, TimeUnit.MILLISECONDS)

        return Pair(resultingServerResponse, resultingException)
    }

    @Test
    fun multipartUpload() {
        val context = InstrumentationRegistry.getInstrumentation().context.applicationContext as Application
        val testFilePath = context.createTestFile()
        context.createNotificationChannel("notiChan")

        UploadServiceConfig.initialize(context, "notiChan", true)
        HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificates.sslSocketFactory())

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val req = MultipartUploadRequest(context, mockWebServer.url("/").toString())
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
            .addFileToUpload(testFilePath, "videofile")
            .setMaxRetries(0)

        val response = req.testObserve(context)

        assertNotNull(response.first)
        assertNull(response.second)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(request.headers["Content-Length"]!!.toLong(), request.bodySize)
        assertEquals(2432, request.bodySize)
    }
}
