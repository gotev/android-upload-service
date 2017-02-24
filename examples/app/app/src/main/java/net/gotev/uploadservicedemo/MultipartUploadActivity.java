package net.gotev.uploadservicedemo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog;
import net.gotev.uploadservicedemo.dialogs.AddNameValueDialog;
import net.gotev.uploadservicedemo.listitems.EmptyItem;
import net.gotev.uploadservicedemo.listitems.UploadItem;
import net.gotev.uploadservicedemo.views.AddItem;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

import static net.gotev.uploadservicedemo.listitems.UploadItem.TYPE_FILE;
import static net.gotev.uploadservicedemo.listitems.UploadItem.TYPE_HEADER;
import static net.gotev.uploadservicedemo.listitems.UploadItem.TYPE_PARAMETER;

/**
 * @author Aleksandar Gotev
 */

public class MultipartUploadActivity extends FilesPickerActivity implements UploadItem.Delegate {

    @BindView(R.id.http_method)
    Spinner httpMethod;

    @BindView(R.id.server_url)
    EditText serverUrl;

    @BindView(R.id.add_file)
    AddItem addFile;

    @BindView(R.id.add_parameter)
    AddItem addParameter;

    @BindView(R.id.request_items)
    RecyclerView requestItems;

    private RecyclerAdapter adapter;
    private AddFileParameterNameDialog addFileParameterNameDialog;
    private AddNameValueDialog addHeaderDialog;
    private AddNameValueDialog addParameterDialog;
    private String fileParameterName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_multipart);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.http_methods, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        httpMethod.setAdapter(spinnerAdapter);

        adapter = new RecyclerAdapter();
        requestItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        requestItems.setAdapter(adapter);
        adapter.setEmptyItem(new EmptyItem());

        addHeaderDialog = new AddNameValueDialog(this, new AddNameValueDialog.Delegate() {
            @Override
            public void onNew(String name, String value) {
                onHeaderAdded(name, value);
            }
        }, R.string.add_header, R.string.header_name_hint, R.string.header_value_hint,
                R.string.provide_header_name, R.string.provide_header_value);

        addParameterDialog = new AddNameValueDialog(this, new AddNameValueDialog.Delegate() {
            @Override
            public void onNew(String name, String value) {
                onParameterAdded(name, value);
            }
        }, R.string.add_parameter, R.string.parameter_name_hint, R.string.parameter_value_hint,
                R.string.provide_parameter_name, R.string.provide_parameter_value);

        addFileParameterNameDialog = new AddFileParameterNameDialog(this, new AddFileParameterNameDialog.Delegate() {
            @Override
            public void onValue(String value) {
                fileParameterName = value;
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
        addParameterDialog.hide();
        addHeaderDialog.hide();
    }

    @OnClick(R.id.add_header)
    public void onAddHeader() {
        addHeaderDialog.show();
    }

    public void onHeaderAdded(String name, String value) {
        addUploadItem(UploadItem.newHeader(name, value, this));
    }

    @OnClick(R.id.add_parameter)
    public void onAddParameter() {
        addParameterDialog.show();
    }

    public void onParameterAdded(String name, String value) {
        addUploadItem(UploadItem.newParameter(name, value, this));
    }

    @OnClick(R.id.add_file)
    public void onAddFile() {
        addFileParameterNameDialog.show();
    }

    @Override
    public void onPickedFiles(List<String> pickedFiles) {
        if (fileParameterName == null || fileParameterName.isEmpty())
            return;

        addUploadItem(UploadItem.newFile(fileParameterName, pickedFiles.get(0), this));
    }

    public void onDone() {

        MultipartUploadRequest request =
                new MultipartUploadRequest(this, serverUrl.getText().toString())
                .setUtf8Charset()
                .setNotificationConfig(getNotificationConfig())
                .setMaxRetries(3)
                .setCustomUserAgent("UploadServiceDemo")
                .setUsesFixedLengthStreamingMode(true);

        request.setMethod((String) httpMethod.getSelectedItem());

        for (int i = 0; i < adapter.getItemCount(); i++) {
            AdapterItem adapterItem = adapter.getItemAtPosition(i);
            if (adapterItem == null || adapterItem.getClass().getClass() != UploadItem.class.getClass())
                continue;

            UploadItem item = (UploadItem) adapter.getItemAtPosition(i);

            switch (item.getType()) {
                case TYPE_HEADER:
                    request.addHeader(item.getTitle(), item.getSubtitle());
                    break;

                case TYPE_PARAMETER:
                    request.addParameter(item.getTitle(), item.getSubtitle());
                    break;

                case TYPE_FILE:
                    try {
                        request.addFileToUpload(item.getSubtitle(), item.getTitle());
                    } catch (IOException exc) {
                        Toast.makeText(this, getString(R.string.file_not_found, item.getSubtitle()),
                                Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }

        try {
            request.startUpload();
            finish();
        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addUploadItem(UploadItem item) {
        adapter.addOrUpdate(item);
        adapter.sort(true);
    }

    @Override
    public void onRemoveUploadItem(int position) {
        adapter.removeItemAtPosition(position);
    }

    protected UploadNotificationConfig getNotificationConfig() {
        return new UploadNotificationConfig()
                .setIcon(R.drawable.ic_upload)
                .setCompletedIcon(R.drawable.ic_upload_success)
                .setErrorIcon(R.drawable.ic_upload_error)
                .setCancelledIcon(R.drawable.ic_cancelled)
                .setIconColor(Color.BLUE)
                .setCompletedIconColor(Color.GREEN)
                .setErrorIconColor(Color.RED)
                .setCancelledIconColor(Color.YELLOW)
                .setTitle(getString(R.string.multipart_upload))
                .setInProgressMessage(getString(R.string.uploading))
                .setCompletedMessage(getString(R.string.upload_success))
                .setErrorMessage(getString(R.string.upload_error))
                .setCancelledMessage(getString(R.string.upload_cancelled))
                .setClickIntent(new Intent(this, MainActivity.class))
                .setClearOnAction(true)
                .setRingToneEnabled(true);
    }
}
