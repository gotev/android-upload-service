package net.gotev.uploadservice;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import net.gotev.uploadservice.http.HttpStack;
import net.gotev.uploadservice.http.impl.HurlStack;

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
 * @author gotev (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 * @author mabdurrahman
 */
public final class UploadService extends Service {

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
    public static String NAMESPACE = "net.gotev";

    /**
     * Sets the Http Stack to use to perform upload requests.
     * By default {@link HurlStack} implementation is used.
     */
    public static HttpStack HTTP_STACK = new HurlStack();
    // end configurable values

    protected static final int UPLOAD_NOTIFICATION_BASE_ID = 1234; // Something unique

    /**
     * The minimum interval between progress reports in milliseconds.
     * If the upload Tasks report more frequently, we will throttle notifications.
     * We aim for 6 updates per second.
     */
    protected static final long PROGRESS_REPORT_INTERVAL = 166;

    // constants used in the intent which starts this service
    private static final String ACTION_UPLOAD_SUFFIX = ".uploadservice.action.upload";
    protected static final String PARAM_TASK_PARAMETERS = "taskParameters";
    protected static final String PARAM_TASK_CLASS = "taskClass";

    // constants used in broadcast intents
    private static final String BROADCAST_ACTION_SUFFIX = ".uploadservice.broadcast.status";
    protected static final String PARAM_BROADCAST_DATA = "broadcastData";

    // internal variables
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
            return shutdownIfThereArentAnyActiveTasks();
        }

        Logger.info(TAG, String.format("Starting service with namespace: %s, " +
                "upload pool size: %d, %ds idle thread keep alive time. Foreground execution is %s",
                NAMESPACE, UPLOAD_POOL_SIZE, KEEP_ALIVE_TIME_IN_SECONDS,
                (EXECUTE_IN_FOREGROUND ? "enabled" : "disabled")));

        HttpUploadTask currentTask = getTask(intent);

        if (currentTask == null) {
            return shutdownIfThereArentAnyActiveTasks();
        }

        // increment by 2 because the notificationIncrementalId + 1 is used internally
        // in each HttpUploadTask. Check its sources for more info about this.
        notificationIncrementalId += 2;
        currentTask.setLastProgressNotificationTime(0)
                   .setNotificationId(UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId);

        wakeLock.acquire();

        uploadTasksMap.put(currentTask.params.getId(), currentTask);
        uploadThreadPool.execute(currentTask);

        return START_STICKY;
    }

    private int shutdownIfThereArentAnyActiveTasks() {
        if (uploadTasksMap.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAllUploads();
        uploadThreadPool.shutdown();

        if (EXECUTE_IN_FOREGROUND) {
            Logger.debug(TAG, "Stopping foreground execution");
            stopForeground(true);
        }

        Logger.debug(TAG, "UploadService destroyed");
    }

    /**
     * Creates a new task instance based on the requested task class in the intent.
     * @param intent intent passed to the service
     * @return task instance or null if the task class is not supported or invalid
     */
    HttpUploadTask getTask(Intent intent) {
        String taskClass = intent.getStringExtra(PARAM_TASK_CLASS);

        if (taskClass == null) {
            return null;
        }

        HttpUploadTask uploadTask = null;

        try {
            Class<?> task = Class.forName(taskClass);

            if (HttpUploadTask.class.isAssignableFrom(task)) {
                uploadTask = HttpUploadTask.class.cast(task.newInstance());
                uploadTask.init(this, intent);
            } else {
                Logger.error(TAG, taskClass + " does not extend HttpUploadTask!");
            }

            Logger.debug(TAG, "Successfully created new task with class: " + taskClass);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while instantiating new task", exc);
        }

        return uploadTask;
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
            Logger.debug(TAG, uploadId + " now holds the foreground notification");
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
        if (EXECUTE_IN_FOREGROUND && task != null && task.params.getId().equals(foregroundUploadId)) {
            Logger.debug(TAG, uploadId + " now un-holded the foreground notification");
            foregroundUploadId = null;
        }

        // when all the upload tasks are completed, release the wake lock and shut down the service
        if (uploadTasksMap.isEmpty()) {
            Logger.debug(TAG, "All tasks finished. UploadService is about to shutdown...");
            wakeLock.release();
            stopSelf();
        }
    }
}
