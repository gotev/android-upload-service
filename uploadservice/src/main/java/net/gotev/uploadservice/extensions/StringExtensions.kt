package net.gotev.uploadservice.extensions

/**
 * @author Aleksandar Gotev
 */
internal fun String?.isASCII(): Boolean {
    if (this.isNullOrBlank())
        return false

    for (index in 0 until length) {
        if (this[index].toInt() > 127) {
            return false
        }
    }

    return true
}
