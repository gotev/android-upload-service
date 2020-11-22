package net.gotev.uploadservice.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import java.util.UUID

fun Context.createTestFile(name: String = "testFile"): String {
    openFileOutput(name, Context.MODE_PRIVATE).use { fileOutput ->
        (1..100).forEach { number ->
            fileOutput.write("$number${UUID.randomUUID()}".toByteArray())
        }
    }

    return getFileStreamPath(name).absolutePath
}

fun Context.createTestNotificationChannel(): String {
    val notificationChannelId = "UploadServiceTestChannel"

    if (Build.VERSION.SDK_INT >= 26) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(notificationChannelId, "Upload Service Test Channel", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    return notificationChannelId
}
