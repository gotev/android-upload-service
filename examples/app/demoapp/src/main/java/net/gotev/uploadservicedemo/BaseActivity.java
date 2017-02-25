package net.gotev.uploadservicedemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.gotev.uploadservice.UploadNotificationConfig;

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

    protected UploadNotificationConfig getNotificationConfig(@StringRes int title) {
        return new UploadNotificationConfig()
                .setIcon(R.drawable.ic_upload)
                .setCompletedIcon(R.drawable.ic_upload_success)
                .setErrorIcon(R.drawable.ic_upload_error)
                .setCancelledIcon(R.drawable.ic_cancelled)
                .setIconColor(Color.BLUE)
                .setCompletedIconColor(Color.GREEN)
                .setErrorIconColor(Color.RED)
                .setCancelledIconColor(Color.YELLOW)
                .setTitle(getString(title))
                .setInProgressMessage(getString(R.string.uploading))
                .setCompletedMessage(getString(R.string.upload_success))
                .setErrorMessage(getString(R.string.upload_error))
                .setCancelledMessage(getString(R.string.upload_cancelled))
                .setClickIntent(new Intent(this, MainActivity.class))
                .setClearOnAction(true)
                .setRingToneEnabled(true);
    }

    protected void openBrowser(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
