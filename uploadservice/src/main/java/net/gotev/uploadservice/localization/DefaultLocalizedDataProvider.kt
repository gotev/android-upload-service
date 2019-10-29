package net.gotev.uploadservice.localization

import net.gotev.uploadservice.data.MinutesSeconds
import net.gotev.uploadservice.data.UploadRate

class DefaultLocalizedDataProvider : LocalizedDataProvider {
    override fun localizedMinutesSeconds(minutesSeconds: MinutesSeconds): String {
        return when {
            minutesSeconds.totalSeconds == 0 -> "0 sec"
            minutesSeconds.minutes == 0 -> "${minutesSeconds.seconds} sec"
            else -> "${minutesSeconds.minutes} min ${minutesSeconds.seconds} sec"
        }
    }

    override fun localizedUploadRate(uploadRate: UploadRate): String {
        val suffix = when (uploadRate.unit) {
            UploadRate.UploadRateUnit.bitSecond -> "bit/s"
            UploadRate.UploadRateUnit.kiloBitSecond -> "Kb/s"
            UploadRate.UploadRateUnit.megaBitSecond -> "Mb/s"
        }

        return "${uploadRate.value} $suffix"
    }

    override fun localizedPercent(percent: Int): String {
        return "$percent %"
    }
}
