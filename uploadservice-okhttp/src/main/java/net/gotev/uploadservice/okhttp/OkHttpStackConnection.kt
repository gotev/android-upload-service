package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.network.HttpConnection
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * [HttpConnection] implementation using OkHttpClient.
 *
 * @author Aleksandar Gotev
 */
class OkHttpStackConnection(private val httpClient: OkHttpClient, private val httpMethod: String, url: String) : HttpConnection {

    private val requestBuilder = Request.Builder().url(URL(url))
    private var bodyLength = 0L
    private var contentType: MediaType? = null
    private val uuid = UUID.randomUUID().toString()

    init {
        UploadServiceLogger.debug(javaClass.simpleName, "creating new OkHttp connection (uuid: $uuid)")
    }

    @Throws(IOException::class)
    override fun setHeaders(requestHeaders: List<NameValue>): HttpConnection {
        for (param in requestHeaders) {
            if ("content-type" == param.name.trim().toLowerCase())
                contentType = param.value.trim().toMediaTypeOrNull()

            requestBuilder.header(param.name.trim(), param.value.trim())
        }

        return this
    }

    override fun setTotalBodyBytes(totalBodyBytes: Long, isFixedLengthStreamingMode: Boolean): HttpConnection {
        // http://stackoverflow.com/questions/33921894/how-do-i-enable-disable-chunked-transfer-encoding-for-a-multi-part-post-that-inc#comment55679982_33921894
        bodyLength = if (isFixedLengthStreamingMode) totalBodyBytes else -1

        return this
    }

    private fun createBody(delegate: HttpConnection.RequestBodyDelegate): RequestBody? {
        if (!httpMethod.hasBody()) return null

        return object : RequestBody() {
            override fun contentLength() = bodyLength

            override fun contentType() = contentType

            override fun writeTo(sink: BufferedSink) {
                OkHttpBodyWriter(sink).use {
                    delegate.onWriteRequestBody(it)
                }
            }
        }
    }

    private fun request(delegate: HttpConnection.RequestBodyDelegate) = requestBuilder
            .method(httpMethod, createBody(delegate))
            .build()

    @Throws(IOException::class)
    override fun getResponse(delegate: HttpConnection.RequestBodyDelegate) = httpClient
            .newCall(request(delegate))
            .execute()
            .use { it.asServerResponse() }

    // Resources are automatically freed after usage. Log only.
    override fun close() {
        UploadServiceLogger.debug(javaClass.simpleName, "closing OkHttp connection (uuid: $uuid)")
    }
}
