package net.gotev.uploadservicedemo;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import net.gotev.recycleradapter.AdapterItem;
import net.gotev.recycleradapter.RecyclerAdapter;
import net.gotev.uploadservicedemo.adapteritems.UploadItem;
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog;
import net.gotev.uploadservicedemo.dialogs.AddNameValueDialog;
import net.gotev.uploadservicedemo.utils.FilesPickerActivity;
import net.gotev.uploadservicedemo.views.AddItem;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * @author Aleksandar Gotev
 */

public class UploadActivity extends FilesPickerActivity implements UploadItem.Delegate {

    public interface ForEachDelegate {
        void onUploadItem(UploadItem item);
    }

    public static final int MAX_RETRIES = 3;
    public static final boolean FIXED_LENGTH_STREAMING_MODE = true;

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
        requestItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
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
        }, R.string.add_header, R.string.header_name_hint, R.string.header_value_hint,
                R.string.provide_header_name, R.string.provide_header_value);

        addParameterDialog = new AddNameValueDialog(this, new AddNameValueDialog.Delegate() {
            @Override
            public void onNew(String name, String value) {
                onParameterAdded(name, value);
            }
        }, R.string.add_parameter, R.string.parameter_name_hint, R.string.parameter_value_hint,
                R.string.provide_parameter_name, R.string.provide_parameter_value);

        addFileParameterNameDialog = new AddFileParameterNameDialog(this,
                R.string.parameter_name_hint, R.string.provide_parameter_name,
                R.string.next_instructions,
                new AddFileParameterNameDialog.Delegate() {
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

            case R.id.info:
                onInfo();
                return true;

            case R.id.done:
                onDone((String) httpMethod.getSelectedItem(), serverUrl.getText().toString(),
                        uploadItemsAdapter);
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

        // hide soft keyboard if shown
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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

    private void addUploadItem(UploadItem item) {
        uploadItemsAdapter.addOrUpdate(item);
        uploadItemsAdapter.sort(true);
    }

    @Override
    public void onRemoveUploadItem(int position) {
        uploadItemsAdapter.removeItemAtPosition(position);
    }

    protected final void forEachUploadItem(MultipartUploadActivity.ForEachDelegate delegate) {
        for (int i = 0; i < uploadItemsAdapter.getItemCount(); i++) {
            AdapterItem adapterItem = uploadItemsAdapter.getItemAtPosition(i);

            if (adapterItem != null && adapterItem.getClass().getClass() == UploadItem.class.getClass()) {
                delegate.onUploadItem((UploadItem) adapterItem);
            }
        }
    }

    public void onDone(String httpMethod, String serverUrl, RecyclerAdapter uploadItemsAdapter) {

    }

    public void onInfo() {

    }

    public AdapterItem getEmptyItem() {
        return null;
    }

    public String getUserAgent() {
        return "AndroidUploadService/" + BuildConfig.VERSION_NAME;
    }
}
