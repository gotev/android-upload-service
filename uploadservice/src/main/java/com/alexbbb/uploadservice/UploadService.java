package com.alexbbb.uploadservice;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service to upload files in background using HTTP POST with notification center progress
 * display.
 *
 * @author alexbbb (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class UploadService extends Service {

    private static final String TAG = UploadService.class.getSimpleName();

    private static final int UPLOAD_NOTIFICATION_BASE_ID = 1234; // Something unique

    public static String NAMESPACE = "com.alexbbb";
    public static int UPLOAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final String ACTION_UPLOAD_SUFFIX = ".uploadservice.action.upload";
    protected static final String PARAM_NOTIFICATION_CONFIG = "notificationConfig";
    protected static final String PARAM_ID = "id";
    protected static final String PARAM_URL = "url";
    protected static final String PARAM_METHOD = "method";
    protected static final String PARAM_FILES = "files";
    protected static final String PARAM_FILE = "file";
    protected static final String PARAM_TYPE = "uploadType";

    protected static final String PARAM_REQUEST_HEADERS = "requestHeaders";
    protected static final String PARAM_REQUEST_PARAMETERS = "requestParameters";
    protected static final String PARAM_CUSTOM_USER_AGENT = "customUserAgent";
    protected static final String PARAM_MAX_RETRIES = "maxRetries";

    protected static final String UPLOAD_BINARY = "binary";
    protected static final String UPLOAD_MULTIPART = "multipart";

    /**
     * The minimum interval between progress reports in milliseconds.
     * If the upload Tasks report more frequently, we will throttle notifications.
     * We aim for 6 updates per second.
     */
    protected static final long PROGRESS_REPORT_INTERVAL = 166;

    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";
    public static final String UPLOAD_ID = "id";
    public static final String STATUS = "status";
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_ERROR = 3;
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_UPLOADED_BYTES = "progressUploadedBytes";
    public static final String PROGRESS_TOTAL_BYTES = "progressTotalBytes";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private static final String FOREGROUND_ACTION_SUFFIX = ".uploadservice.foreground";

    private int notificationIncremental = 1;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;

    private boolean takeoverForegroundNotification;
    private NotificationCompat.Builder tempNotificationToDisplay;

    private static final int KEEP_ALIVE_TIME = 1;

    private static final Map<String, HttpUploadTask> uploadTasksMap = new ConcurrentHashMap<>();

    // A queue of Runnable(s)
    private static final BlockingQueue<Runnable> uploadTasksQueue = new LinkedBlockingQueue<>();
    private static ThreadPoolExecutor uploadThreadPool;

    protected static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    protected static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    protected static String getActionForeground() {
        return NAMESPACE + FOREGROUND_ACTION_SUFFIX;
    }

    /**
     * Stops the upload task with the given uploadId.
     * @param uploadId The unique upload id
     * @return {@code true} if an upload task was actually stopped, Otherwise {@code false}.
     */
    public synchronized static boolean stopUpload(final String uploadId) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return false;

        uploadTask.cancel();
        return true;
    }

    /**
     * Stops all upload tasks.
     * @return {@code true} if at least one upload task was actually stopped, Otherwise {@code false}.
     */
    public synchronized static boolean stopAllUploads() {
        boolean result = false;
        for (HttpUploadTask uploadTask : uploadTasksMap.values()) {
            uploadTask.cancel();
            result = true;
        }
        return result;
    }

    public UploadService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        if (UPLOAD_POOL_SIZE <= 0) {
            UPLOAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
        }
        // Creates a thread pool manager
        uploadThreadPool = new ThreadPoolExecutor(
                UPLOAD_POOL_SIZE,       // Initial pool size
                UPLOAD_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                uploadTasksQueue);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String action = intent.getAction();

            if (getActionUpload().equals(action)) {
                UploadNotificationConfig notificationConfig = intent.getParcelableExtra(PARAM_NOTIFICATION_CONFIG);

                HttpUploadTask newTask = null;

                String type = intent.getStringExtra(PARAM_TYPE);
                if (UPLOAD_MULTIPART.equals(type)) {
                    newTask = new MultipartUploadTask(this, intent);
                } else if (UPLOAD_BINARY.equals(type)) {
                    newTask = new BinaryUploadTask(this, intent);
                }

                if (newTask != null) {
                    if (shouldTakeoverForegroundNotification() || getRemainingTasks() == 0) {
                        takeoverForegroundNotification = false;

                        newTask.setNotificationId(0);
                    } else {
                        newTask.setNotificationId(notificationIncremental++);
                    }
                    newTask.setNotificationConfig(notificationConfig);

                    uploadTasksMap.put(newTask.uploadId, newTask);

                    wakeLock.acquire();

                    createNotification(newTask.uploadId);

                    uploadThreadPool.execute(newTask);
                }
            }
        }
        return START_STICKY;
    }

    synchronized void broadcastProgress(final String uploadId, final long uploadedBytes, final long totalBytes) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null)
            return;

        long currentTime = System.currentTimeMillis();
        if (currentTime < uploadTask.getLastProgressNotificationTime() + PROGRESS_REPORT_INTERVAL) {
            return;
        }

        uploadTask.setLastProgressNotificationTime(currentTime);

        updateNotificationProgress(uploadId, (int) uploadedBytes, (int) totalBytes);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_IN_PROGRESS);

        final int percentsProgress = (int) (uploadedBytes * 100 / totalBytes);
        intent.putExtra(PROGRESS, percentsProgress);

        intent.putExtra(PROGRESS_UPLOADED_BYTES, uploadedBytes);
        intent.putExtra(PROGRESS_TOTAL_BYTES, totalBytes);
        sendBroadcast(intent);
    }

    synchronized void broadcastCompleted(final String uploadId, final int responseCode, final String responseMessage) {
        final String filteredMessage;
        if (responseMessage == null) {
            filteredMessage = "";
        } else {
            filteredMessage = responseMessage;
        }

        if (responseCode >= 200 && responseCode <= 299)
            updateNotificationCompleted(uploadId);
        else
            updateNotificationError(uploadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_COMPLETED);
        intent.putExtra(SERVER_RESPONSE_CODE, responseCode);
        intent.putExtra(SERVER_RESPONSE_MESSAGE, filteredMessage);
        sendBroadcast(intent);

        uploadTasksMap.remove(uploadId);
    }

    synchronized void broadcastError(final String uploadId, final Exception exception) {
        updateNotificationError(uploadId);

        final Intent intent = new Intent(getActionBroadcast());
        intent.setAction(getActionBroadcast());
        intent.putExtra(UPLOAD_ID, uploadId);
        intent.putExtra(STATUS, STATUS_ERROR);
        intent.putExtra(ERROR_EXCEPTION, exception);
        sendBroadcast(intent);

        uploadTasksMap.remove(uploadId);
    }

    private void createNotification(final String uploadId) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return;

        UploadNotificationConfig notificationConfig = uploadTask.getNotificationConfig();
        if (notificationConfig == null) return;

        if (getRemainingTasks() == 0) {
            startForeground(UPLOAD_NOTIFICATION_BASE_ID, new NotificationCompat.Builder(this).build());
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle(notificationConfig.getTitle())
                .setContentText(notificationConfig.getInProgressMessage())
                .setContentIntent(notificationConfig.getPendingIntent(this))
                .setSmallIcon(notificationConfig.getIconResourceID())
                .setProgress(100, 0, true)
                .setOngoing(true);

        notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + uploadTask.getNotificationId(), notification.build());
    }

    private void updateNotificationProgress(final String uploadId, int uploadedBytes, int totalBytes) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return;

        UploadNotificationConfig notificationConfig = uploadTask.getNotificationConfig();
        if (notificationConfig == null) return;

        int oldNotificationId = -1;
        if (shouldTakeoverForegroundNotification()) {
            oldNotificationId = uploadTask.getNotificationId();

            takeoverForegroundNotification = false;
            uploadTask.setNotificationId(0);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentTitle(notificationConfig.getTitle())
                .setContentText(notificationConfig.getInProgressMessage())
                .setContentIntent(notificationConfig.getPendingIntent(this))
                .setSmallIcon(notificationConfig.getIconResourceID())
                .setProgress(totalBytes, uploadedBytes, false)
                .setOngoing(true);

        if (oldNotificationId != -1) {
            if (tempNotificationToDisplay != null) {
                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + oldNotificationId, tempNotificationToDisplay.build());

                tempNotificationToDisplay = null;
            } else {
                notificationManager.cancel(UPLOAD_NOTIFICATION_BASE_ID + oldNotificationId);
            }
        }
        notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + uploadTask.getNotificationId(), notification.build());
    }

    private void updateNotificationCompleted(final String uploadId) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return;

        UploadNotificationConfig notificationConfig = uploadTask.getNotificationConfig();
        if (notificationConfig == null) return;

        if (getRemainingTasks() <= 1) {
            stopForeground((uploadTask.getNotificationId() == 0 && notificationConfig.isAutoClearOnSuccess()) || shouldTakeoverForegroundNotification());

            takeoverForegroundNotification = false;

            if (tempNotificationToDisplay != null) {
                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + notificationIncremental++, tempNotificationToDisplay.build());

                tempNotificationToDisplay = null;
            }

            wakeLock.release();
        }

        if (uploadTask.getNotificationId() != 0) {
            if (!notificationConfig.isAutoClearOnSuccess()) {
                NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                        .setContentTitle(notificationConfig.getTitle())
                        .setContentText(notificationConfig.getCompletedMessage())
                        .setContentIntent(notificationConfig.getPendingIntent(this))
                        .setAutoCancel(notificationConfig.isClearOnAction())
                        .setSmallIcon(notificationConfig.getIconResourceID())
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                setRingtone(uploadId, notification);

                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + uploadTask.getNotificationId(), notification.build());
            } else {
                notificationManager.cancel(UPLOAD_NOTIFICATION_BASE_ID + uploadTask.getNotificationId());
            }
        } else {
            if (!notificationConfig.isAutoClearOnSuccess()) {
                tempNotificationToDisplay = new NotificationCompat.Builder(this)
                        .setContentTitle(notificationConfig.getTitle())
                        .setContentText(notificationConfig.getCompletedMessage())
                        .setContentIntent(notificationConfig.getPendingIntent(this))
                        .setAutoCancel(notificationConfig.isClearOnAction())
                        .setSmallIcon(notificationConfig.getIconResourceID())
                        .setProgress(0, 0, false)
                        .setOngoing(false);
                setRingtone(uploadId, tempNotificationToDisplay);

                if (getRemainingTasks() <= 1) {
                    notificationManager.cancel(UPLOAD_NOTIFICATION_BASE_ID);

                    notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + notificationIncremental++, tempNotificationToDisplay.build());

                    tempNotificationToDisplay = null;
                }
            }

            takeoverForegroundNotification = true;
        }
    }

    private void updateNotificationError(final String uploadId) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return;

        UploadNotificationConfig notificationConfig = uploadTask.getNotificationConfig();
        if (notificationConfig == null) return;

        if (shouldTakeoverForegroundNotification()) {
            int oldNotificationId = uploadTask.getNotificationId();

            takeoverForegroundNotification = false;
            uploadTask.setNotificationId(0);

            if (tempNotificationToDisplay != null) {
                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + oldNotificationId, tempNotificationToDisplay.build());

                tempNotificationToDisplay = null;
            } else {
                notificationManager.cancel(UPLOAD_NOTIFICATION_BASE_ID + oldNotificationId);
            }
        }

        if (getRemainingTasks() <= 1) {
            stopForeground(false);

            takeoverForegroundNotification = false;

            if (tempNotificationToDisplay != null) {
                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + notificationIncremental++, tempNotificationToDisplay.build());

                tempNotificationToDisplay = null;
            }

            wakeLock.release();
        }

        if (uploadTask.getNotificationId() != 0) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                    .setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getErrorMessage())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setAutoCancel(notificationConfig.isClearOnAction())
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            setRingtone(uploadId, notification);
            notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + uploadTask.getNotificationId(), notification.build());
        } else {
            tempNotificationToDisplay = new NotificationCompat.Builder(this)
                    .setContentTitle(notificationConfig.getTitle())
                    .setContentText(notificationConfig.getCompletedMessage())
                    .setContentIntent(notificationConfig.getPendingIntent(this))
                    .setAutoCancel(notificationConfig.isClearOnAction())
                    .setSmallIcon(notificationConfig.getIconResourceID())
                    .setProgress(0, 0, false)
                    .setOngoing(false);
            setRingtone(uploadId, tempNotificationToDisplay);

            if (getRemainingTasks() <= 1) {
                notificationManager.cancel(UPLOAD_NOTIFICATION_BASE_ID);

                notificationManager.notify(UPLOAD_NOTIFICATION_BASE_ID + notificationIncremental++, tempNotificationToDisplay.build());

                tempNotificationToDisplay = null;
            } else {
                takeoverForegroundNotification = true;
            }
        }
    }

    private void setRingtone(final String uploadId, NotificationCompat.Builder notification) {
        HttpUploadTask uploadTask = uploadTasksMap.get(uploadId);
        if (uploadTask == null) return;

        UploadNotificationConfig notificationConfig = uploadTask.getNotificationConfig();
        if (notificationConfig == null) return;

        if(notificationConfig.isRingToneEnabled()) {
            notification.setSound(RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION));
            notification.setOnlyAlertOnce(false);
        }
    }

    private synchronized boolean shouldTakeoverForegroundNotification() {
        return takeoverForegroundNotification;
    }

    private synchronized long getRemainingTasks() {
        if (uploadTasksMap == null)
            return 0;

        return uploadTasksMap.size();
    }
}
