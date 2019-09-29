package net.gotev.uploadservice.logger

import android.util.Log

class DefaultLoggerDelegate : UploadServiceLogger.Delegate {

    companion object {
        private const val TAG = "UploadService"
    }

    override fun error(tag: String, message: String, exception: Throwable?) {
        Log.e(TAG, "$tag - $message", exception)
    }

    override fun debug(tag: String, message: String) {
        Log.i(TAG, "$tag - $message")
    }

    override fun info(tag: String, message: String) {
        Log.i(TAG, "$tag - $message")
    }
}
