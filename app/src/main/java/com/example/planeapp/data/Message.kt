package com.example.planeapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "message_table",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("chatId"),
        Index("userId")
    ]
)
data class Message (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "userId")
    val userId: Long,
    @Ignore
    val userName: String? = null,
    val text: String,
    val time: Long
) {
    constructor(id: Long, chatId: Long, userId: Long, text: String, time: Long) : this(id, chatId, userId, null, text, time)
}

@Entity(primaryKeys= [ "chatId", "messageId" ] )
data class ChatMessages (
    @ColumnInfo(name = "chatId")
    val chatId: Long,
    @ColumnInfo(name = "messageId")
    val userId: Long,
) {
//    constructor(chatId: Long, userId: Long) : this(chatId, userId);
}

@Entity(primaryKeys= [ "groupId", "messageId" ] )
data class GroupMessages (
    @ColumnInfo(name = "groupId")
    val chatId: Long,
    @ColumnInfo(name = "messageId")
    val userId: Long,
) {
//    constructor(chatId: Long, userId: Long) : this(chatId, userId);
}