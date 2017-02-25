package net.gotev.uploadservicedemo.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import net.gotev.uploadservicedemo.BaseActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Aleksandar Gotev
 */

public class FilesPickerActivity extends BaseActivity {

    private static final int FILE_CODE = 1;
    private static final int PERMISSIONS_REQUEST_CODE = 2;

    private AndroidPermissions mPermissions;
    private boolean mEnableMultipleSelections;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissions = new AndroidPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(getClass().getSimpleName(), "onRequestPermissionsResult");

        if (mPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
            startIntent();
        } else {
            Toast.makeText(this, "Please grant permissions to be able to select files", Toast.LENGTH_SHORT).show();
        }
    }

    public final void openFilePicker(boolean enableMultipleSelections) {
        mEnableMultipleSelections = enableMultipleSelections;
        if (mPermissions.checkPermissions()) {
            startIntent();
        } else {
            Log.i(getClass().getSimpleName(), "Some needed permissions are missing. Requesting them.");
            mPermissions.requestPermissions(PERMISSIONS_REQUEST_CODE);
        }
    }

    private void startIntent() {
        // Starts NoNonsense-FilePicker (https://github.com/spacecowboy/NoNonsense-FilePicker)
        Intent intent = new Intent(this, BackHandlingFilePickerActivity.class);

        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, mEnableMultipleSelections);
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
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                ArrayList<String> extraPaths = data.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);

                if (extraPaths != null) {
                    ArrayList<String> paths = new ArrayList<>(extraPaths.size());

                    for (String path : extraPaths) {
                        paths.add(Utils.getFileForUri(Uri.parse(path)).getAbsolutePath());
                    }

                    onPickedFiles(paths);
                }

            } else {
                Uri picked = data.getData();

                if (picked != null) {
                    onPickedFiles(Collections.singletonList(Utils.getFileForUri(picked).getAbsolutePath()));
                }
            }
        }
    }

    public void onPickedFiles(List<String> pickedFiles) {

    }
}
