package net.gotev.uploadservicedemo.activities

import android.content.Intent
import java.util.ArrayList

open class FilePickerActivity : BaseActivity() {

    companion object {
        private const val READ_REQUEST_CODE = 42
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    fun openFilePicker() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            type = "*/*"

            // Get read URI permission and persistable URI permission
            flags =
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                val uri = resultData.data
                val data: MutableList<String> = ArrayList(1)
                data.add(uri.toString())
                onPickedFiles(data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData)
        }
    }

    open fun onPickedFiles(pickedFiles: List<String>) {}
}
