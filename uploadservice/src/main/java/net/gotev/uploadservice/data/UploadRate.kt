package net.gotev.uploadservice.data

data class UploadRate(val value: Int = 0, val unit: UploadRateUnit = UploadRateUnit.bitSecond) {
    enum class UploadRateUnit {
        bitSecond,
        kiloBitSecond,
        megaBitSecond
    }
}
