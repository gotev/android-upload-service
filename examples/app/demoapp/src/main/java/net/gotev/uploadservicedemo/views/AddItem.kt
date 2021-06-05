package net.gotev.uploadservicedemo.views

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.gotev.uploadservicedemo.R

class AddItem : LinearLayout {
    protected lateinit var image: ImageView
    protected lateinit var title: TextView

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    @TargetApi(21)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (isInEditMode) return
        LayoutInflater.from(context).inflate(R.layout.item_add, this)
        image = findViewById(R.id.image)
        title = findViewById(R.id.title)

        if (attrs == null) return

        val types = context.obtainStyledAttributes(attrs, R.styleable.AddItemView, 0, 0)
        handleTypedArray(types)
        types.recycle()
    }

    private fun handleTypedArray(types: TypedArray) {
        val titleText = types.getString(R.styleable.AddItemView_titleText)
        if (titleText != null) title.text = titleText
        val valueColor = types.getColor(R.styleable.AddItemView_colorFilter, -1)
        if (valueColor != -1) image.setColorFilter(valueColor, PorterDuff.Mode.SRC_ATOP)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (hasOnClickListeners()) {
                callOnClick()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    fun setTitleText(newTitleText: String?): AddItem {
        title.text = newTitleText
        return this
    }
}
