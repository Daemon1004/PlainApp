package com.example.plainapp

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.data.observeOnce
import com.example.plainapp.databinding.ActivityChatBinding
import com.example.plainapp.ui.chats.MessageAdapter

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

    var service: SocketService? = null
    var myUser: User? = null

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        {

            service = (binder as SocketService.MyBinder).service
            myUser = service!!.userLiveData.value
            service!!.userLiveData.observe(this@ChatActivity) { user -> myUser = user }

        }
        override fun onServiceDisconnected(className: ComponentName)
        { service = null }
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

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        val chatId = intent.extras?.getLong("chatId") ?: return

        val manager = LinearLayoutManager(this)
        adapter = MessageAdapter(this)

        manager.stackFromEnd = true
        manager.reverseLayout = true
        manager.isSmoothScrollbarEnabled = true

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        chatViewModel.readChat(chatId).observe(this) { chat ->

            val participant = if (chat.participant1 == myUser!!.id) chat.participant2 else chat.participant1
            chatViewModel.readUser(participant).observeOnce(this) { user ->
                binding.chatName.text = user.name
            }

            chatViewModel.readAllMessages(chat).observe(this) { messages ->
                adapter.setData(messages)
                manager.scrollToPosition(0)
            }

            binding.enter.setOnClickListener {

                val text = binding.mytext.text.toString()

                if (text.isNotEmpty()) {

                    //chatViewModel.addMessage(chat, text)

                    service?.sendMessage(chat.id, text)

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
            startActivity(intent)

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}