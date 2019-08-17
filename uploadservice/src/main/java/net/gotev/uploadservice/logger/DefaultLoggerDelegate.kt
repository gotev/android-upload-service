package net.gotev.uploadservice.logger

import android.util.Log

/**
 * Default logger delegate implementation which logs in LogCat with [Log].
 * Log tag is set to **UploadService** for all the logs.
 * @author gotev (Aleksandar Gotev)
 */
class DefaultLoggerDelegate : UploadServiceLogger.Delegate {

    companion object {
        private const val TAG = "UploadService"
    }

    override fun error(tag: String, message: String, exception: Throwable?) {
        Log.e(TAG, "$tag - $message", exception)
    }

    override fun debug(tag: String, message: String) {
        Log.d(TAG, "$tag - $message")
    }

    override fun info(tag: String, message: String) {
        Log.i(TAG, "$tag - $message")
    }
}
