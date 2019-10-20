package net.gotev.uploadservice.data

import android.os.Parcelable
import java.util.ArrayList
import kotlinx.android.parcel.Parcelize

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
