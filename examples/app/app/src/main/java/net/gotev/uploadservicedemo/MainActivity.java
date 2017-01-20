package net.gotev.uploadservicedemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.gotev.uploadservice.BinaryUploadRequest;
import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadStatusDelegate;
import net.gotev.uploadservice.ftp.FTPUploadRequest;
import net.gotev.uploadservice.ftp.UnixPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Activity that demonstrates how to use Android Upload Service.
 *
 * @author gotev (Aleksandar Gotev)
 * @author mabdurrahman
 *
 */
public class MainActivity extends FilesPickerActivity implements UploadStatusDelegate {

    private static final String TAG = "UploadServiceDemo";
    private static final String USER_AGENT = "UploadServiceDemo/" + BuildConfig.VERSION_NAME;

    private static final String FTP_USERNAME = "ftpuser";
    private static final String FTP_PASSWORD = "testpassword";
    private static final String FTP_REMOTE_BASE_PATH = "home/ftpuser/";

    @BindView(R.id.container) ViewGroup container;
    @BindView(R.id.multipartUploadButton) Button multipartUploadButton;
    @BindView(R.id.binaryUploadButton) Button binaryUploadButton;
    @BindView(R.id.cancelAllUploadsButton) Button cancelAllUploadsButton;
    @BindView(R.id.serverURL) EditText serverUrl;
    @BindView(R.id.filesToUpload) EditText filesToUpload;
    @BindView(R.id.parameterName) EditText parameterName;
    @BindView(R.id.displayNotification) CheckBox displayNotification;
    @BindView(R.id.autoDeleteUploadedFiles) CheckBox autoDeleteUploadedFiles;
    @BindView(R.id.autoClearNotification) CheckBox autoClearNotification;
    @BindView(R.id.fixedLengthStreamingMode) CheckBox fixedLengthStreamingMode;
    @BindView(R.id.useUtf8) CheckBox useUtf8;

