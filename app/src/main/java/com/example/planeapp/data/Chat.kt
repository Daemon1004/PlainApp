package com.example.planeapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_table", indices = [Index(value = ["name"], unique = true)])
data class Chat (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @Ignore
    val lastMessage: String? = null,
    @Ignore
    val lastTime: Long? = null
) {
    constructor(id: Long, name: String) : this(id, name, null, null)
}