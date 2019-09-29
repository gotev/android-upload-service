package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * Class which contains specific parameters for HTTP uploads.
 */
@Parcelize
data class HttpUploadTaskParameters(
        var customUserAgent: String? = null,
        var method: String = "POST",
        var usesFixedLengthStreamingMode: Boolean = true,
        val requestHeaders: ArrayList<NameValue> = ArrayList(5),
        val requestParameters: ArrayList<NameValue> = ArrayList(5)
) : Parcelable