    private Map<String, UploadProgressViewHolder> uploadProgressHolders = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void logSuccessfullyUploadedFiles(List<String> files) {
        for (String file : files) {
            Log.e(TAG, "Success: " + file);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private UploadNotificationConfig getNotificationConfig(String filename) {
        if (!displayNotification.isChecked()) return null;

        return new UploadNotificationConfig()
                .setIcon(R.drawable.ic_upload)
                .setCompletedIcon(R.drawable.ic_upload_success)
                .setErrorIcon(R.drawable.ic_upload_error)
                .setCancelledIcon(R.drawable.ic_cancelled)
                .setTitle(filename)
                .setInProgressMessage(getString(R.string.uploading))
                .setCompletedMessage(getString(R.string.upload_success))
                .setErrorMessage(getString(R.string.upload_error))
                .setCancelledMessage(getString(R.string.upload_cancelled))
                .setAutoClearOnCancel(autoClearNotification.isChecked())
                .setAutoClearOnSuccess(autoClearNotification.isChecked())
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
    public void onMultipartUploadClick() {
        final String serverUrlString = serverUrl.getText().toString();
        final String paramNameString = parameterName.getText().toString();

        final String filesToUploadString = filesToUpload.getText().toString();
        final String[] filesToUploadArray = filesToUploadString.split("\\|\\|");

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                final String filename = getFilename(fileToUploadPath);

                MultipartUploadRequest req = new MultipartUploadRequest(this, serverUrlString)
                        .addFileToUpload(fileToUploadPath, paramNameString)
                        .setNotificationConfig(getNotificationConfig(filename))
                        .setCustomUserAgent(USER_AGENT)
                        .setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteUploadedFiles.isChecked())
                        .setUsesFixedLengthStreamingMode(fixedLengthStreamingMode.isChecked())
                        .setMaxRetries(3);

                if (useUtf8.isChecked()) {
                    req.setUtf8Charset();
                }

                String uploadID = req.setDelegate(this).startUpload();

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
    public void onUploadBinaryClick() {
        final String serverUrlString = serverUrl.getText().toString();

        final String filesToUploadString = filesToUpload.getText().toString();
        final String[] filesToUploadArray = filesToUploadString.split(",");

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                final String filename = getFilename(fileToUploadPath);

                final String uploadID = new BinaryUploadRequest(this, serverUrlString)
                        .addHeader("file-name", new File(fileToUploadPath).getName())
                        .setFileToUpload(fileToUploadPath)
                        .setNotificationConfig(getNotificationConfig(filename))
                        .setCustomUserAgent(USER_AGENT)
                        .setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteUploadedFiles.isChecked())
                        .setUsesFixedLengthStreamingMode(fixedLengthStreamingMode.isChecked())
                        .setMaxRetries(2)
                        .setDelegate(this)
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

    @OnClick(R.id.ftpUploadButton)
    public void onUploadFTPClick() {
        String serverUrlString = serverUrl.getText().toString();

        int ftpPort = 21;
        if (serverUrlString.contains(":")) {
            try {
                String[] tmp = serverUrlString.split(":");
                serverUrlString = tmp[0];
                ftpPort = Integer.parseInt(tmp[1]);
            } catch (Exception exc) {
                Log.e(getClass().getSimpleName(), "error while getting FTP port from: " + serverUrlString, exc);
            }
        }

        final String filesToUploadString = filesToUpload.getText().toString();
        final String[] filesToUploadArray = filesToUploadString.split(",");

        FTPUploadRequest request = new FTPUploadRequest(this, serverUrlString, ftpPort)
                .setUsernameAndPassword(FTP_USERNAME, FTP_PASSWORD)
                .setMaxRetries(4)
                .setNotificationConfig(getNotificationConfig("FTP upload"))
                .useCompressedFileTransferMode(true)
                .setCreatedDirectoriesPermissions(new UnixPermissions("777"))
                .setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteUploadedFiles.isChecked());

        for (String fileToUploadPath : filesToUploadArray) {
            try {
                request.addFileToUpload(fileToUploadPath, FTP_REMOTE_BASE_PATH, new UnixPermissions("777"));
            } catch (FileNotFoundException exc) {
                showToast(exc.getMessage());
            } catch (IllegalArgumentException exc) {
                showToast("Missing some arguments. " + exc.getMessage());
            }
        }

        try {
            String uploadID = request.setDelegate(this).startUpload();
            addUploadToList(uploadID, "FTP upload");

        } catch (IllegalArgumentException exc) {
            showToast("Missing some arguments. " + exc.getMessage());
        } catch (MalformedURLException exc) {
            showToast(exc.getMessage());
        }
    }

    @OnClick(R.id.cancelAllUploadsButton)
    public void onCancelAllUploadsButtonClick() {
        UploadService.stopAllUploads();
    }

    @OnClick(R.id.pickFile)
    public void onPickFileClick() {
        openFilePicker(false);
    }

    @Override
    public void onPickedFiles(List<String> pickedFiles) {
        StringBuilder absolutePathsConcat = new StringBuilder();
        for (String file : pickedFiles) {
            if (absolutePathsConcat.length() == 0) {
                absolutePathsConcat.append(new File(file).getAbsolutePath());
            } else {
                absolutePathsConcat.append("||").append(new File(file).getAbsolutePath());
            }
        }
        filesToUpload.setText(absolutePathsConcat.toString());
    }

    private String getFilename(String filepath) {
        if (filepath == null)
            return null;

        final String[] filepathParts = filepath.split("/");

        return filepathParts[filepathParts.length - 1];
    }

    class UploadProgressViewHolder {
        View itemView;

        @BindView(R.id.uploadTitle) TextView uploadTitle;
        @BindView(R.id.uploadProgress) ProgressBar progressBar;

        String uploadId;

        UploadProgressViewHolder(View view, String filename) {
            itemView = view;
            ButterKnife.bind(this, itemView);

            progressBar.setMax(100);
            progressBar.setProgress(0);

            uploadTitle.setText(getString(R.string.upload_progress, filename));
        }

        @OnClick(R.id.cancelUploadButton)
        public void onCancelUploadClick() {
            if (uploadId == null)
                return;

            UploadService.stopUpload(uploadId);
        }
    }

    @Override
    public void onProgress(Context context, UploadInfo uploadInfo) {
        Log.i(TAG, String.format(Locale.getDefault(), "ID: %1$s (%2$d%%) at %3$.2f Kbit/s",
                uploadInfo.getUploadId(), uploadInfo.getProgressPercent(),
                uploadInfo.getUploadRate()));
        logSuccessfullyUploadedFiles(uploadInfo.getSuccessfullyUploadedFiles());

        if (uploadProgressHolders.get(uploadInfo.getUploadId()) == null)
            return;

        uploadProgressHolders.get(uploadInfo.getUploadId())
                .progressBar.setProgress(uploadInfo.getProgressPercent());
    }

    @Override
    public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
        Log.e(TAG, "Error with ID: " + uploadInfo.getUploadId() + ": "
                + exception.getLocalizedMessage(), exception);
        logSuccessfullyUploadedFiles(uploadInfo.getSuccessfullyUploadedFiles());

        if (uploadProgressHolders.get(uploadInfo.getUploadId()) == null)
            return;

        container.removeView(uploadProgressHolders.get(uploadInfo.getUploadId()).itemView);
        uploadProgressHolders.remove(uploadInfo.getUploadId());
    }

    @Override
    public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
        Log.i(TAG, String.format(Locale.getDefault(),
                "ID %1$s: completed in %2$ds at %3$.2f Kbit/s. Response code: %4$d, body:[%5$s]",
                uploadInfo.getUploadId(), uploadInfo.getElapsedTime() / 1000,
                uploadInfo.getUploadRate(), serverResponse.getHttpCode(),
                serverResponse.getBodyAsString()));
        logSuccessfullyUploadedFiles(uploadInfo.getSuccessfullyUploadedFiles());
        for (Map.Entry<String, String> header : serverResponse.getHeaders().entrySet()) {
            Log.i("Header", header.getKey() + ": " + header.getValue());
        }

        Log.e(TAG, "Printing response body bytes");
        byte[] ba = serverResponse.getBody();
        for (int j = 0; j < ba.length; j++) {
            Log.e(TAG, String.format("%02X ", ba[j]));
        }

        if (uploadProgressHolders.get(uploadInfo.getUploadId()) == null)
            return;

        container.removeView(uploadProgressHolders.get(uploadInfo.getUploadId()).itemView);
        uploadProgressHolders.remove(uploadInfo.getUploadId());
    }

    @Override
    public void onCancelled(Context context, UploadInfo uploadInfo) {
        Log.i(TAG, "Upload with ID " + uploadInfo.getUploadId() + " is cancelled");
        logSuccessfullyUploadedFiles(uploadInfo.getSuccessfullyUploadedFiles());

        if (uploadProgressHolders.get(uploadInfo.getUploadId()) == null)
            return;

        container.removeView(uploadProgressHolders.get(uploadInfo.getUploadId()).itemView);
        uploadProgressHolders.remove(uploadInfo.getUploadId());
    }
}
