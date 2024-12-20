package com.example.planeapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addChat(chat: Chat)

    @Query("SELECT chat_table.id AS id, chat_table.name AS name, message_table.text AS lastMessage, message_table.time AS lastTime " +
            "FROM chat_table LEFT JOIN message_table ON message_table.id = " +
            "(SELECT message_table.id FROM message_table WHERE message_table.chatId = chat_table.id ORDER BY message_table.time ASC LIMIT 1)")
    fun readAllData(): LiveData<List<Chat>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMessage(message: Message)

    @Query("SELECT message_table.id AS id, message_table.chatId AS chatId, message_table.userId AS userId, " +
            "message_table.text AS text, message_table.time AS time FROM message_table " +
            "INNER JOIN user_table ON user_table.id = message_table.userId WHERE message_table.chatId = :chatId ORDER BY message_table.time ")
    fun readAllMessages(chatId: Long): LiveData<List<Message>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUser(user: User)

}