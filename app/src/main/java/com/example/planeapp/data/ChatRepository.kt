package com.example.planeapp.data

import androidx.lifecycle.LiveData

class ChatRepository(private val userDao: ChatDao) {

    val readAllData: LiveData<List<Chat>> = userDao.readAllData()

    suspend fun addUser(chat: Chat){
        userDao.addChat(chat)
    }

}