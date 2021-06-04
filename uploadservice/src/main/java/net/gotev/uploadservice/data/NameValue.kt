package net.gotev.uploadservice.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gotev.uploadservice.extensions.isASCII
import net.gotev.uploadservice.persistence.Persistable
import net.gotev.uploadservice.persistence.PersistableData

@Parcelize
data class NameValue(val name: String, val value: String) : Parcelable, Persistable {
    fun validateAsHeader(): NameValue {
        require(name.isASCII() && value.isASCII()) {
            "Header $name and its value $value must be ASCII only! Read http://stackoverflow.com/a/4410331"
        }

        return this
    }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.name, name)
        putString(CodingKeys.value, value)
    }

    companion object : Persistable.Creator<NameValue> {
        private object CodingKeys {
            const val name = "name"
            const val value = "value"
        }

        override fun createFromPersistableData(data: PersistableData) = NameValue(
            name = data.getString(CodingKeys.name),
            value = data.getString(CodingKeys.value)
        )
    }
}
