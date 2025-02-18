package com.example.plainapp.data

import androidx.lifecycle.LiveData

class ChatRepository(private val chatDao: ChatDao) {

    suspend fun addChats(chats: List<Chat>) { chatDao.addChats(chats) }
    val readAllChats: LiveData<List<Chat>> = chatDao.readAllChats()
    suspend fun deleteAllChats() { chatDao.deleteAllChats() }
    suspend fun deleteChats(chats: List<Chat>) { chatDao.deleteChats(chats) }
    fun readChat(id: Long): LiveData<Chat> { return chatDao.readChat(id) }
    fun readUser(id: Long): LiveData<User?> { return chatDao.readUser(id) }

    val readAllUsers: LiveData<List<User>> = chatDao.readAllUsers()
    fun readAllMessages(chat: Chat): LiveData<List<Message>> { return chatDao.readAllChatMessages(chat.id) }
    fun readLastChatMessage(chatId: Long): LiveData<Message?> { return chatDao.readLastChatMessage(chatId) }

    suspend fun addChat(chat: Chat){ chatDao.addChat(chat) }
    suspend fun deleteChat(chatId: Long){ chatDao.deleteChat(chatId) }

    suspend fun addChatMessage(chatId: Long, message: Message){
        chatDao.addMessage(message)
        chatDao.addChatMessage(ChatMessage(chatId, message.id))
    }

    suspend fun addUser(user: User) { chatDao.addUser(user) }
    suspend fun addUsers(users: List<User>) { chatDao.addUsers(users) }

    suspend fun updateUser(user: User) { chatDao.updateUser(user) }
    suspend fun updateUsers(users: List<User>) { chatDao.updateUsers(users) }

    fun readChatIdUser(userId: Long): LiveData<Long> { return chatDao.readChatIdUser(userId) }

}