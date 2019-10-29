package net.gotev.uploadservice.data

data class MinutesSeconds(val minutes: Int, val seconds: Int) {
    val totalSeconds: Int
        get() = minutes * 60 + seconds
}
