package net.gotev.uploadservice.network

import android.os.Parcelable
import java.io.Serializable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ServerResponse(
    /**
     * server response response code. If you are implementing a Non-HTTP
     * protocol, set this to 200 to inform that the task has been completed
     * successfully. Integer values lower than 200 or greater that 299 indicates
     * error response from server.
     */
    val code: Int,

    /**
     * server response body.
     * If your server responds with a string, you can get it with
     * [ServerResponse.bodyString].
     * If the string is a JSON, you can parse it using a library such as org.json
     * (embedded in Android) or google's gson
     * If your server does not return anything, set this to empty array.
     */
    val body: ByteArray,

    /**
     * server response headers
     */
    val headers: LinkedHashMap<String, String>
) : Parcelable, Serializable {

    /**
     * Gets server response body as string.
     * If the string is a JSON, you can parse it using a library such as org.json
     * (embedded in Android) or google's gson
     * @return string
     */
    @IgnoredOnParcel
    val bodyString: String
        get() = String(body)

    @IgnoredOnParcel
    val isSuccessful: Boolean
        get() = code in 200..399

    companion object {
        fun successfulEmpty(): ServerResponse {
            return ServerResponse(
                code = 200,
                body = ByteArray(1),
                headers = LinkedHashMap()
            )
        }
        fun errorEmpty(): ServerResponse {
            return ServerResponse(
                    code = -1,
                    body = ByteArray(1),
                    headers = LinkedHashMap()
            )
        }
    }
}
