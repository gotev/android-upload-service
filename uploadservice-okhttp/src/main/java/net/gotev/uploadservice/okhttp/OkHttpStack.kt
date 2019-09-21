package net.gotev.uploadservice.okhttp

import net.gotev.uploadservice.network.HttpRequest
import net.gotev.uploadservice.network.HttpStack
import okhttp3.OkHttpClient
import java.io.IOException

/**
 * Implementation of the OkHttp Stack.
 * @author Aleksandar Gotev
 */
class OkHttpStack(private val client: OkHttpClient = OkHttpClient()) : HttpStack {
    @Throws(IOException::class)
    override fun newRequest(uploadId: String, method: String, url: String): HttpRequest {
        return OkHttpStackRequest(uploadId, client, method, url)
    }
}
