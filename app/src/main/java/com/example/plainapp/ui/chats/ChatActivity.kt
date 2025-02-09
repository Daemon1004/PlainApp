package com.example.plainapp.ui.chats

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.R
import com.example.plainapp.SocketService
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.databinding.ActivityChatBinding
import com.example.plainapp.observeOnce
import com.example.plainapp.ui.calls.CallActivity
import com.example.plainapp.ui.profile.ProfileActivity
import kotlin.properties.Delegates
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    var serviceLiveData: MutableLiveData<SocketService?> = MutableLiveData<SocketService?>()
    var myUser: User? = null
    private var participant: Long? = null
    var chatId by Delegates.notNull<Long>()

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        {

            val service = (binder as SocketService.MyBinder).service
            serviceLiveData.value = service

            if (service.userLiveData.value != null) userInit(service.userLiveData.value!!)
            service.userLiveData.observe(this@ChatActivity) { user -> if (user != null) userInit(user) }

        }
        override fun onServiceDisconnected(className: ComponentName)
        { serviceLiveData.value = null }
    }

    private lateinit var adapter: MessageAdapter

    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.onlineCircle.visibility = View.GONE

        chatId = intent.extras?.getLong("chatId") ?: return
        serviceLiveData.observeOnce(this) { service -> service?.markAsRead(chatId) }
        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

    }

    private var inited = false
    private fun userInit(myUser: User) {

        if (inited) return
        inited = true

        this.myUser = myUser

        val manager = LinearLayoutManager(this)
        adapter = MessageAdapter(this)

        manager.stackFromEnd = true
        manager.reverseLayout = true
        manager.isSmoothScrollbarEnabled = true

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        chatViewModel.readChat(chatId).observe(this) { chat ->

            participant = if (chat.participant1 == myUser.id) chat.participant2 else chat.participant1
            chatViewModel.readUser(participant!!).observeOnce(this) { user ->
                binding.chatName.text = user.name

                binding.userLogo.setOnClickListener {

                    val intent = Intent(
                        this,
                        ProfileActivity::class.java
                    )
                    intent.putExtra("user", Json.encodeToString(user))
                    startActivity(intent)

                }
            }

            chatViewModel.readAllMessages(chat).observe(this) { messages ->
                adapter.setData(messages)
                manager.scrollToPosition(0)
            }

            binding.enter.setOnClickListener {

                val text = binding.mytext.text.toString()

                if (text.isNotEmpty()) {

                    serviceLiveData.value?.sendMessage(chat.id, text)

                    binding.mytext.setText("")
                    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(binding.enter.windowToken, 0)

                }

            }

        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(chatId.toInt())

        binding.callButton.setOnClickListener {

            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("chatId", chatId)
            startActivity(intent)

        }

    }

    private var timer: CountDownTimer ?= null

    override fun onStart() {
        super.onStart()

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onTick(millisUntilFinished: Long) {
                if (participant != null && serviceLiveData.value != null)
                    binding.onlineCircle.visibility = if (serviceLiveData.value!!.isUserOnline(participant!!)) View.VISIBLE else View.GONE
            }
            override fun onFinish() { start() }
        }
        (timer as CountDownTimer).start()

    }

    override fun onStop() {

        if (timer != null) (timer as CountDownTimer).cancel()

        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}