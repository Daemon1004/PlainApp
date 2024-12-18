package com.example.planeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("chat_table")
data class Chat (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String
)