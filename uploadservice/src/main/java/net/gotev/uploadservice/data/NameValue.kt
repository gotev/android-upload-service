package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.gotev.uploadservice.extensions.isASCII

@Parcelize
data class NameValue(val name: String, val value: String) : Parcelable {
    fun validateAsHeader(): NameValue {
        require(name.isASCII() && value.isASCII()) {
            "Header $name and its value $value must be ASCII only! Read http://stackoverflow.com/a/4410331"
        }

        return this
    }
}
