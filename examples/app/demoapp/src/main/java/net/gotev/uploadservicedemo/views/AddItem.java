package net.gotev.uploadservicedemo.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gotev.uploadservicedemo.R;

/**
 * @author Aleksandar Gotev
 */

public class AddItem extends LinearLayout {

    protected ImageView image;
    protected TextView title;

    public AddItem(Context context) {
        super(context);
        init(context, null);
    }

    public AddItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AddItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(21)
    public AddItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (isInEditMode())
            return;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.item_add, this);

        image = (ImageView) findViewById(R.id.image);
        title = (TextView) findViewById(R.id.title);

        if (attrs == null)
            return;

        TypedArray types = context.obtainStyledAttributes(attrs, R.styleable.AddItemView, 0, 0);
        if (types != null) {
            handleTypedArray(types);
            types.recycle();
        }

    }

    private void handleTypedArray(TypedArray types) {
        String titleText = types.getString(R.styleable.AddItemView_titleText);

        if (titleText != null)
            title.setText(titleText);

        int valueColor = types.getColor(R.styleable.AddItemView_colorFilter, -1);

        if (valueColor != -1)
            image.setColorFilter(valueColor, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            if(hasOnClickListeners()) {
                callOnClick();
            }
        }
        return super.dispatchTouchEvent(event);
    }

    public AddItem setTitleText(String newTitleText) {
        title.setText(newTitleText);
        return this;
    }
}
