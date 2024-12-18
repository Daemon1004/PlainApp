package com.example.planeapp.ui.chats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.planeapp.data.Chat
import com.example.planeapp.data.ChatDatabase
import com.example.planeapp.data.ChatRepository

class ChatsViewModel(application: Application): AndroidViewModel(application) {

    private val readAllData: LiveData<List<Chat>>
    private val repository: ChatRepository

    init {
        val chatDao = ChatDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        readAllData = repository.readAllData
    }

}