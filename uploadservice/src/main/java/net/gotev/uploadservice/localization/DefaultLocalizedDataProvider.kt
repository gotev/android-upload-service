package net.gotev.uploadservice.localization

import net.gotev.uploadservice.data.MinutesSeconds
import net.gotev.uploadservice.data.UploadRate

class DefaultLocalizedDataProvider : LocalizedDataProvider {
    override fun minutesSeconds(minutesSeconds: MinutesSeconds) = when {
        minutesSeconds.minutes == 0 -> "${minutesSeconds.seconds} sec"
        else -> "${minutesSeconds.minutes} min ${minutesSeconds.seconds} sec"
    }

    override fun uploadRate(uploadRate: UploadRate): String {
        val suffix = when (uploadRate.unit) {
            UploadRate.UploadRateUnit.bitSecond -> "bit/s"
            UploadRate.UploadRateUnit.kiloBitSecond -> "Kb/s"
            UploadRate.UploadRateUnit.megaBitSecond -> "Mb/s"
        }

        return "${uploadRate.value} $suffix"
    }

    override fun percent(percent: Int) = "$percent %"

    override fun successfullyUploadedFiles(uploadedFiles: Int) = "$uploadedFiles"

    override fun totalFiles(totalFiles: Int) = "$totalFiles"
}
