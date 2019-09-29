package net.gotev.uploadservicedemo.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private static final int READ_REQUEST_CODE = 42;

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

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                Uri uri = resultData.getData();
                List<String> data = new ArrayList<>(1);
                data.add(uri.toString());
                onPickedFiles(data);
            }
        } else if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                ArrayList<String> extraPaths = resultData.getStringArrayListExtra(FilePickerActivity.EXTRA_PATHS);

                if (extraPaths != null) {
                    ArrayList<String> paths = new ArrayList<>(extraPaths.size());

                    for (String path : extraPaths) {
                        paths.add(Utils.getFileForUri(Uri.parse(path)).getAbsolutePath());
                    }

                    onPickedFiles(paths);
                }

            } else {
                Uri picked = resultData.getData();

                if (picked != null) {
                    onPickedFiles(Collections.singletonList(Utils.getFileForUri(picked).getAbsolutePath()));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }

    public void onPickedFiles(List<String> pickedFiles) {

    }
}
