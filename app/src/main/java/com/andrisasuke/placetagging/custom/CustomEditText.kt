package com.andrisasuke.placetagging.custom

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.AppCompatEditText
import android.util.AttributeSet
import com.andrisasuke.placetagging.R

class XEditText(context: Context?, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {

    init {
        val styledAttributes = getContext().obtainStyledAttributes(attrs, R.styleable.XEditText)
        val fontStyle = styledAttributes.getInt(R.styleable.XEditText_txtFontStyle, XEditAttr.regular)
        styledAttributes.recycle()
        val fontName: String? = when(fontStyle) {
            XEditAttr.regular -> "fonts/proxima-nova-reguler.otf"
            XEditAttr.bold -> "fonts/proxima-nova-semibold.otf"
            else -> "fonts/proxima-nova-regular.otf"
        }

        val typeface = Typeface.createFromAsset(getContext().assets, fontName)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB ||
                android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
            setTypeface(typeface)
        }
    }
}

object XEditAttr {

    // font style
    val regular = 0
    val bold = 1
    val medium = 2
    val thin = 3
    val italic = 4
}