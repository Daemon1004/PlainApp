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

    @Query("SELECT * FROM chat_table ORDER BY id")
    fun readAllData(): LiveData<List<Chat>>

}