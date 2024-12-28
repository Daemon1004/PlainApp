package com.example.plainapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "message_table",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["createdBy"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("createdBy")
    ]
)
data class Message (
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val body: String,
    val notifyDate: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: Long,
    val timesResent: Int? = null
)

/*
@Entity(primaryKeys= [ "groupId", "messageId" ] )
data class GroupMessages (
    @ColumnInfo(name = "groupId")
    val groupId: Long,
    @ColumnInfo(name = "messageId")
    val messageId: Long,
) {
//    constructor(chatId: Long, userId: Long) : this(chatId, userId);
}

 */

