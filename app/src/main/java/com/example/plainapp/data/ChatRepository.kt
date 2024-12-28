package com.example.plainapp.data

import androidx.lifecycle.LiveData

class ChatRepository(private val chatDao: ChatDao) {

    suspend fun writeChats(chats: List<Chat>) { chatDao.writeChats(chats) }
    val readAllChats: LiveData<List<Chat>> = chatDao.readAllChats()
    suspend fun deleteAllChats() { chatDao.deleteAllChats() }
    fun readChat(id: Long): LiveData<Chat> { return chatDao.readChat(id) }

    fun readAllMessages(chat: Chat): LiveData<List<Message>> { return chatDao.readAllChatMessages(chat.id) }

    suspend fun addChat(chat: Chat){ chatDao.addChat(chat) }
    suspend fun deleteChat(chatId: Long){ chatDao.deleteChat(chatId) }

    suspend fun addChatMessage(chatId: Long, message: Message){
        chatDao.addMessage(message)
        chatDao.addChatMessage(ChatMessage(chatId, message.id))
    }

    suspend fun addUser(user: User) { chatDao.addUser(user) }
    suspend fun addUsers(users: List<User>) { chatDao.addUsers(users) }

}