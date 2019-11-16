package net.gotev.uploadservice.network.hurl

import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.BodyWriter
import net.gotev.uploadservice.network.HttpRequest
import net.gotev.uploadservice.network.ServerResponse
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

class HurlStackRequest(
    private val userAgent: String,
    private val uploadId: String,
    method: String,
    url: String,
    followRedirects: Boolean,
    useCaches: Boolean,
    connectTimeoutMillis: Int,
    readTimeoutMillis: Int
) : HttpRequest {

    private val connection: HttpURLConnection
    private val uuid = UUID.randomUUID().toString()

    private fun String.createConnection(): HttpURLConnection {
        val url = URL(trim())

        return if ("https".equals(url.protocol, ignoreCase = true)) {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
    }

    init {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "creating new HttpURLConnection (uuid: $uuid)"
        }

        connection = url.createConnection().apply {
            doInput = true
            doOutput = true
            this.connectTimeout = connectTimeoutMillis
            this.readTimeout = readTimeoutMillis
            this.useCaches = useCaches
            instanceFollowRedirects = followRedirects
            requestMethod = method
        }
    }

    private val responseBody: ByteArray
        @Throws(IOException::class)
        get() {
            return if (connection.responseCode / 100 == 2) {
                connection.inputStream
            } else {
                connection.errorStream
            }.use {
                it.readBytes()
            }
        }

    private val responseHeaders: LinkedHashMap<String, String>
        @Throws(IOException::class)
        get() {
            val headers = connection.headerFields ?: return LinkedHashMap(0)

            return LinkedHashMap<String, String>(headers.size).apply {
                headers.entries
                    .filter { it.key != null && it.value != null && it.value.isNotEmpty() }
                    .forEach { (key, values) ->
                        this[key] = values.first()
                    }
            }
        }

    @Throws(IOException::class)
    override fun setHeaders(requestHeaders: List<NameValue>): HttpRequest {
        connection.setRequestProperty("User-Agent", userAgent)

        for (param in requestHeaders) {
            connection.setRequestProperty(param.name.trim(), param.value.trim())
        }

        return this
    }

    override fun setTotalBodyBytes(
        totalBodyBytes: Long,
        isFixedLengthStreamingMode: Boolean
    ): HttpRequest {
        connection.apply {
            if (isFixedLengthStreamingMode) {
                setFixedLengthStreamingMode(totalBodyBytes)
            } else {
                setChunkedStreamingMode(0)
            }
        }

        return this
    }

    @Throws(IOException::class)
    override fun getResponse(
        delegate: HttpRequest.RequestBodyDelegate,
        listener: BodyWriter.OnStreamWriteListener
    ) = use {
        HurlBodyWriter(connection.outputStream, listener).use {
            delegate.onWriteRequestBody(it)
        }

        ServerResponse(connection.responseCode, responseBody, responseHeaders)
    }

    override fun close() {
        UploadServiceLogger.debug(javaClass.simpleName, uploadId) {
            "closing HttpURLConnection (uuid: $uuid)"
        }

        try {
            connection.inputStream.close()
        } catch (ignored: Throwable) {
        }

        try {
            connection.outputStream.flush()
        } catch (ignored: Throwable) {
        }

        try {
            connection.outputStream.close()
        } catch (ignored: Throwable) {
        }

        try {
            connection.disconnect()
        } catch (ignored: Throwable) {
        }
    }
}
