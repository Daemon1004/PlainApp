package com.example.planeapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.sql.Time

@Entity(tableName = "message_table",
    foreignKeys = [
        ForeignKey(entity = Chat::class, parentColumns = ["id"], childColumns = ["chatId"]),
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"])
    ])
data class Message (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(name = "chatId")
    val chatId: Int,
    @ColumnInfo(name = "userId")
    val userId : Int,
    val text: String,
    val time: Time
)