package net.gotev.uploadservice.data

data class UploadRate(val value: Int = 0, val unit: UploadRateUnit = UploadRateUnit.BitPerSecond) {
    enum class UploadRateUnit {
        BitPerSecond,
        KilobitPerSecond,
        MegabitPerSecond
    }
}
