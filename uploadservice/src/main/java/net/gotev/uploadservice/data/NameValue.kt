package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NameValue(val name: String, val value: String) : Parcelable
