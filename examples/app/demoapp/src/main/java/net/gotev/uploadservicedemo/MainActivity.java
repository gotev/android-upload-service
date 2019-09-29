package net.gotev.uploadservicedemo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservicedemo.issues.Issue251;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.run_issue)
    Button runIssue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //runIssue.setVisibility(View.VISIBLE);
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

    @OnClick(R.id.run_issue)
    public void runIssue() {
        new Issue251(this).run();
    }

}
