package com.example.planeapp.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time

@Entity("message_table")
data class Message (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @Embedded val chat: Chat,
    @Embedded val user : User,
    val text: String,
    val time: Time
)