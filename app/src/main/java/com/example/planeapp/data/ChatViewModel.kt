package com.example.planeapp.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatViewModel(application: Application): AndroidViewModel(application) {

    val readAllData: LiveData<List<Chat>>
    private val repository: ChatRepository

    init {
        val chatDao = LocalDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        readAllData = repository.readAllData
    }

    fun addChat(chat: Chat) {
        viewModelScope.launch(Dispatchers.IO) { repository.addChat(chat) }
    }

}