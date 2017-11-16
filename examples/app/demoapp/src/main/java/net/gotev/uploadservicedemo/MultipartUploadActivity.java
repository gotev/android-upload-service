package net.gotev.uploadservicedemo;

import android.content.Intent;
import android.widget.Toast;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Aleksandar Gotev
 */

public class MultipartUploadActivity extends UploadActivity {

    public static void show(BaseActivity activity) {
        activity.startActivity(new Intent(activity, MultipartUploadActivity.class));
    }

    @Override
    public AdapterItem getEmptyItem() {
        return new EmptyItem(R.string.empty_multipart_upload);
    }

    @Override
    public void onDone(String httpMethod, String serverUrl, UploadItemUtils uploadItemUtils) {
        try {
            final String uploadId = UUID.randomUUID().toString();

            final MultipartUploadRequest request =
                    new MultipartUploadRequest(this, uploadId, serverUrl)
                    .setMethod(httpMethod)
                    .setUtf8Charset()
                    .setNotificationConfig(getNotificationConfig(uploadId, R.string.multipart_upload))
                    .setMaxRetries(MAX_RETRIES)
                    //.setCustomUserAgent(getUserAgent())
                    .setUsesFixedLengthStreamingMode(FIXED_LENGTH_STREAMING_MODE);

            uploadItemUtils.forEach(new UploadItemUtils.ForEachDelegate() {

                @Override
                public void onHeader(UploadItem item) {
                    try {
                        request.addHeader(item.getTitle(), item.getSubtitle());
                    } catch (IllegalArgumentException exc) {
                        Toast.makeText(MultipartUploadActivity.this,
                                exc.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onParameter(UploadItem item) {
                    request.addParameter(item.getTitle(), item.getSubtitle());
                }

                @Override
                public void onFile(UploadItem item) {
                    try {
                        request.addFileToUpload(item.getSubtitle(), item.getTitle());
                    } catch (IOException exc) {
                        Toast.makeText(MultipartUploadActivity.this,
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
        openBrowser("https://github.com/gotev/android-upload-service/wiki/Recipes#http-multipartform-data-upload-rfc2388-");
    }
}
