package net.gotev.uploadservicedemo.activities

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import net.gotev.uploadservice.data.UploadNotificationAction
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.extensions.flagsCompat
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import net.gotev.uploadservicedemo.App
import net.gotev.uploadservicedemo.CustomPlaceholdersProcessor
import net.gotev.uploadservicedemo.R
import net.gotev.uploadservicedemo.extensions.inputMethodManager
import java.util.ArrayList

open class BaseActivity : AppCompatActivity() {
    override fun onPause() {
        super.onPause()

        // hide soft keyboard if shown
        val view = currentFocus
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    protected fun getNotificationConfig(
        uploadId: String,
        @StringRes title: Int
    ): UploadNotificationConfig {
        val clickIntent = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), flagsCompat(PendingIntent.FLAG_UPDATE_CURRENT)
        )

        val autoClear = false
        val largeIcon: Bitmap? = null
        val clearOnAction = true
        val ringToneEnabled = true

        val cancelAction = UploadNotificationAction(
            R.drawable.ic_cancelled,
            getString(R.string.cancel_upload),
            this.getCancelUploadIntent(uploadId)
        )

        val noActions = ArrayList<UploadNotificationAction>(1)
        val progressActions = ArrayList<UploadNotificationAction>(1)
        progressActions.add(cancelAction)

        val progress = UploadNotificationStatusConfig(
            getString(title) + ": " + CustomPlaceholdersProcessor.FILENAME_PLACEHOLDER,
            getString(R.string.uploading),
            R.drawable.ic_upload,
            Color.BLUE,
            largeIcon,
            clickIntent,
            progressActions,
            clearOnAction,
            autoClear
        )

        val success = UploadNotificationStatusConfig(
            getString(title) + ": " + CustomPlaceholdersProcessor.FILENAME_PLACEHOLDER,
            getString(R.string.upload_success),
            R.drawable.ic_upload_success,
            Color.GREEN,
            largeIcon,
            clickIntent,
            noActions,
            clearOnAction,
            autoClear
        )

        val error = UploadNotificationStatusConfig(
            getString(title) + ": " + CustomPlaceholdersProcessor.FILENAME_PLACEHOLDER,
            getString(R.string.upload_error),
            R.drawable.ic_upload_error,
            Color.RED,
            largeIcon,
            clickIntent,
            noActions,
            clearOnAction,
            autoClear
        )

        val cancelled = UploadNotificationStatusConfig(
            getString(title) + ": " + CustomPlaceholdersProcessor.FILENAME_PLACEHOLDER,
            getString(R.string.upload_cancelled),
            R.drawable.ic_cancelled,
            Color.YELLOW,
            largeIcon,
            clickIntent,
            noActions,
            clearOnAction
        )

        return UploadNotificationConfig(
            App.CHANNEL,
            ringToneEnabled,
            progress,
            success,
            error,
            cancelled
        )
    }
}
