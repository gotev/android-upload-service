package net.gotev.uploadservice;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

import net.gotev.uploadservice.logger.UploadServiceLogger;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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

    protected static final int UPLOAD_NOTIFICATION_BASE_ID = 1234; // Something unique

    // constants used in the intent which starts this service
    protected static final String PARAM_TASK_PARAMETERS = "taskParameters";
    protected static final String PARAM_TASK_CLASS = "taskClass";

    // internal variables
    private PowerManager.WakeLock wakeLock;
    private static int notificationIncrementalId = 0;
    private static final Map<String, UploadTask> uploadTasksMap = new ConcurrentHashMap<>();
    private static final Map<String, WeakReference<UploadStatusDelegate>> uploadDelegates = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> uploadTasksQueue = new LinkedBlockingQueue<>();
    private static volatile String foregroundUploadId = null;
    private ThreadPoolExecutor uploadThreadPool;
    private Timer idleTimer = null;

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

    /**
     * Stops the service if no upload tasks are currently running
     * @param context application context
     * @return true if the service is getting stopped, false otherwise
     */
    public static synchronized boolean stop(final Context context) {
        return stop(context, false);
    }

    /**
     * Stops the service.
     * @param context application context
     * @param forceStop stops the service no matter if some tasks are running
     * @return true if the service is getting stopped, false otherwise
     */
    public static synchronized boolean stop(final Context context, boolean forceStop) {
        if (forceStop) {
            return context.stopService(new Intent(context, UploadService.class));
        }
        return uploadTasksMap.isEmpty() && context.stopService(new Intent(context, UploadService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.setReferenceCounted(false);

        if (!wakeLock.isHeld())
            wakeLock.acquire();

        // Creates a thread pool manager
        uploadThreadPool = new ThreadPoolExecutor(
                UploadServiceConfig.INSTANCE.getUploadPoolSize(),       // Initial pool size
                UploadServiceConfig.INSTANCE.getUploadPoolSize(),       // Max pool size
                UploadServiceConfig.INSTANCE.getKeepAliveTimeSeconds(),
                TimeUnit.SECONDS,
                uploadTasksQueue);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !UploadServiceConfig.INSTANCE.getUploadAction().equals(intent.getAction())) {
            return shutdownIfThereArentAnyActiveTasks();
        }

        UploadServiceLogger.INSTANCE.debug(TAG, "Starting UploadService. Debug info: " + UploadServiceConfig.INSTANCE);

        UploadTask currentTask = getTask(intent);

        if (currentTask == null) {
            return shutdownIfThereArentAnyActiveTasks();
        }

        if (uploadTasksMap.containsKey(currentTask.params.getId())) {
            UploadServiceLogger.INSTANCE.error(TAG, "Preventing upload with id: " + currentTask.params.getId()
                    + " to be uploaded twice! Please check your code and fix it!");
            return shutdownIfThereArentAnyActiveTasks();
        }

        clearIdleTimer();

        // increment by 2 because the notificationIncrementalId + 1 is used internally
        // in each UploadTask. Check its sources for more info about this.
        notificationIncrementalId += 2;
        currentTask.setLastProgressNotificationTime(0)
                   .setNotificationId(UPLOAD_NOTIFICATION_BASE_ID + notificationIncrementalId);

        uploadTasksMap.put(currentTask.params.getId(), currentTask);
        uploadThreadPool.execute(currentTask);

        return START_STICKY;
    }

    synchronized private void clearIdleTimer() {
        if (idleTimer != null) {
            UploadServiceLogger.INSTANCE.info(TAG, "Clearing idle timer");
            idleTimer.cancel();
            idleTimer = null;
        }
    }

    synchronized private int shutdownIfThereArentAnyActiveTasks() {
        if (uploadTasksMap.isEmpty()) {
            clearIdleTimer();

            UploadServiceLogger.INSTANCE.info(TAG, "Service will be shut down in " + UploadServiceConfig.INSTANCE.getIdleTimeoutSeconds() + "s if no new tasks are received");
            idleTimer = new Timer(TAG + "IdleTimer");
            idleTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    UploadServiceLogger.INSTANCE.info(TAG, "Service is about to be stopped because idle timeout of "
                            + UploadServiceConfig.INSTANCE.getIdleTimeoutSeconds() + "s has been reached");
                    stopSelf();
                }
            }, UploadServiceConfig.INSTANCE.getIdleTimeoutSeconds() * 1000);

            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopAllUploads();
        uploadThreadPool.shutdown();

        if (UploadServiceConfig.INSTANCE.isForegroundService()) {
            UploadServiceLogger.INSTANCE.debug(TAG, "Stopping foreground execution");
            stopForeground(true);
        }

        if (wakeLock.isHeld())
            wakeLock.release();

        uploadTasksMap.clear();
        uploadDelegates.clear();

        UploadServiceLogger.INSTANCE.debug(TAG, "UploadService destroyed");
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
                UploadServiceLogger.INSTANCE.error(TAG, taskClass + " does not extend UploadTask!");
            }

            UploadServiceLogger.INSTANCE.debug(TAG, "Successfully created new task with class: " + taskClass);

        } catch (Exception exc) {
            UploadServiceLogger.INSTANCE.error(TAG, "Error while instantiating new task", exc);
        }

        return uploadTask;
    }

    /**
     * Check if the task is currently the one shown in the foreground notification.
     * @param uploadId ID of the upload
     * @return true if the current upload task holds the foreground notification, otherwise false
     */
    public synchronized boolean holdForegroundNotification(String uploadId, Notification notification) {
        if (!UploadServiceConfig.INSTANCE.isForegroundService()) return false;

        if (foregroundUploadId == null) {
            foregroundUploadId = uploadId;
            UploadServiceLogger.INSTANCE.debug(TAG, uploadId + " now holds the foreground notification");
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
        if (UploadServiceConfig.INSTANCE.isForegroundService() && task != null && task.params.getId().equals(foregroundUploadId)) {
            UploadServiceLogger.INSTANCE.debug(TAG, uploadId + " now un-holded the foreground notification");
            foregroundUploadId = null;
        }

        if (UploadServiceConfig.INSTANCE.isForegroundService() && uploadTasksMap.isEmpty()) {
            UploadServiceLogger.INSTANCE.debug(TAG, "All tasks completed, stopping foreground execution");
            stopForeground(true);
            shutdownIfThereArentAnyActiveTasks();
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

        uploadDelegates.put(uploadId, new WeakReference<>(delegate));
    }

    /**
     * Gets the delegate for an upload request.
     * @param uploadId uploadID of the upload request
     * @return {@link UploadStatusDelegate} or null if no delegate has been set for the given
     * uploadId
     */
    protected static UploadStatusDelegate getUploadStatusDelegate(String uploadId) {
        WeakReference<UploadStatusDelegate> reference = uploadDelegates.get(uploadId);

        if (reference == null)
            return null;

        UploadStatusDelegate delegate = reference.get();

        if (delegate == null) {
            uploadDelegates.remove(uploadId);
            UploadServiceLogger.INSTANCE.info(TAG, "\n\n\nUpload delegate for upload with Id " + uploadId + " is gone!\n" +
                    "Probably you have set it in an activity and the user navigated away from it\n" +
                    "before the upload was completed. From now on, the events will be dispatched\n" +
                    "with broadcast intents. If you see this message, consider switching to the\n" +
                    "UploadServiceBroadcastReceiver registered globally in your manifest.\n" +
                    "Read this:\n" +
                    "https://github.com/gotev/android-upload-service/wiki/Monitoring-upload-status\n");
        }

        return delegate;
    }
}
