package com.example.plainapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity("user_table")
data class User (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nickname: String,
    val name: String,
    val bio: String? = null,
    val birthdate: String? = null,
    val phoneNumber: String,
    val createdAt: String
)