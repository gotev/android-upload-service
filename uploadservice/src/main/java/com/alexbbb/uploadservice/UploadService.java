package com.alexbbb.uploadservice;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.Iterator;
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
 * @author mabdurrahman
 */
public class UploadService extends Service {

    private static final String TAG = UploadService.class.getSimpleName();

    // configurable values
    /**
     * Sets how many threads to use to handle concurrent uploads.
     */
    public static int UPLOAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * When the number of threads is greater than UPLOAD_POOL_SIZE, this is the maximum time that
     * excess idle threads will wait for new tasks before terminating.
     */
    public static int KEEP_ALIVE_TIME_IN_SECONDS = 1;

    /**
     * If set to true, the service will go in foreground mode when doing uploads,
     * lowering the probability of being killed by the system on low memory.
     * This setting is used only when your uploads have a notification configuration.
     * It's not possible to run in foreground without notifications, as per Android policy
     * constraints, so if you set this to true, but you do upload tasks without a
     * notification configuration, the service will simply run in background mode.
     */
    public static boolean EXECUTE_IN_FOREGROUND = true;

    /**
     * Sets the namespace used to broadcast events. Set this to your app namespace to avoid
     * conflicts and unexpected behaviours.
     */
    public static String NAMESPACE = "com.alexbbb";
    // end configurable values

    protected static final int UPLOAD_NOTIFICATION_BASE_ID = 1234; // Something unique

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
    public static final int STATUS_CANCELLED = 4;
    public static final String PROGRESS = "progress";
    public static final String PROGRESS_UPLOADED_BYTES = "progressUploadedBytes";
    public static final String PROGRESS_TOTAL_BYTES = "progressTotalBytes";
    public static final String ERROR_EXCEPTION = "errorException";
    public static final String SERVER_RESPONSE_CODE = "serverResponseCode";
    public static final String SERVER_RESPONSE_MESSAGE = "serverResponseMessage";

    private PowerManager.WakeLock wakeLock;
    private int notificationIncrementalId = 0;
    private static final Map<String, HttpUploadTask> uploadTasksMap = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> uploadTasksQueue = new LinkedBlockingQueue<>();
    private static volatile String foregroundUploadId = null;
    private ThreadPoolExecutor uploadThreadPool;

    protected static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    protected static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    /**
     * Stops the upload task with the given uploadId.
     * @param uploadId The unique upload id
     */
    public static synchronized void stopUpload(final String uploadId) {
        HttpUploadTask removedTask = uploadTasksMap.get(uploadId);
        if (removedTask != null) {
            removedTask.cancel();
        }
    }

    /**
     * Stop all the active uploads.
     */
    public static synchronized void stopAllUploads() {
        if (uploadTasksMap.isEmpty()) {
            return;
        }

        // using iterator instead for each loop, because it's faster on Android
        Iterator<String> iterator = uploadTasksMap.keySet().iterator();

        while (iterator.hasNext()) {
            HttpUploadTask taskToCancel = uploadTasksMap.get(iterator.next());
            taskToCancel.cancel();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        if (UPLOAD_POOL_SIZE <= 0) {
            UPLOAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
        }

        // Creates a thread pool manager
        uploadThreadPool = new ThreadPoolExecutor(
                UPLOAD_POOL_SIZE,       // Initial pool size
                UPLOAD_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME_IN_SECONDS,
                TimeUnit.SECONDS,
                uploadTasksQueue);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !getActionUpload().equals(intent.getAction())) {
            return START_STICKY;
        }

        HttpUploadTask currentTask = getTask(intent);

        if (currentTask == null) {
            return START_STICKY;
        }

        // increment by 2 because the notificationIncrementalId + 1 is used internally
        // in each HttpUploadTask. Check its sources for more info about this.
        notificationIncrementalId += 2;
        currentTask.setLastProgressNotificationTime(0)
                   .setNotificationId(UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId);

        wakeLock.acquire();

        uploadTasksMap.put(currentTask.uploadId, currentTask);
        uploadThreadPool.execute(currentTask);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAllUploads();
        uploadThreadPool.shutdown();

        if (EXECUTE_IN_FOREGROUND) {
            stopForeground(true);
        }

        Log.d(TAG, "UploadService destroyed");
    }

    /**
     * Creates a new task instance based on the requested task type in the intent.
     * @param intent
     * @return task instance or null if the type is not supported or invalid
     */
    HttpUploadTask getTask(Intent intent) {
        String type = intent.getStringExtra(PARAM_TYPE);

        if (MultipartUploadRequest.NAME.equals(type)) {
            return new MultipartUploadTask(this, intent);
        }

        if (BinaryUploadRequest.NAME.equals(type)) {
            return new BinaryUploadTask(this, intent);
        }

        return null;
    }

    /**
     * Check if the task is currently the one shown in the foreground notification.
     * @param uploadId ID of the upload
     * @return true if the current upload task holds the foreground notification, otherwise false
     */
    protected synchronized boolean holdForegroundNotification(String uploadId, Notification notification) {
        if (!EXECUTE_IN_FOREGROUND) return false;

        if (foregroundUploadId == null) {
            foregroundUploadId = uploadId;
            Log.d(TAG, uploadId + " now holds the foreground notification");
        }

        if (uploadId.equals(foregroundUploadId)) {
            startForeground(UPLOAD_NOTIFICATION_BASE_ID, notification);
            return true;
        }

        return false;
    }

    /**
     * Called by each task when it is completed (either successfully, with an error or due to
     * user cancellation).
     * @param uploadId the uploadID of the finished task
     */
    protected synchronized void taskCompleted(String uploadId) {
        HttpUploadTask task = uploadTasksMap.remove(uploadId);

        // un-hold foreground upload ID if it's been hold
        if (EXECUTE_IN_FOREGROUND && task != null && task.uploadId.equals(foregroundUploadId)) {
            Log.d(TAG, uploadId + " now un-holded the foreground notification");
            foregroundUploadId = null;
        }

        // when all the upload tasks are completed, release the wake lock and shut down the service
        if (uploadTasksMap.isEmpty()) {
            Log.d(TAG, "All tasks finished. UploadService is about to shutdown...");
            wakeLock.release();
            stopSelf();
        }
    }
}
