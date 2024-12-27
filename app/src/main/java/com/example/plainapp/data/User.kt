package com.example.plainapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity("user_table")
data class User (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String?,
    val nickname: String?,
    val bio: String?,
    val birthdate: String?,
    val phoneNumber: String,
    val createdAt: String,
    val updatedAt: String
)