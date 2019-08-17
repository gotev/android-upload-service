package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Represents a request parameter.
 *
 * @author gotev (Aleksandar Gotev)
 */
@Parcelize
data class NameValue(val name: String, val value: String) : Parcelable
