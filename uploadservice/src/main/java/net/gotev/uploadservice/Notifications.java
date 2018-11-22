package net.gotev.uploadservice;

import android.app.Notification;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import static net.gotev.uploadservice.UploadTask.NOTIFICATION_BUNDLED_BASE_ID;

public class Notifications {

    public static final String SUCCESS_TITLE = "Files uploaded";
    public static final String SUCCESS_MESSAGE = "Files uploaded successfully";
    public static final String ONGOING_TITLE = "Uploading Files";

    interface NotificationHandler {
        /**
         * If the upload task is initialized with a notification configuration, this handles its
         * creation.
         */
        void createNotification(UploadInfo uploadInfo, UploadService service, UploadTaskParameters params,
                                NotificationManager notificationManager, long notificationCreationTimeMillis, int notificationId);

        /**
         * Informs the {@link UploadService} that the task has made some progress. You should call this
         * method from your task whenever you have successfully transferred some bytes to the server.
         */
        void updateNotificationProgress(UploadInfo uploadInfo, UploadService service,
                                        UploadTaskParameters params, NotificationManager notificationManager,
                                        long notificationCreationTimeMillis, int notificationId);

        /**
         * Informs the {@link UploadService} that the task has made some progress. You should call this
         * method from your task whenever you have successfully transferred some bytes to the server.
         */
        void updateNotification(UploadInfo uploadInfo, UploadNotificationStatusConfig statusConfig,
                                UploadService service, UploadTaskParameters params,
                                NotificationManager notificationManager, int notificationId);

    }


    protected static void setRingtone(NotificationCompat.Builder notification, UploadService service, UploadTaskParameters params) {

        if (params.notificationConfig.isRingToneEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri sound = RingtoneManager.getActualDefaultRingtoneUri(service, RingtoneManager.TYPE_NOTIFICATION);
            notification.setSound(sound);
        }

    }


}
