package net.gotev.uploadservicedemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservice.ftp.FTPUploadRequest;
import net.gotev.uploadservice.ftp.UnixPermissions;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog;
import net.gotev.uploadservicedemo.utils.FilesPickerActivity;
import net.gotev.uploadservicedemo.utils.IPAddressAndHostnameValidator;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static net.gotev.uploadservicedemo.adapteritems.UploadItem.TYPE_FILE;

/**
 * @author Aleksandar Gotev
 */

public class FTPUploadActivity extends FilesPickerActivity implements UploadItem.Delegate {

    public static void show(BaseActivity activity) {
        activity.startActivity(new Intent(activity, FTPUploadActivity.class));
    }

    @BindView(R.id.server_url)
    EditText serverUrl;

    @BindView(R.id.server_port)
    EditText serverPort;

    @BindView(R.id.ftp_username)
    EditText ftpUsername;

    @BindView(R.id.ftp_password)
    EditText ftpPassword;

    @BindView(R.id.request_items)
    RecyclerView requestItems;

    private RecyclerAdapter uploadItemsAdapter;
    private IPAddressAndHostnameValidator ipAddressAndHostnameValidator;
    private AddFileParameterNameDialog addFTPFile;
    private String remotePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_ftp);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uploadItemsAdapter = new RecyclerAdapter();
        requestItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        requestItems.setAdapter(uploadItemsAdapter);

        ipAddressAndHostnameValidator = new IPAddressAndHostnameValidator();

        uploadItemsAdapter.setEmptyItem(new EmptyItem(R.string.empty_ftp_upload));

        addFTPFile = new AddFileParameterNameDialog(this,
                R.string.file_remote_path_hint, R.string.provide_remote_path,
                R.string.provide_remote_path_next_instructions,
                new AddFileParameterNameDialog.Delegate() {
                    @Override
                    public void onValue(String value) {
                        remotePath = value;
                        openFilePicker(false);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_upload, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.settings:
                return true;

            case R.id.done:
                onDone();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        addFTPFile.hide();

        // hide soft keyboard if shown
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @OnClick(R.id.add_file)
    public void onAddFile() {
        addFTPFile.show();
    }

    @Override
    public void onPickedFiles(List<String> pickedFiles) {
        if (remotePath == null || remotePath.isEmpty())
            return;


        addUploadItem(UploadItem.newFile(remotePath, pickedFiles.get(0), this));
    }

    private void addUploadItem(UploadItem item) {
        uploadItemsAdapter.addOrUpdate(item);
        uploadItemsAdapter.sort(true);
    }

    @Override
    public void onRemoveUploadItem(int position) {
        uploadItemsAdapter.removeItemAtPosition(position);
    }

    public void onDone() {
        if (!ipAddressAndHostnameValidator.isValidIPorHostname(serverUrl.getText().toString())) {
            serverUrl.setError(getString(R.string.provide_valid_host));
            return;
        }

        int ftpPort;
        try {
            ftpPort = Integer.parseInt(serverPort.getText().toString(), 10);
        } catch (Exception exc) {
            serverPort.setError(getString(R.string.provide_valid_port));
            return;
        }

        FTPUploadRequest request = new FTPUploadRequest(this, serverUrl.getText().toString(), ftpPort)
                .setMaxRetries(UploadActivity.MAX_RETRIES)
                .setNotificationConfig(getNotificationConfig(R.string.ftp_upload))
                .setUsernameAndPassword(ftpUsername.getText().toString(), ftpPassword.getText().toString())
                .setCreatedDirectoriesPermissions(new UnixPermissions("777"))
                .setSocketTimeout(5000)
                .setConnectTimeout(5000);

        for (int i = 0; i < uploadItemsAdapter.getItemCount(); i++) {
            AdapterItem adapterItem = uploadItemsAdapter.getItemAtPosition(i);

            if (adapterItem != null && adapterItem.getClass().getClass() == UploadItem.class.getClass()) {
                UploadItem uploadItem = (UploadItem) adapterItem;

                switch (uploadItem.getType()) {
                    case TYPE_FILE:
                        try {
                            request.addFileToUpload(uploadItem.getSubtitle(), uploadItem.getTitle());
                        } catch (IOException exc) {
                            Toast.makeText(FTPUploadActivity.this,
                                    getString(R.string.file_not_found, uploadItem.getSubtitle()),
                                    Toast.LENGTH_LONG).show();
                        }
                        break;

                    default:
                        break;
                }
            }
        }

        try {
            request.startUpload();
            finish();
        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
