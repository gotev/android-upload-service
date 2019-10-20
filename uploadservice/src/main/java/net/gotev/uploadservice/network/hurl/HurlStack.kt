package net.gotev.uploadservice.network.hurl

import java.io.IOException
import net.gotev.uploadservice.network.HttpRequest
import net.gotev.uploadservice.network.HttpStack

class HurlStack(
    private val followRedirects: Boolean = true,
    private val useCaches: Boolean = false,
    private val connectTimeout: Int = 15000,
    private val readTimeout: Int = 30000
) : HttpStack {

    @Throws(IOException::class)
    override fun newRequest(uploadId: String, method: String, url: String): HttpRequest {
        return HurlStackRequest(
            uploadId, method, url, followRedirects, useCaches,
            connectTimeout, readTimeout
        )
    }
}
