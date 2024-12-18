package com.example.planeapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("user_table")
data class User (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val userName: String
)