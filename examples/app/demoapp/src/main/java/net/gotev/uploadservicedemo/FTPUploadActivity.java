package net.gotev.uploadservicedemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservice.ftp.FTPUploadRequest;
import net.gotev.uploadservicedemo.adapteritems.EmptyItem;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog;
import net.gotev.uploadservicedemo.utils.FilesPickerActivity;
import net.gotev.uploadservicedemo.utils.IPAddressAndHostnameValidator;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author Aleksandar Gotev
 */

public class FTPUploadActivity extends FilesPickerActivity {

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
    private UploadItemUtils uploadItemUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_ftp);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uploadItemsAdapter = new RecyclerAdapter();
        uploadItemUtils = new UploadItemUtils(uploadItemsAdapter);
        requestItems.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
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
                        //openFilePicker(false);
                        performFileSearch();
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

            case R.id.info:
                onInfo();
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
    }

    @OnClick(R.id.add_file)
    public void onAddFile() {
        addFTPFile.show();
    }

    @Override
    public void onPickedFiles(List<String> pickedFiles) {
        if (remotePath == null || remotePath.isEmpty())
            return;


        uploadItemUtils.addFile(remotePath, pickedFiles.get(0));
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

        try {
            final FTPUploadRequest request = new FTPUploadRequest(this, serverUrl.getText().toString(), ftpPort)
                    .setNotificationConfig((context, uploadId) -> getNotificationConfig(uploadId, R.string.ftp_upload))
                    .setUsernameAndPassword(ftpUsername.getText().toString(), ftpPassword.getText().toString());

            uploadItemUtils.forEach(new UploadItemUtils.ForEachDelegate() {

                @Override
                public void onHeader(UploadItem item) {
                    // FTP does not support headers
                }

                @Override
                public void onParameter(UploadItem item) {
                    // FTP does not support parameters
                }

                @Override
                public void onFile(UploadItem item) {
                    try {
                        request.addFileToUpload(item.getSubtitle(), item.getTitle());
                    } catch (IOException exc) {
                        Toast.makeText(FTPUploadActivity.this,
                                getString(R.string.file_not_found, item.getSubtitle()),
                                Toast.LENGTH_LONG).show();
                    }
                }

            });

            request.startUpload();
            finish();

        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void onInfo() {
        openBrowser("https://github.com/gotev/android-upload-service/blob/master/uploadservice-ftp/README.md");
    }
}
