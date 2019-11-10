package net.gotev.uploadservice

import android.content.Context
import android.os.Parcelable
import android.util.Base64
import net.gotev.uploadservice.data.HttpUploadTaskParameters
import net.gotev.uploadservice.data.NameValue
import net.gotev.uploadservice.extensions.addHeader
import net.gotev.uploadservice.extensions.isValidHttpUrl
import java.util.Locale

/**
 * Represents a generic HTTP upload request.<br></br>
 * Subclass to create your own custom HTTP upload request.
 * @param context application context
 * @param serverUrl URL of the server side script that handles the request
 */
abstract class HttpUploadRequest<B : HttpUploadRequest<B>>(context: Context, serverUrl: String) :
    UploadRequest<B>(context, serverUrl) {

    protected val httpParams = HttpUploadTaskParameters()

    init {
        require(serverUrl.isValidHttpUrl()) { "Specify either http:// or https:// as protocol" }
    }

    override fun getAdditionalParameters(): Parcelable {
        return httpParams
    }

    /**
     * Adds a header to this upload request.
     *
     * @param headerName header name
     * @param headerValue header value
     * @return self instance
     */
    fun addHeader(headerName: String, headerValue: String): B {
        httpParams.requestHeaders.addHeader(headerName, headerValue)
        return self()
    }

    /**
     * Sets the HTTP Basic Authentication header.
     * @param username HTTP Basic Auth username
     * @param password HTTP Basic Auth password
     * @return self instance
     */
    fun setBasicAuth(username: String, password: String): B {
        val auth = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        return addHeader("Authorization", "Basic $auth")
    }

    /**
     * Sets HTTP Bearer authentication with a token.
     * @param bearerToken bearer authorization token
     * @return self instance
     */
    fun setBearerAuth(bearerToken: String): B {
        return addHeader("Authorization", "Bearer $bearerToken")
    }

    /**
     * Adds a parameter to this upload request.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     * @return self instance
     */
    open fun addParameter(paramName: String, paramValue: String): B {
        httpParams.requestParameters.add(NameValue(paramName, paramValue))
        return self()
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param array values
     * @return self instance
     */
    open fun addArrayParameter(paramName: String, vararg array: String): B {
        for (value in array) {
            httpParams.requestParameters.add(NameValue(paramName, value))
        }
        return self()
    }

    /**
     * Adds a parameter with multiple values to this upload request.
     *
     * @param paramName parameter name
     * @param list values
     * @return self instance
     */
    open fun addArrayParameter(paramName: String, list: List<String>): B {
        for (value in list) {
            httpParams.requestParameters.add(NameValue(paramName, value))
        }
        return self()
    }

    /**
     * Sets the HTTP method to use. By default it's set to POST.
     *
     * @param method new HTTP method to use
     * @return self instance
     */
    fun setMethod(method: String): B {
        httpParams.method = method.toUpperCase(Locale.ROOT)
        return self()
    }

    /**
     * Sets if this upload request is using fixed length streaming mode.
     * By default it's set to true.
     * If it uses fixed length streaming mode, then the value returned by
     * [HttpUploadTask.getBodyLength] will be automatically used to properly set the
     * underlying [java.net.HttpURLConnection], otherwise chunked streaming mode will be used.
     * @param fixedLength true to use fixed length streaming mode (this is the default setting) or
     * false to use chunked streaming mode.
     * @return self instance
     */
    fun setUsesFixedLengthStreamingMode(fixedLength: Boolean): B {
        httpParams.usesFixedLengthStreamingMode = fixedLength
        return self()
    }
}
