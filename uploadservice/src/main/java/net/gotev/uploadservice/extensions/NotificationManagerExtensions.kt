package net.gotev.uploadservice.extensions

import android.app.NotificationManager
import android.os.Build

internal fun NotificationManager.validateNotificationChannel(channelID: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        getNotificationChannel(channelID)
            ?: throw IllegalArgumentException("The provided notification channel ID $channelID does not exist! You must create it at app startup and before Upload Service!")
    }
}
