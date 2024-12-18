package com.example.planeapp.ui.chats

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.planeapp.R

class ChatPreviewView(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ChatPanelView)
    private val view: View = View.inflate(context, R.layout.chat_preview_view, this)

    init {
        view.findViewById<TextView>(R.id.name).text = attributes.getString(R.styleable.ChatPanelView_username)
        view.findViewById<TextView>(R.id.lastMessage).text = attributes.getString(R.styleable.ChatPanelView_lastmessage)
        view.findViewById<TextView>(R.id.lastTime).text = attributes.getString(R.styleable.ChatPanelView_lasttime)
    }

}