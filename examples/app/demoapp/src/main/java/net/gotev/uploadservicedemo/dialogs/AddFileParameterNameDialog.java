package net.gotev.uploadservicedemo.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import net.gotev.uploadservicedemo.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author Aleksandar Gotev
 */

public class AddFileParameterNameDialog implements View.OnClickListener {

    public interface Delegate {
        void onValue(String value);
    }

    @BindView(R.id.input)
    EditText input;

    @BindView(R.id.details)
    TextView details;

    private Delegate delegate;
    private View dialogView;
    private InputMethodManager inputMethodManager;
    private AlertDialog dialog;

    private String valueErrorString;

    public AddFileParameterNameDialog(AppCompatActivity context,
                                      @StringRes int hint,
                                      @StringRes int errorMessage,
                                      @StringRes int detailsMessage,
                                      Delegate delegate) {
        this.delegate = delegate;

        valueErrorString = context.getString(errorMessage);

        inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        dialogView = context.getLayoutInflater().inflate(R.layout.dialog_add_file_parameter_name, null);
        ButterKnife.bind(this, dialogView);

        input.setHint(hint);
        details.setText(context.getString(detailsMessage));

        dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.add_file)
                .setView(dialogView)
                .setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do not use this, as is dismisses the dialog without validation
                        // http://stackoverflow.com/questions/11363209/alertdialog-with-positive-button-and-validating-custom-edittext
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    }
                })
                .create();
    }

    @Override
    public void onClick(View view) {
        if (input.getText().toString().trim().isEmpty()) {
            input.setError(valueErrorString);
            return;
        }

        if (input.getText().toString().contains(" ")) {
            input.setError(view.getContext().getString(R.string.no_whitespaces_here));
            return;
        }

        delegate.onValue(input.getText().toString().trim());

        hide();
    }

    public void show() {
        input.setText("");
        input.setError(null);
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this);

        input.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void hide() {
        if (dialog.isShowing()) {
            inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
            dialog.dismiss();
        }
    }
}
