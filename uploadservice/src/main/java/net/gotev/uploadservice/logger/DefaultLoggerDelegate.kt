package net.gotev.uploadservice.logger

import android.util.Log

class DefaultLoggerDelegate : UploadServiceLogger.Delegate {

    companion object {
        private const val TAG = "UploadService"
    }

    override fun error(component: String, uploadId: String, message: String, exception: Throwable?) {
        Log.e(TAG, "$component - (uploadId: $uploadId) - $message", exception)
    }

    override fun debug(component: String, uploadId: String, message: String) {
        Log.i(TAG, "$component - (uploadId: $uploadId) - $message")
    }

    override fun info(component: String, uploadId: String, message: String) {
        Log.i(TAG, "$component - (uploadId: $uploadId) - $message")
    }
}
