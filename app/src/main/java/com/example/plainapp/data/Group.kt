package com.example.plainapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_table", indices = [Index(value = ["name"], unique = true)])
data class Group (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    val lastMessage: String? = null,
    val lastTime: Long? = null
) {
    constructor(id: Long, name: String) : this(id, name, null, null)
}