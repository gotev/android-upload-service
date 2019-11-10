package net.gotev.uploadservicedemo;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog;
import net.gotev.uploadservicedemo.dialogs.AddNameValueDialog;
import net.gotev.uploadservicedemo.utils.FilesPickerActivity;
import net.gotev.uploadservicedemo.utils.UploadItemUtils;
import net.gotev.uploadservicedemo.views.AddItem;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author Aleksandar Gotev
 */

public abstract class UploadActivity extends FilesPickerActivity {

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

    private RecyclerAdapter uploadItemsAdapter;
    private AddFileParameterNameDialog addFileParameterNameDialog;
    private AddNameValueDialog addHeaderDialog;
    private AddNameValueDialog addParameterDialog;
    protected String fileParameterName;
    private UploadItemUtils uploadItemUtils;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.http_methods, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        httpMethod.setAdapter(spinnerAdapter);

        uploadItemsAdapter = new RecyclerAdapter();
        uploadItemUtils = new UploadItemUtils(uploadItemsAdapter);
        requestItems.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        requestItems.setAdapter(uploadItemsAdapter);

        AdapterItem emptyItem = getEmptyItem();
        if (emptyItem != null) {
            uploadItemsAdapter.setEmptyItem(emptyItem);
        }

        addHeaderDialog = new AddNameValueDialog(this, new AddNameValueDialog.Delegate() {
            @Override
            public void onNew(String name, String value) {
                onHeaderAdded(name, value);
            }
        }, true, R.string.add_header, R.string.header_name_hint, R.string.header_value_hint,
                R.string.provide_header_name, R.string.provide_header_value);

        addParameterDialog = new AddNameValueDialog(this, new AddNameValueDialog.Delegate() {
            @Override
            public void onNew(String name, String value) {
                onParameterAdded(name, value);
            }
        }, false, R.string.add_parameter, R.string.parameter_name_hint, R.string.parameter_value_hint,
                R.string.provide_parameter_name, R.string.provide_parameter_value);

        addFileParameterNameDialog = new AddFileParameterNameDialog(this,
                R.string.parameter_name_hint, R.string.provide_parameter_name,
                R.string.next_instructions,
                new AddFileParameterNameDialog.Delegate() {
                    @Override
                    public void onValue(String value) {
                        fileParameterName = value;
                        //openFilePicker(false); // Bundled file picker
                        performFileSearch(); // System file picker
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
                onDone((String) httpMethod.getSelectedItem(), serverUrl.getText().toString(),
                        uploadItemUtils);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        addParameterDialog.hide();
        addHeaderDialog.hide();
        addFileParameterNameDialog.hide();
    }

    @OnClick(R.id.add_header)
    public void onAddHeader() {
        addHeaderDialog.show();
    }

    public void onHeaderAdded(String name, String value) {
        uploadItemUtils.addHeader(name, value);
    }

    @OnClick(R.id.add_parameter)
    public void onAddParameter() {
        addParameterDialog.show();
    }

    public void onParameterAdded(String name, String value) {
        uploadItemUtils.addParameter(name, value);
    }

    @OnClick(R.id.add_file)
    public void onAddFile() {
        addFileParameterNameDialog.show();
    }

    @Override
    public void onPickedFiles(List<String> pickedFiles) {
        if (fileParameterName == null || fileParameterName.isEmpty())
            return;

        uploadItemUtils.addFile(fileParameterName, pickedFiles.get(0));
    }

    public String getUserAgent() {
        return "AndroidUploadServiceDemo/" + BuildConfig.VERSION_NAME;
    }

    public abstract AdapterItem getEmptyItem();

    public abstract void onDone(String httpMethod, String serverUrl, UploadItemUtils uploadItemUtils);

    public abstract void onInfo();
}
