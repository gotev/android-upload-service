package net.gotev.uploadservicedemo;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;

import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;
import net.gotev.uploadservice.demo.BuildConfig;
import net.gotev.uploadservice.demo.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author gotev (Aleksandar Gotev)
 * @author mabdurrahman
 *
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "UploadServiceDemo";
    private static final String USER_AGENT = "UploadServiceDemo/" + BuildConfig.VERSION_NAME;
    private static final int FILE_CODE = 1;

    @Bind(R.id.container) ViewGroup container;
    @Bind(R.id.multipartUploadButton) Button multipartUploadButton;
    @Bind(R.id.binaryUploadButton) Button binaryUploadButton;
    @Bind(R.id.cancelAllUploadsButton) Button cancelAllUploadsButton;
    @Bind(R.id.serverURL) EditText serverUrl;
    @Bind(R.id.filesToUpload) EditText filesToUpload;
    @Bind(R.id.parameterName) EditText parameterName;
    @Bind(R.id.displayNotification) CheckBox displayNotification;
    @Bind(R.id.autoDeleteUploadedFiles) CheckBox autoDeleteUploadedFiles;
    @Bind(R.id.autoClearOnSuccess) CheckBox autoClearOnSuccess;
    @Bind(R.id.fixedLengthStreamingMode) CheckBox fixedLengthStreamingMode;

    private Map<String, UploadProgressViewHolder> uploadProgressHolders = new HashMap<>();

    private final UploadServiceBroadcastReceiver uploadReceiver =
            new UploadServiceBroadcastReceiver() {

        @Override
        public void onProgress(String uploadId, int progress) {
            Log.i(TAG, "The progress of the upload with ID " + uploadId + " is: " + progress);

            if (uploadProgressHolders.get(uploadId) == null)
                return;

            uploadProgressHolders.get(uploadId).progressBar.setProgress(progress);
        }

        @Override
        public void onError(String uploadId, Exception exception) {
            Log.e(TAG, "Error in upload with ID: " + uploadId + ". "
                        + exception.getLocalizedMessage(), exception);

            if (uploadProgressHolders.get(uploadId) == null)
                return;

            container.removeView(uploadProgressHolders.get(uploadId).itemView);
            uploadProgressHolders.remove(uploadId);
        }

        @Override
        public void onCompleted(String uploadId, int serverResponseCode, byte[] serverResponseBody) {
            Log.i(TAG, "Upload with ID " + uploadId + " is completed: " + serverResponseCode + ", "
                       + new String(serverResponseBody));

            if (uploadProgressHolders.get(uploadId) == null)
                return;

            container.removeView(uploadProgressHolders.get(uploadId).itemView);
            uploadProgressHolders.remove(uploadId);
        }

        @Override
        public void onCancelled(String uploadId) {
            Log.i(TAG, "Upload with ID " + uploadId + " is cancelled");

            if (uploadProgressHolders.get(uploadId) == null)
                return;

            container.removeView(uploadProgressHolders.get(uploadId).itemView);
            uploadProgressHolders.remove(uploadId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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

    private UploadNotificationConfig getNotificationConfig(String filename) {
        if (!displayNotification.isChecked()) return null;

        return new UploadNotificationConfig()
            .setIcon(R.drawable.ic_upload)
            .setTitle(filename)
            .setInProgressMessage(getString(R.string.uploading))
            .setCompletedMessage(getString(R.string.upload_success))
            .setErrorMessage(getString(R.string.upload_error))
            .setAutoClearOnSuccess(autoClearOnSuccess.isChecked())
            .setClickIntent(new Intent(this, MainActivity.class))
            .setClearOnAction(true)
            .setRingToneEnabled(true);
    }

    private void addUploadToList(String uploadID, String filename) {
        View uploadProgressView = getLayoutInflater().inflate(R.layout.view_upload_progress, null);
        UploadProgressViewHolder viewHolder = new UploadProgressViewHolder(uploadProgressView, filename);
        viewHolder.uploadId = uploadID;
        container.addView(viewHolder.itemView, 0);
        uploadProgressHolders.put(uploadID, viewHolder);
    }

    @OnClick(R.id.multipartUploadButton)
    void onMultipartUploadClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String paramNameString = parameterName.getText().toString();

        final String filesToUploadString = filesToUpload.getText().toString();
        final String[] filesToUploadArray = filesToUploadString.split(",");

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                final String filename = getFilename(fileToUploadPath);

                String uploadID = new MultipartUploadRequest(this, serverUrlString)
                        .addFileToUpload(fileToUploadPath, paramNameString)
                        .setNotificationConfig(getNotificationConfig(filename))
                        .setCustomUserAgent(USER_AGENT)
                        .setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteUploadedFiles.isChecked())
                        .setUsesFixedLengthStreamingMode(fixedLengthStreamingMode.isChecked())
                        .setMaxRetries(2)
                        .startUpload();

                addUploadToList(uploadID,filename);

                // these are the different exceptions that may be thrown
            } catch (FileNotFoundException exc) {
                showToast(exc.getMessage());
            } catch (IllegalArgumentException exc) {
                showToast("Missing some arguments. " + exc.getMessage());
            } catch (MalformedURLException exc) {
                showToast(exc.getMessage());
            }
        }
    }

    @OnClick(R.id.binaryUploadButton)
    void onUploadBinaryClick() {
        final String serverUrlString = serverUrl.getText().toString();

        final String filesToUploadString = filesToUpload.getText().toString();
        final String[] filesToUploadArray = filesToUploadString.split(",");

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                final String filename = getFilename(fileToUploadPath);

                String uploadID = new BinaryUploadRequest(this, serverUrlString)
                        .addHeader("file-name", new File(fileToUploadPath).getName())
                        .setFileToUpload(fileToUploadPath)
                        .setNotificationConfig(getNotificationConfig(filename))
                        .setCustomUserAgent(USER_AGENT)
                        .setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteUploadedFiles.isChecked())
                        .setUsesFixedLengthStreamingMode(fixedLengthStreamingMode.isChecked())
                        .setMaxRetries(2)
                        .startUpload();

                addUploadToList(uploadID, filename);

                // these are the different exceptions that may be thrown
            } catch (FileNotFoundException exc) {
                showToast(exc.getMessage());
            } catch (IllegalArgumentException exc) {
                showToast("Missing some arguments. " + exc.getMessage());
            } catch (MalformedURLException exc) {
                showToast(exc.getMessage());
            }
        }
    }

    @OnClick(R.id.cancelAllUploadsButton)
    void onCancelAllUploadsButtonClick() {
        UploadService.stopAllUploads();
    }

    @OnClick(R.id.pickFile)
    void onPickFileClick() {
        // Starts NoNonsense-FilePicker (https://github.com/spacecowboy/NoNonsense-FilePicker)
        Intent intent = new Intent(this, FilePickerActivity.class);

        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        // Configure initial directory by specifying a String.
        // You could specify a String like "/storage/emulated/0/", but that can
        // dangerous. Always use Android's API calls to get paths to the SD-card or
        // internal memory.
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH,
                        Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(intent, FILE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            List<Uri> resultUris = new ArrayList<>();

            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                // For JellyBean and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ClipData clip = data.getClipData();

                    if (clip != null) {
                        for (int i = 0; i < clip.getItemCount(); i++) {
                            resultUris.add(clip.getItemAt(i).getUri());
                        }
                    }

                // For Ice Cream Sandwich
                } else {
                    ArrayList<String> paths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);

                    if (paths != null) {
                        for (String path: paths) {
                            resultUris.add(Uri.parse(path));
                        }
                    }
                }
            } else {
                resultUris.add(data.getData());
            }

            StringBuilder absolutePathsConcat = new StringBuilder();
            for (Uri uri : resultUris) {
                if (absolutePathsConcat.length() == 0) {
                    absolutePathsConcat.append(new File(uri.getPath()).getAbsolutePath());
                } else {
                    absolutePathsConcat.append(",").append(new File(uri.getPath()).getAbsolutePath());
                }
            }
            filesToUpload.setText(absolutePathsConcat.toString());
        }
    }

    private String getFilename(String filepath) {
        if (filepath == null)
            return null;

        final String[] filepathParts = filepath.split("/");

        return filepathParts[filepathParts.length - 1];
    }

    class UploadProgressViewHolder {
        View itemView;

        @Bind(R.id.uploadTitle) TextView uploadTitle;
        @Bind(R.id.uploadProgress) ProgressBar progressBar;

        String uploadId;

        UploadProgressViewHolder(View view, String filename) {
            itemView = view;
            ButterKnife.bind(this, itemView);

            progressBar.setMax(100);
            progressBar.setProgress(0);

            uploadTitle.setText(getString(R.string.upload_progress, filename));
        }

        @OnClick(R.id.cancelUploadButton)
        void onCancelUploadClick() {
            if (uploadId == null)
                return;

            UploadService.stopUpload(uploadId);
        }
    }
}
