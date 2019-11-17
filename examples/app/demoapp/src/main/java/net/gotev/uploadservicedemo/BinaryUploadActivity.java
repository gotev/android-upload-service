package net.gotev.uploadservicedemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;

import java.io.IOException;

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
        addFile.setTitleText(getString(R.string.set_file));
    }

    @Override
    public void onAddFile() {
        fileParameterName = "file";
        //openFilePicker(false);
        performFileSearch();
    }

    @Override
    public void onDone(String httpMethod, String serverUrl, UploadItemUtils uploadItemUtils) {

        try {
            final BinaryUploadRequest request = new BinaryUploadRequest(this, serverUrl)
                    .setMethod(httpMethod)
                    .setNotificationConfig((context, uploadId) ->
                            getNotificationConfig(uploadId, R.string.binary_upload)
                    );

            uploadItemUtils.forEach(new UploadItemUtils.ForEachDelegate() {

                @Override
                public void onHeader(UploadItem item) {
                    request.addHeader(item.getTitle(), item.getSubtitle());
                }

                @Override
                public void onParameter(UploadItem item) {
                    // Binary uploads does not support parameters
                }

                @Override
                public void onFile(UploadItem item) {
                    try {
                        request.setFileToUpload(item.getSubtitle());
                    } catch (IOException exc) {
                        Toast.makeText(BinaryUploadActivity.this,
                                getString(R.string.file_not_found, item.getSubtitle()),
                                Toast.LENGTH_LONG).show();
                    }
                }

            });

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
