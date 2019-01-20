package net.gotev.uploadservicedemo.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import net.gotev.uploadservicedemo.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Aleksandar Gotev
 */

public class AddNameValueDialog implements View.OnClickListener {

    public interface Delegate {
        void onNew(String name, String value);
    }

    @BindView(R.id.parameter_name)
    EditText paramName;

    @BindView(R.id.parameter_value)
    EditText paramValue;

    private AppCompatActivity context;
    private Delegate delegate;
    private View dialogView;
    private InputMethodManager inputMethodManager;
    private AlertDialog dialog;

    private String nameErrorString;
    private String valueErrorString;
    private boolean asciiOnly;

    public AddNameValueDialog(AppCompatActivity context, Delegate delegate,
                              boolean asciiOnly, @StringRes int title,
                              @StringRes int nameHint, @StringRes int valueHint,
                              @StringRes int nameError, @StringRes int valueError) {
        this.context = context;
        this.delegate = delegate;
        this.asciiOnly = asciiOnly;

        nameErrorString = context.getString(nameError);
        valueErrorString = context.getString(valueError);

        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        dialogView = context.getLayoutInflater().inflate(R.layout.dialog_add_name_value, null);
        ButterKnife.bind(this, dialogView);

        paramName.setHint(nameHint);
        paramValue.setHint(valueHint);

        dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do not use this, as is dismisses the dialog without validation
                        // http://stackoverflow.com/questions/11363209/alertdialog-with-positive-button-and-validating-custom-edittext
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        inputMethodManager.hideSoftInputFromWindow(paramName.getWindowToken(), 0);
                    }
                })
                .create();
    }

    @Override
    public void onClick(View view) {
        if (paramName.getText().toString().trim().isEmpty()) {
            paramName.setError(nameErrorString);
            return;
        }

        if (paramName.getText().toString().contains(" ")) {
            paramName.setError(view.getContext().getString(R.string.no_whitespaces_here));
            return;
        }

        if (paramValue.getText().toString().trim().isEmpty()) {
            paramValue.setError(valueErrorString);
            return;
        }

        if (asciiOnly) {
            boolean fail = false;

            if (!isAllASCII(paramName.getText().toString())) {
                fail = true;
                paramName.setError(view.getContext().getString(R.string.use_only_ascii));
            }

            if (!isAllASCII(paramValue.getText().toString())) {
                fail = true;
                paramValue.setError(view.getContext().getString(R.string.use_only_ascii));
            }

            if (fail)
                return;
        }

        delegate.onNew(paramName.getText().toString().trim(), paramValue.getText().toString().trim());

        hide();
    }

    public void show() {
        paramName.setText("");
        paramValue.setText("");
        paramName.setError(null);
        paramValue.setError(null);
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);

        paramName.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void hide() {
        if (dialog.isShowing()) {
            inputMethodManager.hideSoftInputFromWindow(paramName.getWindowToken(), 0);
            dialog.dismiss();
        }
    }

    private boolean isAllASCII(String input) {
        if (input == null || input.isEmpty())
            return false;

        boolean isASCII = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c > 127) {
                isASCII = false;
                break;
            }
        }
        return isASCII;
    }
}
