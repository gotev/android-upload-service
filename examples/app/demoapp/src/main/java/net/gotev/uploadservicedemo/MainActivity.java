package net.gotev.uploadservicedemo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadService;

import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @OnClick(R.id.multipart_upload)
    public void onMultipartUpload(View view) {
        MultipartUploadActivity.show(this);
    }

    @OnClick(R.id.binary_upload)
    public void onBinaryUpload(View view) {
        BinaryUploadActivity.show(this);
    }

    @OnClick(R.id.ftp_upload)
    public void onFTPUpload(View view) {
        FTPUploadActivity.show(this);
    }

    @OnClick(R.id.cancelAllUploadsButton)
    public void onCancelAllUploadsButtonClick() {
        //upload(this, UUID.randomUUID().toString(), "http://posttestserver.com/post.php");
        UploadService.stopAllUploads();
    }

    // Replicate https://github.com/gotev/android-upload-service/issues/245
    private String upload(Context context, String uploadId, String endpoint) {
        try {
            return new MultipartUploadRequest(context, uploadId, endpoint)
                    .setMethod("POST")
                    .setNotificationConfig(null)
                    .setAutoDeleteFilesAfterSuccessfulUpload(true)
                    .addParameter("color", "#ffffff")
                    .setMaxRetries(2)
                    .startUpload();
        } catch (Exception exc) {
            Log.e(getClass().getSimpleName(), "Error", exc);
            return null;
        }
    }

}
