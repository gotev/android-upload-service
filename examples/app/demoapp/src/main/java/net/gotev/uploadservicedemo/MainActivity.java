package net.gotev.uploadservicedemo;

import android.os.Bundle;
import android.view.View;

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
        UploadService.stopAllUploads();
    }

}
