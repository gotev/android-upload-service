package net.gotev.uploadservice.testcore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import okio.buffer
import okio.source
import java.util.UUID

fun Context.createTestFile(name: String): String {
    openFileOutput(name, Context.MODE_PRIVATE).use { fileOutput ->
        (1..200).forEach { number ->
            fileOutput.write("$number${UUID.randomUUID()}".toByteArray())
        }
    }

    return getFileStreamPath(name).absolutePath
}

fun Context.readFile(path: String) = openFileInput(path).source().buffer().readByteArray()

val Context.notificationManager: NotificationManager
    get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

private const val notificationChannelId = "UploadServiceTestChannel"

fun Context.createTestNotificationChannel(): String {
    if (Build.VERSION.SDK_INT >= 26) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                notificationChannelId,
                "Upload Service Test Channel",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    return notificationChannelId
}

fun Context.deleteTestNotificationChannel() {
    if (Build.VERSION.SDK_INT >= 26) {
        notificationManager.deleteNotificationChannel(notificationChannelId)
    }
}
