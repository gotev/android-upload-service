package net.gotev.uploadservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import static net.gotev.uploadservice.Notifications.setRingtone;

public class NotificationsHandler implements Notifications.NotificationHandler {

    protected static void createItemNotification(UploadNotificationStatusConfig statusConfig, UploadService service,
                                                 UploadTaskParameters params, NotificationManager notificationManager,
                                                 int notificationId, UploadInfo uploadInfo, long notificationCreationTimeMillis) {
        NotificationCompat.Builder notification = createNotification(statusConfig, service, params,
                uploadInfo, notificationCreationTimeMillis);

        notification.setProgress(100, 0, false);

        statusConfig.addActionsToNotificationBuilder(notification);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.id, builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    protected static void updateItemNotification(UploadNotificationStatusConfig statusConfig, UploadService service,
                                                 UploadTaskParameters params, NotificationManager notificationManager,
                                                 int notificationId, UploadInfo uploadInfo, long notificationCreationTimeMillis) {

        NotificationCompat.Builder notification = createNotification(statusConfig, service, params,
                uploadInfo, notificationCreationTimeMillis);

        notification.setProgress((int) uploadInfo.getTotalBytes(), (int) uploadInfo.getUploadedBytes(), false);

        statusConfig.addActionsToNotificationBuilder(notification);

        Notification builtNotification = notification.build();

        if (service.holdForegroundNotification(params.id, builtNotification)) {
            notificationManager.cancel(notificationId);
        } else {
            notificationManager.notify(notificationId, builtNotification);
        }
    }

    private static NotificationCompat.Builder createNotification(UploadNotificationStatusConfig statusConfig, UploadService service,
                                                                 UploadTaskParameters params, UploadInfo uploadInfo, long notificationCreationTimeMillis) {
        return new NotificationCompat.Builder(service, params.notificationConfig.getNotificationChannelId())
                .setWhen(notificationCreationTimeMillis)
                .setContentTitle(Placeholders.replace(statusConfig.title, uploadInfo))
                .setContentText(Placeholders.replace(statusConfig.message, uploadInfo))
                .setContentIntent(statusConfig.getClickIntent(service))
                .setSmallIcon(statusConfig.iconResourceID)
                .setLargeIcon(statusConfig.largeIcon)
                .setColor(statusConfig.iconColorResourceID)
                .setGroup(UploadService.NAMESPACE)
                .setOngoing(true);
    }

    @Override
    public void createNotification(UploadInfo uploadInfo, UploadService service, UploadTaskParameters params, NotificationManager notificationManager, long notificationCreationTimeMillis, int notificationId) {
        if (params.notificationConfig == null || params.notificationConfig.getProgress().message == null)
            return;

        UploadNotificationStatusConfig statusConfig = params.notificationConfig.getProgress();
        notificationCreationTimeMillis = System.currentTimeMillis();


        createItemNotification(statusConfig, service, params, notificationManager,
                notificationId, uploadInfo, notificationCreationTimeMillis);

    }

    @Override
    public void updateNotificationProgress(UploadInfo uploadInfo, UploadService service,
                                           UploadTaskParameters params, NotificationManager notificationManager,
                                           long notificationCreationTimeMillis, int notificationId) {

        if (params.notificationConfig == null || params.notificationConfig.getProgress().message == null)
            return;

        UploadNotificationStatusConfig statusConfig = params.notificationConfig.getProgress();

        updateItemNotification(statusConfig, service, params, notificationManager,
                notificationId, uploadInfo, notificationCreationTimeMillis);
    }

    @Override
    public void updateNotification(UploadInfo uploadInfo, UploadNotificationStatusConfig statusConfig,
                                   UploadService service, UploadTaskParameters params,
                                   NotificationManager notificationManager, int notificationId) {
        if (params.notificationConfig == null) return;

        notificationManager.cancel(notificationId);

        if (statusConfig.message == null) return;

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
