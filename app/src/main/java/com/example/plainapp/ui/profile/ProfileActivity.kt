package com.example.plainapp.ui.profile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.plainapp.data.User
import com.example.plainapp.databinding.ActivityProfileBinding
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val user = Json.decodeFromString<User>(intent.extras!!.getString("user")!!)

        binding.name.text = user.name
        binding.nickname.text = user.nickname
        binding.bio.text = user.bio ?: ""
        binding.birthdate.text = user.birthdate ?: "?"

        val createdAt = LocalDateTime.ofInstant(
            Instant.parse(user.createdAt),
            OffsetDateTime.now().offset
        ).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        binding.createdAt.text = createdAt

    }

}