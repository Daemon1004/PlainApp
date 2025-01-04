package com.example.plainapp.rtc.utils

import com.example.plainapp.rtc.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(type: String, message: MessageModel)
}