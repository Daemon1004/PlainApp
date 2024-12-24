package com.example.planeapp.data

import androidx.lifecycle.LiveData

class ChatRepository(private val chatDao: ChatDao) {

    val readAllData: LiveData<List<Chat>> = chatDao.readAllData()
    fun readAllMessages(chat: Chat): LiveData<List<Message>> { return chatDao.readAllMessages(chat.id) }
    fun readChat(id: Long): LiveData<Chat> { return chatDao.readChat(id) }

    suspend fun addChat(chat: Chat){
        chatDao.addChat(chat)
    }

    suspend fun addMessage(chat: Chat, text: String){
        //chatDao.addUser(User(1, "Me"))
        chatDao.addMessage(Message(
            chatId = chat.id,
            userId = 1,
            text = text,
            time = System.currentTimeMillis(),
        ))
    }

}