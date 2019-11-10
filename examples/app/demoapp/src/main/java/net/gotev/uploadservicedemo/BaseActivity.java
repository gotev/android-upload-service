package net.gotev.uploadservicedemo;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import net.gotev.uploadservice.data.UploadNotificationAction;
import net.gotev.uploadservice.data.UploadNotificationConfig;
import net.gotev.uploadservice.extensions.ContextExtensionsKt;

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
        UploadNotificationConfig config = new UploadNotificationConfig(App.CHANNEL);

        PendingIntent clickIntent = PendingIntent.getActivity(
                this, 1, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        config.setTitleForAllStatuses(getString(title))
                .setClickIntentForAllStatuses(clickIntent)
                .setClearOnActionForAllStatuses(true)
                .setRingToneEnabled(true);

        config.getProgress().setMessage(getString(R.string.uploading));
        config.getProgress().setIconResourceID(R.drawable.ic_upload);
        config.getProgress().setIconColorResourceID(Color.BLUE);
        config.getProgress().getActions().add(new UploadNotificationAction(
                R.drawable.ic_cancelled,
                getString(R.string.cancel_upload),
                ContextExtensionsKt.getCancelUploadIntent(this, uploadId)
        ));

        config.getSuccess().setMessage(getString(R.string.upload_success));
        config.getSuccess().setIconResourceID(R.drawable.ic_upload_success);
        config.getSuccess().setIconColorResourceID(Color.GREEN);

        config.getError().setMessage(getString(R.string.upload_error));
        config.getError().setIconResourceID(R.drawable.ic_upload_error);
        config.getError().setIconColorResourceID(Color.RED);

        config.getCancelled().setMessage(getString(R.string.upload_cancelled));
        config.getCancelled().setIconResourceID(R.drawable.ic_cancelled);
        config.getCancelled().setIconColorResourceID(Color.YELLOW);

        return config;
    }

    protected void openBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
