package net.gotev.uploadservice;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import net.gotev.uploadservice.http.HttpStack;
import net.gotev.uploadservice.http.impl.HurlStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
     * Sets the HTTP Stack to use to perform HTTP based upload requests.
     * By default {@link HurlStack} implementation is used.
     */
    public static HttpStack HTTP_STACK = new HurlStack();

    /**
     * Buffer size in bytes used for data transfer by the upload tasks.
     */
    public static int BUFFER_SIZE = 4096;

    /**
     * Sets the time to wait in milliseconds before the next attempt when an upload fails
     * for the first time. From the second time onwards, this value will be multiplied by
     * {@link UploadService#BACKOFF_MULTIPLIER} to get the time to wait before the next attempt.
     */
    public static int INITIAL_RETRY_WAIT_TIME = 1000;

    /**
     * Sets the backoff timer multiplier. By default is set to 10, so every time that an upload
     * fails, the time to wait between retries will be multiplied by 10.
     * E.g. if the first time the wait time is 1s, the second time it will be 10s and the third
     * time it will be 100s.
     */
    public static int BACKOFF_MULTIPLIER = 10;

    /**
     * Sets the maximum time to wait in milliseconds between two upload attempts.
     * This is useful because every time an upload fails, the wait time gets multiplied by
     * {@link UploadService#BACKOFF_MULTIPLIER} and it's not convenient that the value grows
     * indefinitely.
     */
    public static int MAX_RETRY_WAIT_TIME = 10 * 60 * 1000;
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
    private static final Map<String, UploadTask> uploadTasksMap = new ConcurrentHashMap<>();
    private static final Map<String, UploadStatusDelegate> uploadDelegates = new ConcurrentHashMap<>();
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
        UploadTask removedTask = uploadTasksMap.get(uploadId);
        if (removedTask != null) {
            removedTask.cancel();
        }
    }

    /**
     * Gets the list of the currently active upload tasks.
     * @return list of uploadIDs or an empty list if no tasks are currently running
     */
    public static synchronized List<String> getTaskList() {
        List<String> tasks;

        if (uploadTasksMap.isEmpty()) {
            tasks = new ArrayList<>(1);
        } else {
            tasks = new ArrayList<>(uploadTasksMap.size());
            tasks.addAll(uploadTasksMap.keySet());
        }

        return tasks;
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
            UploadTask taskToCancel = uploadTasksMap.get(iterator.next());
            taskToCancel.cancel();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.setReferenceCounted(false);

        if (!wakeLock.isHeld())
            wakeLock.acquire();

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

        Logger.info(TAG, String.format(Locale.getDefault(), "Starting service with namespace: %s, " +
                "upload pool size: %d, %ds idle thread keep alive time. Foreground execution is %s",
                NAMESPACE, UPLOAD_POOL_SIZE, KEEP_ALIVE_TIME_IN_SECONDS,
                (EXECUTE_IN_FOREGROUND ? "enabled" : "disabled")));

        UploadTask currentTask = getTask(intent);

        if (currentTask == null) {
            return shutdownIfThereArentAnyActiveTasks();
        }

        if (uploadTasksMap.containsKey(currentTask.params.getId())) {
            Logger.error(TAG, "Preventing upload with id: " + currentTask.params.getId()
                    + " to be uploaded twice! Please check your code and fix it!");
            return shutdownIfThereArentAnyActiveTasks();
        }

        // increment by 2 because the notificationIncrementalId + 1 is used internally
        // in each UploadTask. Check its sources for more info about this.
        notificationIncrementalId += 2;
        currentTask.setLastProgressNotificationTime(0)
                   .setNotificationId(UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId);

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

        if (wakeLock.isHeld())
            wakeLock.release();

        uploadTasksMap.clear();
        uploadDelegates.clear();

        Logger.debug(TAG, "UploadService destroyed");
    }

    /**
     * Creates a new task instance based on the requested task class in the intent.
     * @param intent intent passed to the service
     * @return task instance or null if the task class is not supported or invalid
     */
    UploadTask getTask(Intent intent) {
        String taskClass = intent.getStringExtra(PARAM_TASK_CLASS);

        if (taskClass == null) {
            return null;
        }

        UploadTask uploadTask = null;

        try {
            Class<?> task = Class.forName(taskClass);

            if (UploadTask.class.isAssignableFrom(task)) {
                uploadTask = UploadTask.class.cast(task.newInstance());
                uploadTask.init(this, intent);
            } else {
                Logger.error(TAG, taskClass + " does not extend UploadTask!");
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
        UploadTask task = uploadTasksMap.remove(uploadId);
        uploadDelegates.remove(uploadId);

        // un-hold foreground upload ID if it's been hold
        if (EXECUTE_IN_FOREGROUND && task != null && task.params.getId().equals(foregroundUploadId)) {
            Logger.debug(TAG, uploadId + " now un-holded the foreground notification");
            foregroundUploadId = null;
        }

        if (EXECUTE_IN_FOREGROUND && uploadTasksMap.isEmpty()) {
            Logger.debug(TAG, "All tasks completed, stopping foreground execution");
            stopForeground(true);
        }
    }

    /**
     * Sets the delegate which will receive the events for the given upload request.
     * Those events will not be sent in broadcast, but only to the delegate.
     * @param uploadId uploadID of the upload request
     * @param delegate the delegate instance
     */
    protected static void setUploadStatusDelegate(String uploadId, UploadStatusDelegate delegate) {
        if (delegate == null)
            return;

        uploadDelegates.put(uploadId, delegate);
    }

    /**
     * Gets the delegate for an upload request.
     * @param uploadId uploadID of the upload request
     * @return {@link UploadStatusDelegate} or null if no delegate has been set for the given
     * uploadId
     */
    protected static UploadStatusDelegate getUploadStatusDelegate(String uploadId) {
        return uploadDelegates.get(uploadId);
    }
}
