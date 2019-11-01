package net.gotev.uploadservice.data

data class UploadRate(val value: Int = 0, val unit: UploadRateUnit = UploadRateUnit.bitPerSecond) {
    enum class UploadRateUnit {
        bitPerSecond,
        kiloBitPerSecond,
        megaBitPerSecond
    }
}
