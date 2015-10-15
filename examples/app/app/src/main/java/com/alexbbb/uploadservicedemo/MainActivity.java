package com.alexbbb.uploadservicedemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.alexbbb.uploadservice.AbstractUploadServiceReceiver;
import com.alexbbb.uploadservice.BinaryUploadRequest;
import com.alexbbb.uploadservice.ContentType;
import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadService;
import com.alexbbb.uploadservice.demo.R;

import java.io.File;
import java.net.URL;
import java.util.UUID;

/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author Alex Gotev
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "UploadServiceDemo";

    private ProgressBar progressBar;
    private Button multipartUploadButton;
    private Button binaryUploadButton;
    private Button cancelUploadButton;
    private EditText serverUrl;
    private EditText fileToUpload;
    private EditText parameterName;

    private final AbstractUploadServiceReceiver uploadReceiver = new AbstractUploadServiceReceiver() {

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

        progressBar = (ProgressBar) findViewById(R.id.uploadProgress);
        serverUrl = (EditText) findViewById(R.id.serverURL);
        fileToUpload = (EditText) findViewById(R.id.fileToUpload);
        parameterName = (EditText) findViewById(R.id.parameterName);
        multipartUploadButton = (Button) findViewById(R.id.multipartUploadButton);
        binaryUploadButton = (Button) findViewById(R.id.binaryUploadButton);
        cancelUploadButton = (Button) findViewById(R.id.cancelUploadButton);

        multipartUploadButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onUploadButtonClick();
            }
        });

        binaryUploadButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onUploadBinaryClick();
            }
        });

        cancelUploadButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onCancelUploadButtonClick();
            }
        });

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

    private boolean userInputIsValid(final String serverUrlString, final String fileToUploadPath,
                                     final String paramNameString) {
        if (serverUrlString.length() == 0) {
            showToast(getString(R.string.provide_valid_server_url));
            return false;
        }

        try {
            new URL(serverUrlString.toString());
        } catch (Exception exc) {
            showToast(getString(R.string.provide_valid_server_url));
            return false;
        }

        if (fileToUploadPath.length() == 0) {
            showToast(getString(R.string.provide_file_to_upload));
            return false;
        }

        if (!new File(fileToUploadPath).exists()) {
            showToast(getString(R.string.file_does_not_exist));
            return false;
        }

        if (paramNameString.length() == 0) {
            showToast(getString(R.string.provide_param_name));
            return false;
        }

        return true;
    }

    private void onUploadButtonClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String paramNameString = parameterName.getText().toString();

        if (!userInputIsValid(serverUrlString, fileToUploadPath, paramNameString))
            return;

        final MultipartUploadRequest request =
                new MultipartUploadRequest(this, UUID.randomUUID().toString(), serverUrlString);

        request.addFileToUpload(fileToUploadPath, paramNameString,
                                "test", ContentType.APPLICATION_OCTET_STREAM);

        request.setNotificationConfig(R.mipmap.ic_launcher,
                                      getString(R.string.app_name),
                                      getString(R.string.uploading),
                                      getString(R.string.upload_success),
                                      getString(R.string.upload_error),
                                      false);

        // if you comment the following line, the system default user-agent will be used
        request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps
        // on the notification
        request.setNotificationClickIntent(new Intent(this, MainActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            request.startUpload();
        } catch (Exception exc) {
            showToast("Malformed upload request. " + exc.getLocalizedMessage());
        }
    }

    private void onUploadBinaryClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String fileToUploadPath = fileToUpload.getText().toString();
        final String paramNameString = parameterName.getText().toString();

        if (!userInputIsValid(serverUrlString, fileToUploadPath, paramNameString))
            return;

        final BinaryUploadRequest request =
                new BinaryUploadRequest(this, UUID.randomUUID().toString(), serverUrlString);

        // you can pass some data as request header, but you should be extremely careful
        request.addHeader("file-name", paramNameString);

        request.setFileToUpload(fileToUploadPath);

        request.setNotificationConfig(R.mipmap.ic_launcher,
                                      getString(R.string.app_name),
                                      getString(R.string.uploading),
                                      getString(R.string.upload_success),
                                      getString(R.string.upload_error),
                                      false);

        // if you comment the following line, the system default user-agent will be used
        request.setCustomUserAgent("UploadServiceDemo/1.0");

        // set the intent to perform when the user taps on the upload notification.
        // currently tested only with intents that launches an activity
        // if you comment this line, no action will be performed when the user taps on the notification
        request.setNotificationClickIntent(new Intent(this, MainActivity.class));

        // set the maximum number of automatic upload retries on error
        request.setMaxRetries(2);

        try {
            request.startUpload();
        } catch (Exception exc) {
            showToast("Malformed upload request. " + exc.getLocalizedMessage());
        }
    }

    private void onCancelUploadButtonClick() {
        UploadService.stopCurrentUpload();
    }
}
