package com.example.plainapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Chat::class, Message::class, ChatMessage::class, User::class], version = 1, exportSchema = false)
abstract class LocalDatabase: RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object{
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase{
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "chat_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}