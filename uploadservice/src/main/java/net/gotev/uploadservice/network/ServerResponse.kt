package net.gotev.uploadservice.network

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Contains the server response.
 * @author Aleksandar Gotev
 */
@Parcelize
data class ServerResponse(
        /**
         * server response response code. If you are implementing a Non-HTTP
         * protocol, set this to [UploadTask.TASK_COMPLETED_SUCCESSFULLY]
         * to inform that the task has been completed successfully. Integer values
         * lower than 200 or greater that 299 indicates error response from server.
         */
        val code: Int,

        /**
         * server response body.
         * If your server responds with a string, you can get it with
         * [ServerResponse.getBodyAsString].
         * If the string is a JSON, you can parse it using a library such as org.json
         * (embedded in Android) or google's gson
         * If your server does not return anything, set this to [UploadTask.EMPTY_RESPONSE]
         */
        val body: ByteArray,

        /**
         * server response headers
         */
        val headers: LinkedHashMap<String, String>
) : Parcelable {

    /**
     * Gets server response body as string.
     * If the string is a JSON, you can parse it using a library such as org.json
     * (embedded in Android) or google's gson
     * @return string
     */
    @IgnoredOnParcel
    val bodyString: String
        get() = String(body)
}
