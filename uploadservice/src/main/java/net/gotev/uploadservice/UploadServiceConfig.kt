package net.gotev.uploadservice

import android.content.IntentFilter
import android.os.Build
import net.gotev.uploadservice.data.RetryPolicyConfig
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.hurl.HurlStack
import net.gotev.uploadservice.observer.request.NotificationActionsObserver
import net.gotev.uploadservice.observer.task.NotificationHandler
import net.gotev.uploadservice.observer.task.UploadTaskObserver
import net.gotev.uploadservice.placeholders.DefaultPlaceholdersProcessor
import net.gotev.uploadservice.placeholders.PlaceholdersProcessor
import net.gotev.uploadservice.schemehandlers.ContentResolverSchemeHandler
import net.gotev.uploadservice.schemehandlers.FileSchemeHandler
import net.gotev.uploadservice.schemehandlers.SchemeHandler
import java.lang.reflect.InvocationTargetException
import java.util.LinkedHashMap
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object UploadServiceConfig {

    private const val uploadActionSuffix = ".uploadservice.action.upload"
    private const val broadcastStatusActionSuffix = ".uploadservice.broadcast.status"
    private const val notificationActionSuffix = ".uploadservice.broadcast.notification.action"

    private const val fileScheme = "/"
    private const val contentScheme = "content://"

    private val schemeHandlers by lazy {
        LinkedHashMap<String, Class<out SchemeHandler>>().apply {
            this[fileScheme] = FileSchemeHandler::class.java
            this[contentScheme] = ContentResolverSchemeHandler::class.java
        }
    }

    /**
     * Namespace with which Upload Service is going to operate. This must be set in application
     * subclass onCreate method before anything else.
     */
    @JvmStatic
    var namespace: String? = null
        get() = if (field == null)
            throw IllegalArgumentException("You have to set namespace to BuildConfig.APPLICATION_ID in your Application subclass")
        else
            field

    /**
     * Sets the Thread Pool to use for upload operations.
     * By default a thread pool with size equal to the number of processors is created.
     */
    @JvmStatic
    var threadPool: AbstractExecutorService = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(), // Initial pool size
        Runtime.getRuntime().availableProcessors(), // Max pool size
        5.toLong(), // Keep Alive Time
        TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    /**
     * Creates the notification actions observer, which intercepts actions coming from notifications
     * and executes the required business logic.
     * By default, [NotificationActionsObserver] takes care of allowing the cancelling of
     * running uploads
     */
    @JvmStatic
    var notificationActionsObserverFactory: (UploadService) -> NotificationActionsObserver = {
        NotificationActionsObserver(it)
    }

    /**
     * Creates the notification handler for upload tasks.
     * The default notification handler creates a notification for each upload task.
     */
    @JvmStatic
    var notificationHandlerFactory: (UploadService) -> UploadTaskObserver = { uploadService ->
        NotificationHandler(uploadService)
    }

    /**
     * How many time to wait in idle before shutting down the service.
     * The service is idle when is running, but no tasks are running.
     */
    @JvmStatic
    var idleTimeoutSeconds = 10
        set(value) {
            require(value >= 1) { "Idle timeout min allowable value is 1. It cannot be $value" }
            field = value
        }

    /**
     * Buffer size in bytes used for data transfer by the upload tasks.
     */
    @JvmStatic
    var bufferSizeBytes = 4096
        set(value) {
            require(value >= 256) { "You can't set buffer size lower than 256 bytes" }
            field = value
        }

    /**
     * Sets the HTTP Stack to use to perform HTTP based upload requests.
     * By default [HurlStack] implementation is used.
     */
    @JvmStatic
    var httpStack: HttpStack = HurlStack()

    /**
     * Interval between progress notifications in milliseconds.
     * If the upload tasks report more frequently than this value, upload service will automatically apply throttling.
     * Default is 6 updates per second
     */
    @JvmStatic
    var uploadProgressNotificationIntervalMillis: Long = 1000 / 6

    /**
     * Sets the Upload Service Retry Policy. Refer to [RetryPolicyConfig] docs for detailed
     * explanation of each parameter.
     */
    @JvmStatic
    var retryPolicy = RetryPolicyConfig()

    /**
     * If set to true, the service will go in foreground mode when doing uploads,
     * lowering the probability of being killed by the system on low memory.
     * This setting is used only when your uploads have a notification configuration.
     * It's not possible to run in foreground without notifications, as per Android policy
     * constraints, so if you set this to true, but you do upload tasks without a
     * notification configuration, the service will simply run in background mode.
     *
     * NOTE: As of Android Oreo (API 26+), this setting is ignored as it always has to be true,
     * because the service must run in the foreground and expose a notification to the user.
     * https://developer.android.com/reference/android/content/Context.html#startForegroundService(android.content.Intent)
     */
    @JvmStatic
    var isForegroundService = true
        get() = Build.VERSION.SDK_INT >= 26 || field

    @JvmStatic
    val uploadAction: String
        get() = "$namespace$uploadActionSuffix"

    @JvmStatic
    val broadcastStatusAction: String
        get() = "$namespace$broadcastStatusActionSuffix"

    @JvmStatic
    val broadcastNotificationAction: String
        get() = "$namespace$notificationActionSuffix"

    /**
     * Get the intent filter for Upload Service broadcast events
     */
    @JvmStatic
    val broadcastStatusIntentFilter: IntentFilter
        get() = IntentFilter(broadcastStatusAction)

    /**
     * Get the intent filter for Upload Service Notification Actions events
     */
    @JvmStatic
    val broadcastNotificationActionIntentFilter: IntentFilter
        get() = IntentFilter(broadcastNotificationAction)

    /**
     * Processes placeholders contained in strings and replaces them with values.
     * Custom placeholders processor can be made by either extending [DefaultPlaceholdersProcessor]
     * and overriding only what you need (if you need to add your own placeholders on top of the
     * default ones or modify default values) or by implementing [PlaceholdersProcessor].
     */
    @JvmStatic
    var placeholdersProcessor: PlaceholdersProcessor =
        DefaultPlaceholdersProcessor()

    /**
     * Register a custom scheme handler.
     * You cannot override existing File and content:// schemes.
     * @param scheme scheme to support (e.g. content:// , yourCustomScheme://)
     * @param handler scheme handler implementation
     */
    @JvmStatic
    fun addSchemeHandler(scheme: String, handler: Class<out SchemeHandler>) {
        require(!(scheme == fileScheme || scheme == contentScheme)) { "Cannot override bundled scheme: $scheme! If you found a bug in a bundled scheme handler, please open an issue: https://github.com/gotev/android-upload-service" }
        schemeHandlers[scheme] = handler
    }

    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    @JvmStatic
    fun getSchemeHandler(path: String): SchemeHandler {
        val trimmedPath = path.trim()

        for ((scheme, handler) in schemeHandlers) {
            if (trimmedPath.startsWith(scheme, ignoreCase = true)) {
                return handler.newInstance().apply {
                    init(trimmedPath)
                }
            }
        }

        throw UnsupportedOperationException("Unsupported scheme for $path. Currently supported schemes are ${schemeHandlers.keys}")
    }

    override fun toString(): String {
        return """
            {
                "uploadServiceVersion": "${BuildConfig.VERSION_NAME}",
                "androidApiVesion": ${Build.VERSION.SDK_INT},
                "namespace": "$namespace",
                "deviceProcessors": ${Runtime.getRuntime().availableProcessors()},
                "idleTimeoutSeconds": $idleTimeoutSeconds,
                "bufferSizeBytes": $bufferSizeBytes,
                "httpStack": "${httpStack::class.java.name}",
                "uploadProgressNotificationIntervalMillis": $uploadProgressNotificationIntervalMillis,
                "retryPolicy": $retryPolicy,
                "isForegroundService": $isForegroundService,
                "schemeHandlers": [${schemeHandlers.entries.joinToString { (key, value) -> "\"$key\": \"$value\"" }}]
            }
        """.trimIndent()
    }
}
