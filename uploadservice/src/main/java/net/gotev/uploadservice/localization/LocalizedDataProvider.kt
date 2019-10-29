package net.gotev.uploadservice.localization

import net.gotev.uploadservice.data.MinutesSeconds
import net.gotev.uploadservice.data.UploadRate

interface LocalizedDataProvider {
    fun localizedMinutesSeconds(minutesSeconds: MinutesSeconds): String
    fun localizedUploadRate(uploadRate: UploadRate): String
    fun localizedPercent(percent: Int): String
}
