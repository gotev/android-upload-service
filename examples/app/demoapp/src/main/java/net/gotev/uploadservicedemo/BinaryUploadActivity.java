package net.gotev.uploadservicedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;

import java.io.IOException;

import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_FILE;
import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_HEADER;

/**
 * @author Aleksandar Gotev
 */

public class BinaryUploadActivity extends UploadActivity {

    public static void show(BaseActivity activity) {
        activity.startActivity(new Intent(activity, BinaryUploadActivity.class));
    }

    @Override
    public AdapterItem getEmptyItem() {
        return new EmptyItem(R.string.empty_binary_upload);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addParameter.setVisibility(View.GONE);
    }

    @Override
    public void onAddFile() {
        fileParameterName = "file";
        openFilePicker(false);
    }

    @Override
    public void onDone(String httpMethod, String serverUrl, RecyclerAdapter uploadItemsAdapter) {

        final BinaryUploadRequest request = new BinaryUploadRequest(this, serverUrl)
                .setMethod(httpMethod)
                .setNotificationConfig(getNotificationConfig(R.string.binary_upload))
                .setMaxRetries(MAX_RETRIES)
                .setUsesFixedLengthStreamingMode(FIXED_LENGTH_STREAMING_MODE)
                .setCustomUserAgent(getUserAgent());

        uploadItemUtils.forEach(new UploadItemUtils.ForEachDelegate() {

            @Override
            public void onUploadItem(UploadItem item) {

                switch (item.getType()) {
                    case TYPE_HEADER:
                        request.addHeader(item.getTitle(), item.getSubtitle());
                        break;

                    case TYPE_FILE:
                        try {
                            request.setFileToUpload(item.getSubtitle());
                        } catch (IOException exc) {
                            Toast.makeText(BinaryUploadActivity.this,
                                    getString(R.string.file_not_found, item.getSubtitle()),
                                    Toast.LENGTH_LONG).show();
                        }
                        break;

                    default:
                        break;
                }
            }

        });

        try {
            request.startUpload();
            finish();
        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onInfo() {
        openBrowser("https://github.com/gotev/android-upload-service/wiki/Recipes#http-binary-upload-");
    }
}
