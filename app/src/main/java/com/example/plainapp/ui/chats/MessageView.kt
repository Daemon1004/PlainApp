package com.example.plainapp.ui.chats

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.plainapp.R

class MessageView(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.MessageView)
    private val view: View = View.inflate(context, R.layout.message_view, this)

    init {

        view.findViewById<TextView>(R.id.message).text = attributes.getString(R.styleable.MessageView_message)
        view.findViewById<TextView>(R.id.time).text = attributes.getString(R.styleable.MessageView_time)

    }

}