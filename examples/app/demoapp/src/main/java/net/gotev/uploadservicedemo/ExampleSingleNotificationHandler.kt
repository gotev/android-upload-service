package net.gotev.uploadservicedemo

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.observer.task.AbstractSingleNotificationHandler

class ExampleSingleNotificationHandler(service: UploadService) :
    AbstractSingleNotificationHandler(service) {
    override fun updateNotification(
        notificationManager: NotificationManager,
        notificationBuilder: NotificationCompat.Builder,
        tasks: Map<String, TaskData>
    ): NotificationCompat.Builder? {
        // Return null to not update the notification
        return notificationBuilder
            .setContentTitle("${tasks.size} Uploads")
            .setContentText("${tasks.values.count { it.status == TaskStatus.InProgress }} in progress")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
    }
}
