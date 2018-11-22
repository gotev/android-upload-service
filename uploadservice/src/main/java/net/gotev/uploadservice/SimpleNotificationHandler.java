package net.gotev.uploadservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import static net.gotev.uploadservice.Notifications.ONGOING_TITLE;
import static net.gotev.uploadservice.Notifications.SUCCESS_MESSAGE;
import static net.gotev.uploadservice.Notifications.SUCCESS_TITLE;
import static net.gotev.uploadservice.Notifications.setRingtone;
import static net.gotev.uploadservice.UploadTask.NOTIFICATION_BUNDLED_BASE_ID;

public class SimpleNotificationHandler implements Notifications.NotificationHandler {

    protected static void createSimplifiedNotification(UploadNotificationStatusConfig statusConfig, UploadService service, UploadTaskParameters params, NotificationManager notificationManager) {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                .setContentTitle(ONGOING_TITLE)
                .setContentText("Uploading " + (service.getFilesCount() - service.getFilesUploaded()) + " files")
                .setSmallIcon(statusConfig.iconResourceID)
                .setOngoing(true)
                .setGroup(UploadService.NAMESPACE)
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setProgress(service.getFilesCount(), service.getFilesUploaded(), false);

        statusConfig.addActionsToNotificationBuilder(summaryBuilder);

        Notification notification = summaryBuilder.build();
        if (service.holdForegroundNotification(String.valueOf(params.id), notification)) {
            notificationManager.cancel(NOTIFICATION_BUNDLED_BASE_ID);
        } else {
            notificationManager.notify(NOTIFICATION_BUNDLED_BASE_ID, notification);
        }
    }

    protected static void createSimplifiedCompletedNotification(UploadNotificationStatusConfig statusConfig, UploadService service, UploadTaskParameters params, NotificationManager notificationManager) {
        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                .setContentTitle(SUCCESS_TITLE)
                .setContentText(SUCCESS_MESSAGE)
                .setSmallIcon(statusConfig.iconResourceID)
                .setGroup(UploadService.NAMESPACE)
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setOngoing(false)
                .setProgress(0, 0, false);
        Notification notification = summaryBuilder.build();
        notificationManager.notify(NOTIFICATION_BUNDLED_BASE_ID, notification);
    }
    @Override
    public void createNotification(UploadInfo uploadInfo, UploadService service, UploadTaskParameters params,
                                   NotificationManager notificationManager, long notificationCreationTimeMillis, int notificationId) {
        if (params.notificationConfig == null || params.notificationConfig.getProgress().message == null)
            return;

        UploadNotificationStatusConfig statusConfig = params.notificationConfig.getProgress();

        createSimplifiedNotification(statusConfig, service, params, notificationManager);
    }

    @Override
    public void updateNotificationProgress(UploadInfo uploadInfo, UploadService service, UploadTaskParameters params, NotificationManager notificationManager, long notificationCreationTimeMillis, int notificationId) {
        createNotification(uploadInfo, service, params, notificationManager, notificationCreationTimeMillis, notificationId);
    }

    @Override
    public void updateNotification(UploadInfo uploadInfo, UploadNotificationStatusConfig statusConfig,
                                   UploadService service, UploadTaskParameters params,
                                   NotificationManager notificationManager, int notificationId) {
        if (params.notificationConfig == null) return;

        notificationManager.cancel(notificationId);

        if (statusConfig.message == null) return;

        if (statusConfig.message.contains(Placeholders.ELAPSED_TIME) && params.notificationConfig.isSimplifiedNotification()) {
            service.fileUploaded();
            if (service.getFilesCount() == service.getFilesUploaded()) {
                createSimplifiedCompletedNotification(statusConfig, service, params, notificationManager);
            } else {
                createSimplifiedNotification(statusConfig, service, params, notificationManager);
            }
        }

        if (!statusConfig.autoClear) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                    .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                    .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                    .setContentIntent(statusConfig.getClickIntent(service))
                    .setAutoCancel(statusConfig.clearOnAction)
                    .setSmallIcon(statusConfig.iconResourceID)
                    .setLargeIcon(statusConfig.largeIcon)
                    .setColor(statusConfig.iconColorResourceID)
                    .setGroup(UploadService.NAMESPACE)
                    .setProgress(0, 0, false)
                    .setOngoing(false);

            statusConfig.addActionsToNotificationBuilder(notification);

            setRingtone(notification, service, params);

            // this is needed because the main notification used to show progress is ongoing
            // and a new one has to be created to allow the user to dismiss it
            uploadInfo.setNotificationID(notificationId + 1);
            notificationManager.notify(notificationId + 1, notification.build());
        }
    }
}
