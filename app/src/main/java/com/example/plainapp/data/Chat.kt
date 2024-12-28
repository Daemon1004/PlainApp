package com.example.plainapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chat_table",
    foreignKeys = [
        ForeignKey(entity = User::class,
            parentColumns = ["id"],
            childColumns = ["participant1"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION),
        ForeignKey(entity = User::class,
            parentColumns = ["id"],
            childColumns = ["participant2"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION)],
    indices = [Index(value = ["participant1", "participant2"], unique = true)])
data class Chat (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "participant1")
    val participant1: Long,
    @ColumnInfo(name = "participant2")
    val participant2: Long,
    val updatedAt: String,
    val createdAt: String
)