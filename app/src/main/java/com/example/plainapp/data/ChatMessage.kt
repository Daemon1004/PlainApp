package com.example.plainapp.data

import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Entity

@Entity(tableName = "chatmessage_table", primaryKeys= [ "chatId", "messageId" ],
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ]
)
data class ChatMessage (
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "messageId")
    val messageId: Long,
)
