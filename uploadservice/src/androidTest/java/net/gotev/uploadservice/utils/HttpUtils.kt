package net.gotev.uploadservice.utils

import okhttp3.MultipartReader
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import java.lang.IllegalArgumentException
import java.net.InetAddress
import javax.net.ssl.HttpsURLConnection

private val localhostCertificate by lazy {
    HeldCertificate.Builder()
        .addSubjectAlternativeName(InetAddress.getByName("localhost").canonicalHostName)
        .build()
}

private val serverCertificates by lazy {
    HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()
}

private val clientCertificates by lazy {
    HandshakeCertificates.Builder()
        .addTrustedCertificate(localhostCertificate.certificate)
        .build()
}

fun newSSLMockWebServer() = MockWebServer().apply {
    HttpsURLConnection.setDefaultSSLSocketFactory(clientCertificates.sslSocketFactory())
    useHttps(serverCertificates.sslSocketFactory(), false)
}

val MockWebServer.baseUrl: String
    get() = url("/").toString()

fun newSSLOkHttpClientBuilder() =
    OkHttpClient.Builder()
        .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)

fun RecordedRequest.assertContentTypeIsMultipartFormData() {
    assertTrue(
        headers["Content-Type"]?.startsWith("multipart/form-data; boundary=-------UploadService")
            ?: false
    )
}

sealed class BodyPart(val name: String) {
    class Parameter(name: String, val value: String): BodyPart(name)
    class File(name: String, val filename: String?, val contentType: String, val body: ByteArray) : BodyPart(name)
}

val RecordedRequest.multipartBodyParts: Map<String, BodyPart>
    get() {
        val parts = HashMap<String, BodyPart>()

        val contentType = headers["Content-Type"]
            ?: throw IllegalArgumentException("missing Content-Type header")

        MultipartReader(body, contentType.split("boundary=")[1]).use {
            while (true) {
                val part = it.nextPart() ?: return@use
                val contentDisposition = part.headers["Content-Disposition"]
                    ?: throw IllegalArgumentException("missing Content-Disposition header")
                val contentDispositionValues = contentDisposition.split(";")

                if (contentDispositionValues.first() != "form-data")
                    throw IllegalArgumentException("Content-Disposition misses form-data")

                val map = HashMap<String, String>()

                contentDispositionValues.drop(1).map { rawParameter ->
                    val parameter = rawParameter.split("=")
                        .map { s -> s.trim().removeSurrounding("\"") }
                    map[parameter[0]] = parameter[1]
                }

                val partContentType = part.headers["Content-Type"]

                val multipartPart = if (partContentType == null) {
                    BodyPart.Parameter(
                        name = map["name"]!!,
                        value = part.body.readUtf8()
                    )
                } else {
                    BodyPart.File(
                        name = map["name"]!!,
                        filename = map["filename"],
                        contentType = partContentType,
                        body = part.body.readByteArray()
                    )
                }

                parts[multipartPart.name] = multipartPart
            }
        }

        return parts
    }

fun Map<String, BodyPart>.assertParameter(name: String, value: String) {
    assertTrue("No parameter $name found", containsKey(name))

    when (val param = get(name)!!) {
        is BodyPart.Parameter -> {
            assertEquals(value, param.value)
        }

        else -> {
            fail("$name is present in the map, but it's not a parameter")
        }
    }
}

fun Map<String, BodyPart>.assertFile(
    parameterName: String,
    fileContent: ByteArray,
    contentType: String = "application/octet-stream",
    filename: String? = null
) {
    assertTrue("No parameter $parameterName found", containsKey(parameterName))

    when (val file = get(parameterName)!!) {
        is BodyPart.File -> {
            assertEquals(contentType, file.contentType)
            filename?.let { assertEquals(it, file.filename) }
            assertEquals("Size is not equal", fileContent.size, file.body.size)
            assertTrue("File content does not match", fileContent.contentEquals(file.body))
        }

        else -> {
            fail("$parameterName is present in the map, but it's not a file")
        }
    }
}

fun RecordedRequest.assertHeader(name: String, value: String) {
    val headerValue = headers[name]

    assertNotNull("header $name is not present", headerValue)
    assertEquals("header $name value does not match", value, headerValue)
}

fun RecordedRequest.assertDeclaredContentLengthMatchesPostBodySize() {
    assertEquals(headers["Content-Length"]!!.toLong(), bodySize)
}

fun RecordedRequest.assertHttpMethodIs(expectedMethod: String) {
    assertEquals(expectedMethod, method)
}
