package com.example.plainapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "usertyping_table",
    foreignKeys = [
        ForeignKey(entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"])
    ]
)
data class UserTyping (
    @PrimaryKey(autoGenerate = false)
    val userId: Long = 0,
    val isTyping: Boolean
)