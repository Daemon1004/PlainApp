package com.example.plainapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Chat::class,
        ChatMessage::class,
        Message::class
               ],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase: RoomDatabase() {

    abstract fun chatDao(): ChatDao

    companion object{
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase{
            if (INSTANCE != null) return INSTANCE as LocalDatabase
            synchronized(this){
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "plainapp_database"
                ).build()
                return INSTANCE as LocalDatabase
            }
        }
    }

}