package net.gotev.uploadservice.extensions

import android.content.Context
import android.content.Intent
import android.os.Build
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadTaskParameters

fun Context.startNewUpload(taskClass: Class<out UploadTask>, params: UploadTaskParameters): String {
    val intent = Intent(this, UploadService::class.java).apply {
        action = UploadServiceConfig.uploadAction
        putExtra(UploadService.taskClass, taskClass.name)
        putExtra(UploadService.taskParameters, params)
    }

    if (Build.VERSION.SDK_INT >= 26) {
        require(params.notificationConfig != null) {
            "Android Oreo and newer (API 26+) requires a notification configuration for the upload service to run. https://developer.android.com/reference/android/content/Context.html#startForegroundService(android.content.Intent)"
        }
        startForegroundService(intent)
    } else {
        startService(intent)
    }

    return params.id
}
