package com.example.plainapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "chat_table",
    foreignKeys = [
        ForeignKey(entity = User::class,
            parentColumns = ["id"],
            childColumns = ["participant1"]),
        ForeignKey(entity = User::class,
            parentColumns = ["id"],
            childColumns = ["participant2"])])
data class Chat (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val participant1: Long,
    val participant2: Long,
    val updatedAt: String,
    val createdAt: String
)