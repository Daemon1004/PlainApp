package com.example.plainapp.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChats(chats: List<Chat>)

    @Delete
    suspend fun deleteChats(chats: List<Chat>)

    @Query("DELETE FROM chat_table")
    suspend fun deleteAllChats()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChat(chat: Chat)

    @Query("DELETE FROM chat_table WHERE id = :chatId")
    suspend fun deleteChat(chatId: Long)

    /*
    @Query("SELECT chat_table.id AS id, chat_table.name AS name, message_table.text as lastMessage, message_table.time AS lastTime " +
            "FROM chat_table LEFT JOIN message_table ON message_table.id = " +
            "(SELECT message_table.id FROM message_table WHERE message_table.chatId = chat_table.id ORDER BY message_table.time DESC LIMIT 1)" +
            "ORDER BY message_table.time DESC")

     */

    @Query("SELECT * FROM chat_table")
    fun readAllChats(): LiveData<List<Chat>>

    @Query("SELECT * FROM chat_table WHERE id = :chatId")
    fun readChat(chatId: Long): LiveData<Chat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addChatMessage(chatMessage: ChatMessage)

    /*
    @Query("SELECT message_table.id AS id, message_table.chatId AS chatId, message_table.userId AS userId, " +
            "message_table.text AS text, message_table.time AS time FROM message_table " +
            "INNER JOIN user_table ON user_table.id = message_table.userId WHERE message_table.chatId = :chatId ORDER BY message_table.time DESC")
    */

    @Query("SELECT * FROM message_table INNER JOIN chatmessage_table " +
            "WHERE message_table.id = chatmessage_table.messageId AND chatmessage_table.chatId = :chatId ORDER BY message_table.id DESC")
    fun readAllChatMessages(chatId: Long): LiveData<List<Message>>

    @Query("SELECT * FROM message_table INNER JOIN chatmessage_table " +
            "WHERE message_table.id = chatmessage_table.messageId AND chatmessage_table.chatId = :chatId ORDER BY message_table.id DESC LIMIT 1")
    fun readLastChatMessage(chatId: Long): LiveData<Message?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUsers(users: List<User>)

    @Query("SELECT * FROM user_table WHERE user_table.id = :userId")
    fun readUser(userId: Long): LiveData<User>

    @Query("SELECT * FROM user_table")
    fun readAllUsers(): LiveData<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUserOnline(userOnline: UserOnline)

    @Query("SELECT isOnline FROM useronline_table WHERE useronline_table.userId = :userId")
    fun readUserOnline(userId: Long): LiveData<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUserTyping(userTyping: UserTyping)

    @Query("SELECT isTyping FROM usertyping_table WHERE usertyping_table.userId = :userId")
    fun readUserTyping(userId: Long): LiveData<Boolean>

}