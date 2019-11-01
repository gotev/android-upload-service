package net.gotev.uploadservice.data

data class UploadElapsedTime(val minutes: Int, val seconds: Int) {
    val totalSeconds: Int
        get() = minutes * 60 + seconds
}
