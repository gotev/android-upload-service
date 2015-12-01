package com.alexbbb.uploadservicedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alexbbb.uploadservice.BinaryUploadRequest;
import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadNotificationConfig;
import com.alexbbb.uploadservice.UploadService;
import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.alexbbb.uploadservice.demo.BuildConfig;
import com.alexbbb.uploadservice.demo.R;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author Alex Gotev
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "UploadServiceDemo";
    private static final String USER_AGENT = "UploadServiceDemo/" + BuildConfig.VERSION_NAME;

    @Bind(R.id.uploadProgress) ProgressBar progressBar;
    @Bind(R.id.multipartUploadButton) Button multipartUploadButton;
    @Bind(R.id.binaryUploadButton) Button binaryUploadButton;
    @Bind(R.id.cancelUploadButton) Button cancelUploadButton;
    @Bind(R.id.serverURL) EditText serverUrl;
    @Bind(R.id.fileToUpload) EditText fileToUpload;
    @Bind(R.id.parameterName) EditText parameterName;
    @Bind(R.id.displayNotification) CheckBox displayNotification;

    private final UploadServiceBroadcastReceiver uploadReceiver =
            new UploadServiceBroadcastReceiver() {

        @Override
        public void onProgress(String uploadId, int progress) {
            progressBar.setProgress(progress);

            Log.i(TAG, "The progress of the upload with ID " + uploadId + " is: " + progress);
        }

        @Override
        public void onError(String uploadId, Exception exception) {
            progressBar.setProgress(0);

            Log.e(TAG, "Error in upload with ID: " + uploadId + ". "
                        + exception.getLocalizedMessage(), exception);
        }

        @Override
        public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage) {
            progressBar.setProgress(0);

            Log.i(TAG, "Upload with ID " + uploadId + " is completed: " + serverResponseCode + ", "
                       + serverResponseMessage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        progressBar.setMax(100);
        progressBar.setProgress(0);

        // Uncomment this line to enable self-signed SSL certificates in HTTPS connections
        // WARNING: Do not use in production environment. Recommended for development only
        // AllCertificatesAndHostsTruster.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        uploadReceiver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uploadReceiver.unregister(this);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private UploadNotificationConfig getNotificationConfig() {
        if (!displayNotification.isChecked()) return null;

        return new UploadNotificationConfig()
            .setIcon(R.drawable.ic_upload)
            .setTitle(getString(R.string.app_name))
            .setInProgressMessage(getString(R.string.uploading))
            .setCompletedMessage(getString(R.string.upload_success))
            .setErrorMessage(getString(R.string.upload_error))
            .setAutoClearOnSuccess(false)
            .setClickIntent(new Intent(this, MainActivity.class))
            .setClearOnAction(true)
            .setRingToneEnabled(true);
    }

    @OnClick(R.id.multipartUploadButton)
    void onMultipartUploadClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String paramNameString = parameterName.getText().toString();
        final String uploadID = UUID.randomUUID().toString();

        try {
            new MultipartUploadRequest(this, uploadID, serverUrlString)
                .addFileToUpload(fileToUploadPath, paramNameString, "test",
                        ContentType.APPLICATION_OCTET_STREAM)
                .setNotificationConfig(getNotificationConfig())
                .setCustomUserAgent(USER_AGENT)
                .setMaxRetries(2)
                .startUpload();

        // these are the different exceptions that may be thrown
        } catch (FileNotFoundException exc) {
            showToast(exc.getMessage());
        } catch (IllegalArgumentException exc) {
            showToast("Missing some arguments. " + exc.getMessage());
        } catch (MalformedURLException exc) {
            showToast(exc.getMessage());
        }
    }

    @OnClick(R.id.binaryUploadButton)
    void onUploadBinaryClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String paramNameString = parameterName.getText().toString();
        final String uploadID = UUID.randomUUID().toString();

        try {
            new BinaryUploadRequest(this, uploadID, serverUrlString)
                .addHeader("file-name", paramNameString)
                .setFileToUpload(fileToUploadPath)
                .setNotificationConfig(getNotificationConfig())
                .setCustomUserAgent(USER_AGENT)
                .setMaxRetries(2)
                .startUpload();

        // these are the different exceptions that may be thrown
        } catch (FileNotFoundException exc) {
            showToast(exc.getMessage());
        } catch (IllegalArgumentException exc) {
            showToast("Missing some arguments. " + exc.getMessage());
        } catch (MalformedURLException exc) {
            showToast(exc.getMessage());
        }
    }

    @OnClick(R.id.cancelUploadButton)
    void onCancelUploadButtonClick() {
        UploadService.stopCurrentUpload();
    }
}
