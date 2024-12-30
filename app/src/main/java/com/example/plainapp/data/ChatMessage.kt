package com.example.plainapp.data

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity

@Entity(tableName = "chatmessage_table", primaryKeys= [ "chatId", "messageId" ],
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"]
        ),
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"]
        )
    ]
)
data class ChatMessage (
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "messageId")
    val messageId: Long,
)
