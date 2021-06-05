package net.gotev.uploadservicedemo.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.gotev.recycleradapter.AdapterItem
import net.gotev.recycleradapter.RecyclerAdapter
import net.gotev.uploadservicedemo.R
import net.gotev.uploadservicedemo.dialogs.AddFileParameterNameDialog
import net.gotev.uploadservicedemo.dialogs.AddNameValueDialog
import net.gotev.uploadservicedemo.utils.UploadItemUtils
import net.gotev.uploadservicedemo.views.AddItem

abstract class HttpUploadActivity : FilePickerActivity() {

    val httpMethod: Spinner by lazy { findViewById(R.id.http_method) }
    val serverUrl: EditText by lazy { findViewById(R.id.server_url) }
    val addHeader: AddItem by lazy { findViewById(R.id.add_header) }
    val addParameter: AddItem by lazy { findViewById(R.id.add_parameter) }
    val addFile: AddItem by lazy { findViewById(R.id.add_file) }

    private val uploadItemsAdapter = RecyclerAdapter()
    private val uploadItemUtils = UploadItemUtils(uploadItemsAdapter)
    protected var fileParameterName: String? = null

    private val addHeaderDialog by lazy {
        AddNameValueDialog(
            context = this,
            delegate = { name, value -> uploadItemUtils.addHeader(name, value) },
            asciiOnly = true,
            title = R.string.add_header,
            nameHint = R.string.header_name_hint,
            valueHint = R.string.header_value_hint,
            nameError = R.string.provide_header_name,
            valueError = R.string.provide_header_value
        )
    }

    private val addParameterDialog by lazy {
        AddNameValueDialog(
            context = this,
            delegate = { name, value -> uploadItemUtils.addParameter(name, value) },
            asciiOnly = false,
            title = R.string.add_parameter,
            nameHint = R.string.parameter_name_hint,
            valueHint = R.string.parameter_value_hint,
            nameError = R.string.provide_parameter_name,
            valueError = R.string.provide_parameter_value
        )
    }

    private val addFileParameterNameDialog: AddFileParameterNameDialog by lazy {
        AddFileParameterNameDialog(
            context = this,
            hint = R.string.parameter_name_hint,
            errorMessage = R.string.provide_parameter_name,
            detailsMessage = R.string.next_instructions,
            delegate = { value ->
                fileParameterName = value
                openFilePicker()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        supportActionBar?.apply {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        httpMethod.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.http_methods,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        findViewById<RecyclerView>(R.id.request_items).apply {
            layoutManager =
                LinearLayoutManager(this@HttpUploadActivity, RecyclerView.VERTICAL, false)
            adapter = uploadItemsAdapter
        }

        emptyItem?.let {
            uploadItemsAdapter.setEmptyItem(it)
        }

        addHeader.setOnClickListener {
            addHeaderDialog.show()
        }

        addParameter.setOnClickListener {
            addParameterDialog.show()
        }

        addFile.setOnClickListener {
            addFileParameterNameDialog.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_upload, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(this)
                return true
            }

            R.id.settings -> return true

            R.id.info -> {
                onInfo()
                return true
            }

            R.id.done -> {
                onDone(
                    httpMethod = httpMethod.selectedItem as String,
                    serverUrl = serverUrl.text.toString(),
                    uploadItemUtils = uploadItemUtils
                )
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        addParameterDialog.hide()
        addHeaderDialog.hide()
        addFileParameterNameDialog.hide()
    }

    override fun onPickedFiles(pickedFiles: List<String>) {
        val paramName = fileParameterName
        if (paramName.isNullOrBlank()) return
        uploadItemUtils.addFile(paramName, pickedFiles.first())
    }

    abstract val emptyItem: AdapterItem<*>?
    abstract fun onDone(httpMethod: String, serverUrl: String, uploadItemUtils: UploadItemUtils)
    abstract fun onInfo()
}
