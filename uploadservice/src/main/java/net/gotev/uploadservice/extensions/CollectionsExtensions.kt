package net.gotev.uploadservice.extensions

import net.gotev.uploadservice.data.NameValue

fun ArrayList<NameValue>.addHeader(name: String, value: String) {
    add(NameValue(name, value).validateAsHeader())
}

fun LinkedHashMap<String, String>.setOrRemove(key: String, value: String?) {
    if (value == null) {
        remove(key)
    } else {
        this[key] = value
    }
}
