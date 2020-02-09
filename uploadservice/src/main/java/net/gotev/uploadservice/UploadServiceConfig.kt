package net.gotev.uploadservice

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import net.gotev.uploadservice.data.RetryPolicyConfig
import net.gotev.uploadservice.data.UploadNotificationAction
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import net.gotev.uploadservice.logger.UploadServiceLogger
import net.gotev.uploadservice.network.HttpStack
import net.gotev.uploadservice.network.hurl.HurlStack
import net.gotev.uploadservice.observer.request.NotificationActionsObserver
import net.gotev.uploadservice.observer.task.NotificationHandler
import net.gotev.uploadservice.observer.task.UploadTaskObserver
import net.gotev.uploadservice.placeholders.DefaultPlaceholdersProcessor
import net.gotev.uploadservice.placeholders.Placeholder
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

    /**
     * Default User Agent used by default Http Stack constructors.
     */
    const val defaultUserAgent = "AndroidUploadService/" + BuildConfig.VERSION_NAME

    private val schemeHandlers by lazy {
        LinkedHashMap<String, Class<out SchemeHandler>>().apply {
            this[fileScheme] = FileSchemeHandler::class.java
            this[contentScheme] = ContentResolverSchemeHandler::class.java
        }
    }

    /**
     * Initializes Upload Service with namespace and default notification channel.
     * This must be done in your application subclass onCreate method before anything else.
     * @param context your Application's context
     * @param defaultNotificationChannel Default notification channel to use
     * @param debug set this to your BuildConfig.DEBUG
     */
    @JvmStatic
    fun initialize(context: Application, defaultNotificationChannel: String, debug: Boolean) {
        this.namespace = context.packageName
        this.defaultNotificationChannel = defaultNotificationChannel
        UploadServiceLogger.setDevelopmentMode(debug)
    }

    /**
     * Namespace with which Upload Service is going to operate. This must be set in application
     * subclass onCreate method before anything else.
     */
    @JvmStatic
    var namespace: String? = null
        private set
        get() = if (field == null)
            throw IllegalArgumentException("You have to set namespace to your app package name (context.packageName) in your Application subclass")
        else
            field

    /**
     * Default notification channel to use. This must be set in application
     * subclass onCreate method before anything else.
     */
    @JvmStatic
    var defaultNotificationChannel: String? = null
        private set
        get() = if (field == null)
            throw IllegalArgumentException("You have to set defaultNotificationChannel in your Application subclass")
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
     * Creates the default notification configuration for uploads.
     * You can set your own configuration factory. Each upload can also override those settings.
     */
    @JvmStatic
    var notificationConfigFactory: (context: Context, uploadId: String) -> UploadNotificationConfig = { context, uploadId ->
        val title = "Upload"

        UploadNotificationConfig(
            notificationChannelId = defaultNotificationChannel!!,
            isRingToneEnabled = true,
            progress = UploadNotificationStatusConfig(
                title = title,
                message = "Uploading at ${Placeholder.UploadRate} (${Placeholder.Progress})",
                actions = arrayListOf(
                    UploadNotificationAction(
                        icon = android.R.drawable.ic_menu_close_clear_cancel,
                        title = "Cancel",
                        intent = context.getCancelUploadIntent(uploadId)
                    )
                )
            ),
            success = UploadNotificationStatusConfig(
                title = title,
                message = "Upload completed successfully in ${Placeholder.ElapsedTime}"
            ),
            error = UploadNotificationStatusConfig(
                title = title,
                message = "Error during upload"
            ),
            cancelled = UploadNotificationStatusConfig(
                title = title,
                message = "Upload cancelled"
            )
        )
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
     * Default is 3 updates per second
     */
    @JvmStatic
    var uploadProgressNotificationIntervalMillis: Long = 1000 / 3

    /**
     * Sets the Upload Service Retry Policy. Refer to [RetryPolicyConfig] docs for detailed
     * explanation of each parameter.
     */
    @JvmStatic
    var retryPolicy = RetryPolicyConfig(
        initialWaitTimeSeconds = 1,
        maxWaitTimeSeconds = 100,
        multiplier = 2,
        defaultMaxRetries = 3
    )

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
                "androidApiVersion": ${Build.VERSION.SDK_INT},
                "namespace": "$namespace",
                "deviceProcessors": ${Runtime.getRuntime().availableProcessors()},
                "idleTimeoutSeconds": $idleTimeoutSeconds,
                "bufferSizeBytes": $bufferSizeBytes,
                "httpStack": "${httpStack::class.java.name}",
                "uploadProgressNotificationIntervalMillis": $uploadProgressNotificationIntervalMillis,
                "retryPolicy": $retryPolicy,
                "isForegroundService": $isForegroundService,
                "schemeHandlers": {${schemeHandlers.entries.joinToString { (key, value) -> "\"$key\": \"${value.name}\"" }}}
            }
        """.trimIndent()
    }
}
