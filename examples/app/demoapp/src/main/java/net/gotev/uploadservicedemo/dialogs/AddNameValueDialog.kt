package net.gotev.uploadservicedemo.dialogs

import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import net.gotev.uploadservice.extensions.isASCII
import net.gotev.uploadservicedemo.R
import net.gotev.uploadservicedemo.extensions.inputMethodManager

/**
 * @author Aleksandar Gotev
 */
class AddNameValueDialog(
    context: AppCompatActivity,
    private val delegate: (name: String, value: String) -> Unit,
    private val asciiOnly: Boolean,
    @StringRes title: Int,
    @StringRes nameHint: Int,
    @StringRes valueHint: Int,
    @StringRes nameError: Int,
    @StringRes valueError: Int
) : View.OnClickListener {
    private val nameErrorString: String = context.getString(nameError)
    private val valueErrorString: String = context.getString(valueError)

    private val dialogView: View =
        context.layoutInflater.inflate(R.layout.dialog_add_name_value, null)
    val paramName: EditText = dialogView.findViewById(R.id.parameter_name)
    val paramValue: EditText = dialogView.findViewById(R.id.parameter_value)

    private val dialog: AlertDialog

    init {
        paramName.setHint(nameHint)
        paramValue.setHint(valueHint)

        dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.add) { _, _ ->
                // do not use this, as is dismisses the dialog without validation
                // http://stackoverflow.com/questions/11363209/alertdialog-with-positive-button-and-validating-custom-edittext
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                context.inputMethodManager.hideSoftInputFromWindow(paramName.windowToken, 0)
            }
            .create()
    }

    override fun onClick(view: View) {
        if (paramName.text.toString().isBlank()) {
            paramName.error = nameErrorString
            return
        }

        if (paramName.text.toString().contains(" ")) {
            paramName.error = view.context.getString(R.string.no_whitespaces_here)
            return
        }

        if (paramValue.text.toString().isBlank()) {
            paramValue.error = valueErrorString
            return
        }

        if (asciiOnly) {
            var fail = false

            if (!paramName.text.toString().isASCII()) {
                fail = true
                paramName.error = view.context.getString(R.string.use_only_ascii)
            }

            if (!paramValue.text.toString().isASCII()) {
                fail = true
                paramValue.error = view.context.getString(R.string.use_only_ascii)
            }

            if (fail) return
        }

        delegate(paramName.text.toString().trim(), paramValue.text.toString().trim())

        hide()
    }

    fun show() {
        paramName.setText("")
        paramValue.setText("")

        paramName.error = null
        paramValue.error = null

        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this)

        paramName.requestFocus()
        dialog.context.inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun hide() {
        if (dialog.isShowing) {
            dialog.context.inputMethodManager.hideSoftInputFromWindow(paramName.windowToken, 0)
            dialog.dismiss()
        }
    }
}
