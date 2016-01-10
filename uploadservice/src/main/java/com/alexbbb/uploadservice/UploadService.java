package com.alexbbb.uploadservice;

import android.app.IntentService;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Service to upload files in background using HTTP POST with notification center progress
 * display.
 *
 * @author alexbbb (Aleksandar Gotev)
 * @author eliasnaur
 * @author cankov
 */
public class UploadService extends IntentService {

    private static final String SERVICE_NAME = UploadService.class.getName();
    private static final String TAG = "UploadService";

    private static final int UPLOAD_NOTIFICATION_ID = 1234; // Something unique

    public static String NAMESPACE = "com.alexbbb";

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

    private PowerManager.WakeLock wakeLock;

    private static HttpUploadTask currentTask;

    protected static String getActionUpload() {
        return NAMESPACE + ACTION_UPLOAD_SUFFIX;
    }

    protected static String getActionBroadcast() {
        return NAMESPACE + BROADCAST_ACTION_SUFFIX;
    }

    /**
     * Stops the currently active upload task.
     */
    public static void stopCurrentUpload() {
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    public UploadService() {
        super(SERVICE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();

            if (getActionUpload().equals(action)) {
                currentTask = getTask(intent);

                if (currentTask == null) {
                    return;
                }

                currentTask.setLastProgressNotificationTime(0)
                           .setNotificationId(UPLOAD_NOTIFICATION_ID);
                wakeLock.acquire();
                currentTask.run();
            }
        }
    }

    /**
     * Creates a new task instance based on the requested task type in the intent.
     * @param intent
     * @return task instance or null if the type is not supported or invalid
     */
    HttpUploadTask getTask(Intent intent) {
        String type = intent.getStringExtra(PARAM_TYPE);

        if (UPLOAD_MULTIPART.equals(type)) {
            return new MultipartUploadTask(this, intent);
        }

        if (UPLOAD_BINARY.equals(type)) {
            return new BinaryUploadTask(this, intent);
        }

        return null;
    }

    /**
     * Called by each task when it is completed (either successfully, with an error or due to
     * user cancellation).
     */
    protected void taskCompleted() {
        wakeLock.release();
    }
}
