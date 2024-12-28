package com.example.plainapp.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class ChatViewModel(application: Application): AndroidViewModel(application) {

    val readAllChats: LiveData<List<Chat>>
    private val repository: ChatRepository

    init {
        val chatDao = LocalDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        readAllChats = repository.readAllChats
    }

    fun readAllMessages(chat: Chat): LiveData<List<Message>> { return repository.readAllMessages(chat) }

    fun readChat(id: Long): LiveData<Chat> { return repository.readChat(id) }

    fun readUser(id: Long): LiveData<User> { return repository.readUser(id) }

}