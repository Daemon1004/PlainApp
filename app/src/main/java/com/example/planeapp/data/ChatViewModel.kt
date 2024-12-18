package com.example.planeapp.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class ChatViewModel(application: Application): AndroidViewModel(application) {

    val readAllData: LiveData<List<Chat>>
    private val repository: ChatRepository

    init {
        val chatDao = ChatDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        readAllData = repository.readAllData
    }

}