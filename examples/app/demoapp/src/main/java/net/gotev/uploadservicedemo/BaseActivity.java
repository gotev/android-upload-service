package net.gotev.uploadservicedemo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import net.gotev.uploadservice.data.UploadNotificationAction;
import net.gotev.uploadservice.data.UploadNotificationConfig;
import net.gotev.uploadservice.data.UploadNotificationStatusConfig;
import net.gotev.uploadservice.extensions.ContextExtensionsKt;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * @author Aleksandar Gotev
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // hide soft keyboard if shown
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected UploadNotificationConfig getNotificationConfig(final String uploadId, @StringRes int title) {
        PendingIntent clickIntent = PendingIntent.getActivity(
                this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        final boolean autoClear = false;
        final Bitmap largeIcon = null;
        final boolean clearOnAction = true;
        final boolean ringToneEnabled = true;
        final ArrayList<UploadNotificationAction> noActions = new ArrayList<>(1);

        final UploadNotificationAction cancelAction = new UploadNotificationAction(
                R.drawable.ic_cancelled,
                getString(R.string.cancel_upload),
                ContextExtensionsKt.getCancelUploadIntent(this, uploadId)
        );

        final ArrayList<UploadNotificationAction> progressActions = new ArrayList<>(1);
        progressActions.add(cancelAction);

        UploadNotificationStatusConfig progress = new UploadNotificationStatusConfig(
                getString(title),
                getString(R.string.uploading),
                R.drawable.ic_upload,
                Color.BLUE,
                largeIcon,
                clickIntent,
                progressActions,
                clearOnAction,
                autoClear
        );

        UploadNotificationStatusConfig success = new UploadNotificationStatusConfig(
                getString(title),
                getString(R.string.upload_success),
                R.drawable.ic_upload_success,
                Color.GREEN,
                largeIcon,
                clickIntent,
                noActions,
                clearOnAction,
                autoClear
        );

        UploadNotificationStatusConfig error = new UploadNotificationStatusConfig(
                getString(title),
                getString(R.string.upload_error),
                R.drawable.ic_upload_error,
                Color.RED,
                largeIcon,
                clickIntent,
                noActions,
                clearOnAction,
                autoClear
        );

        UploadNotificationStatusConfig cancelled = new UploadNotificationStatusConfig(
                getString(title),
                getString(R.string.upload_cancelled),
                R.drawable.ic_cancelled,
                Color.YELLOW,
                largeIcon,
                clickIntent,
                noActions,
                clearOnAction
        );

        return new UploadNotificationConfig(App.CHANNEL, ringToneEnabled, progress, success, error, cancelled);
    }

    protected void openBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
