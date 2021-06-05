package net.gotev.uploadservicedemo.dialogs

import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import net.gotev.uploadservicedemo.R
import net.gotev.uploadservicedemo.extensions.inputMethodManager

class AddFileParameterNameDialog(
    context: AppCompatActivity,
    @StringRes hint: Int,
    @StringRes errorMessage: Int,
    @StringRes detailsMessage: Int,
    private val delegate: (value: String) -> Unit
) : View.OnClickListener {
    private val dialogView: View =
        context.layoutInflater.inflate(R.layout.dialog_add_file_parameter_name, null)
    private val input: EditText = dialogView.findViewById(R.id.input)
    private val details: TextView = dialogView.findViewById(R.id.details)
    private val valueErrorString: String = context.getString(errorMessage)
    private val dialog: AlertDialog

    init {
        input.setHint(hint)
        details.text = context.getString(detailsMessage)

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.add_file)
            .setView(dialogView)
            .setPositiveButton(R.string.next) { _, _ ->
                // do not use this, as is dismisses the dialog without validation
                // http://stackoverflow.com/questions/11363209/alertdialog-with-positive-button-and-validating-custom-edittext
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                context.inputMethodManager.hideSoftInputFromWindow(input.windowToken, 0)
            }
            .create()
    }

    override fun onClick(view: View) {
        val inputString = input.text.toString()

        if (inputString.isBlank()) {
            input.error = valueErrorString
            return
        }

        if (inputString.contains(" ")) {
            input.error = view.context.getString(R.string.no_whitespaces_here)
            return
        }

        delegate(inputString.trim())

        hide()
    }

    fun show() {
        input.setText("")
        input.error = null

        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this)

        input.requestFocus()
        dialog.context.inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    fun hide() {
        if (dialog.isShowing) {
            dialog.context.inputMethodManager.hideSoftInputFromWindow(input.windowToken, 0)
            dialog.dismiss()
        }
    }
}
