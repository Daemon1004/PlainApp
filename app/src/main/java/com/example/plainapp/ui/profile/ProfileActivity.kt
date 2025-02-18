package com.example.plainapp.ui.profile

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.plainapp.SocketService
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.databinding.ActivityProfileBinding
import com.example.plainapp.observeOnce
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!

    var service: SocketService? = null

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        { service = (binder as SocketService.MyBinder).service }
        override fun onServiceDisconnected(className: ComponentName)
        { service = null }
    }

    private fun getStr(json: JSONObject, key: String): String? {
        return if (json[key].toString() == "null") null else json[key].toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        val chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        val data = intent.extras!!.getString("data")

        if (data != null) {

            val json = JSONObject(data)
            val id = json["id"].toString().toLong()

            setUser(User(
                id = id,
                nickname = getStr(json, "nickname")!!,
                name = getStr(json, "name")!!,
                bio = getStr(json, "bio") ?: "",
                birthdate = getStr(json, "birthdate") ?: "?",
                phoneNumber = getStr(json, "phoneNumber")!!,
                createdAt = getStr(json, "createdAt") ?: "?"
            ))

            chatViewModel.readUser(id).observeOnce(this) { user ->

                if (user == null) {

                    binding.add.visibility = View.VISIBLE

                    binding.add.setOnClickListener {

                        if (service != null) {

                            service!!.createChat(id)

                            binding.add.visibility = View.GONE

                            finish()

                        }

                    }

                }

            }

        } else {

            val userId = intent.extras!!.getLong("user")
            chatViewModel.readUser(userId).observe(this) { user ->
                if (user != null) {
                    setUser(user)
                }
            }

        }

    }

    private fun setUser(user: User) {

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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}