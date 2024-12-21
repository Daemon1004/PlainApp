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

    fun readAllMessages(chat: Chat): LiveData<List<Message>> { return repository.readAllMessages(chat) }

    fun readChat(id: Long): LiveData<Chat> { return repository.readChat(id) }

    fun addChat(chat: Chat) {
        viewModelScope.launch(Dispatchers.IO) { repository.addChat(chat) }
    }

    fun addMessage(chat: Chat, text: String) {
        viewModelScope.launch(Dispatchers.IO) { repository.addMessage(chat, text) }
    }

}