package net.gotev.uploadservicedemo.extensions

import java.util.regex.Pattern

private val ipAddressPattern by lazy {
    Pattern.compile("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")
}

private val hostnamePattern by lazy {
    Pattern.compile("^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$")
}

fun String?.isValidIPAddress(): Boolean = if (isNullOrBlank())
    false
else
    ipAddressPattern.matcher(this).matches()

fun String?.isValidHostname(): Boolean = if (isNullOrBlank())
    false
else
    hostnamePattern.matcher(this).matches()

fun String?.isValidIPorHostname(): Boolean = isValidIPAddress() || isValidHostname()
