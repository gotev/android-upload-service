package net.gotev.uploadservice.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig
import net.gotev.uploadservice.UploadTask
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.logger.UploadServiceLogger.NA
import net.gotev.uploadservice.observer.task.UploadTaskObserver

// constants used in the intent which starts this service
private const val taskParametersKey = "taskParameters"
private const val taskNotificationConfig = "taskUploadConfig"

private const val actionKey = "action"
private const val uploadIdKey = "uploadId"
private const val cancelUploadAction = "cancelUpload"

fun Context.startNewUpload(
    params: UploadTaskParameters,
    notificationConfig: UploadNotificationConfig
): String {
    val intent = Intent(this, UploadService::class.java).apply {
        action = UploadServiceConfig.uploadAction
        putExtra(taskParametersKey, params)
        putExtra(taskNotificationConfig, notificationConfig)
    }

    if (Build.VERSION.SDK_INT >= 26) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }

    return params.id
}

data class UploadTaskCreationParameters(
    val params: UploadTaskParameters,
    val notificationConfig: UploadNotificationConfig
)

@Suppress("UNCHECKED_CAST")
fun Intent?.getUploadTaskCreationParameters(): UploadTaskCreationParameters? {
    if (this == null || action != UploadServiceConfig.uploadAction) {
        UploadServiceLogger.error(UploadService.TAG, NA) { "Error while instantiating new task. Invalid intent received" }
        return null
    }

    val params: UploadTaskParameters = getParcelableExtra(taskParametersKey) ?: run {
        UploadServiceLogger.error(UploadService.TAG, NA) { "Error while instantiating new task. Missing task parameters." }
        return null
    }

    val taskClass = try {
        Class.forName(params.taskClass)
    } catch (exc: Throwable) {
        UploadServiceLogger.error(UploadService.TAG, NA, exc) { "Error while instantiating new task. ${params.taskClass} does not exist." }
        null
    } ?: return null

    if (!UploadTask::class.java.isAssignableFrom(taskClass)) {
        UploadServiceLogger.error(UploadService.TAG, NA) { "Error while instantiating new task. ${params.taskClass} does not extend UploadTask." }
        return null
    }

    val notificationConfig: UploadNotificationConfig = getParcelableExtra(taskNotificationConfig) ?: run {
        UploadServiceLogger.error(UploadService.TAG, NA) { "Error while instantiating new task. Missing notification config." }
        return null
    }

    return UploadTaskCreationParameters(
        params = params,
        notificationConfig = notificationConfig
    )
}

/**
 * Creates a new task instance based on the requested task class in the intent.
 * @param intent intent passed to the service
 * @return task instance or null if the task class is not supported or invalid
 */
@Suppress("UNCHECKED_CAST")
fun Context.getUploadTask(
    creationParameters: UploadTaskCreationParameters,
    notificationId: Int,
    vararg observers: UploadTaskObserver
): UploadTask? {
    return try {
        val taskClass = Class.forName(creationParameters.params.taskClass) as Class<out UploadTask>
        val uploadTask = taskClass.newInstance().apply {
            init(
                context = this@getUploadTask,
                taskParams = creationParameters.params,
                notificationConfig = creationParameters.notificationConfig,
                notificationId = notificationId,
                taskObservers = observers
            )
        }

        UploadServiceLogger.debug(UploadService.TAG, NA) { "Successfully created new task with class: ${taskClass.name}" }
        uploadTask
    } catch (exc: Throwable) {
        UploadServiceLogger.error(UploadService.TAG, NA, exc) { "Error while instantiating new task" }
        null
    }
}

fun Context.getNotificationActionIntent(
    uploadId: String,
    action: String
): PendingIntent {
    val intent = Intent(UploadServiceConfig.broadcastNotificationAction).apply {
        `package` = UploadServiceConfig.namespace
        putExtra(actionKey, action)
        putExtra(uploadIdKey, uploadId)
    }

    return PendingIntent.getBroadcast(
        this,
        // this is to prevent duplicate PendingIntent request codes which can cause cancelling
        // the wrong upload
        uploadId.hashCode(),
        intent,
        PendingIntent.FLAG_ONE_SHOT
    )
}

fun Context.getCancelUploadIntent(uploadId: String) =
    getNotificationActionIntent(uploadId, cancelUploadAction)

val Intent.uploadIdToCancel: String?
    get() {
        if (getStringExtra(actionKey) != cancelUploadAction) return null
        return getStringExtra(uploadIdKey)
    }
