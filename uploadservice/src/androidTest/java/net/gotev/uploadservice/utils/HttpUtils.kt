package net.gotev.uploadservice.utils

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
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
