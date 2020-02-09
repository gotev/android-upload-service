package net.gotev.uploadservice

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.LifecycleOwner
import net.gotev.uploadservice.data.UploadFile
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadTaskParameters
import net.gotev.uploadservice.extensions.startNewUpload
import net.gotev.uploadservice.observer.request.RequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import java.util.ArrayList
import java.util.UUID

/**
 * Base class to extend to create an upload request. If you are implementing an HTTP based upload,
 * extend [HttpUploadRequest] instead.
 */
abstract class UploadRequest<B : UploadRequest<B>>
/**
 * Creates a new upload request.
 *
 * @param context application context
 * @param serverUrl URL of the server side script that handles the request
 * @throws IllegalArgumentException if one or more arguments are not valid
 */
@Throws(IllegalArgumentException::class)
constructor(protected val context: Context, protected var serverUrl: String) {

    private var uploadId = UUID.randomUUID().toString()
    protected var maxRetries = UploadServiceConfig.retryPolicy.defaultMaxRetries
    protected var autoDeleteSuccessfullyUploadedFiles = false
    protected var notificationConfig: (context: Context, uploadId: String) -> UploadNotificationConfig = UploadServiceConfig.notificationConfigFactory
    protected val files = ArrayList<UploadFile>()

    /**
     * Implement in subclasses to specify the class which will handle the the upload task.
     * The class must be a subclass of [UploadTask].
     * @return class
     */
    protected abstract val taskClass: Class<out UploadTask>

    init {
        require(serverUrl.isNotBlank()) { "Server URL cannot be empty" }
    }

    /**
     * Start the background file upload service.
     * @return the uploadId string. If you have passed your own uploadId in the constructor, this
     * method will return that same uploadId, otherwise it will return the automatically
     * generated uploadId
     */
    open fun startUpload(): String {
        return context.startNewUpload(
            taskClass, UploadTaskParameters(
                uploadId,
                serverUrl,
                maxRetries,
                autoDeleteSuccessfullyUploadedFiles,
                notificationConfig(context, uploadId),
                files,
                getAdditionalParameters()
            )
        )
    }

    /**
     * Subscribe to events of this upload request
     * @param observer observer to listen for events.
     */
    fun subscribe(observer: RequestObserver) {
        observer.subscribe(this)
    }

    /**
     * Subscribe to events of this upload request by creating a new request observer.
     * @param context context
     * @param lifecycleOwner lifecycle to use when subscribing for events
     * @param delegate Observer delegate implementation
     */
    fun subscribe(context: Context, lifecycleOwner: LifecycleOwner, delegate: RequestObserverDelegate): RequestObserver {
        return RequestObserver(context, lifecycleOwner, delegate).apply { subscribe(this@UploadRequest) }
    }

    protected abstract fun getAdditionalParameters(): Parcelable

    @Suppress("UNCHECKED_CAST")
    protected fun self(): B {
        return this as B
    }

    /**
     * Sets custom notification configuration.
     *
     * @param config the upload configuration object
     * @return self instance
     */
    fun setNotificationConfig(config: (context: Context, uploadId: String) -> UploadNotificationConfig): B {
        this.notificationConfig = config
        return self()
    }

    /**
     * Sets the automatic file deletion after successful upload.
     * @param autoDeleteFiles true to auto delete files included in the
     * request when the upload is completed successfully.
     * By default this setting is set to false, and nothing gets deleted.
     * @return self instance
     */
    fun setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFiles: Boolean): B {
        this.autoDeleteSuccessfullyUploadedFiles = autoDeleteFiles
        return self()
    }

    /**
     * Sets the maximum number of retries that the library will try if an error occurs,
     * before returning an error.
     *
     * @param maxRetries number of maximum retries on error
     * @return self instance
     */
    fun setMaxRetries(maxRetries: Int): B {
        this.maxRetries = maxRetries
        return self()
    }

    /**
     * Set Upload ID.
     *
     * @param uploadID unique ID to assign to this upload request.
     * If it's null or empty, a random UUID will be automatically generated.
     * It's used in the broadcast receiver when receiving updates.
     */
    fun setUploadID(uploadID: String): B {
        this.uploadId = uploadID
        return self()
    }
}
