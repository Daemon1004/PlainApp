package com.example.plainapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.databinding.ActivityChatBinding
import com.example.plainapp.ui.chats.MessageAdapter

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!

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

        val chatId = intent.extras?.getLong("chatId") ?: return

        val manager = LinearLayoutManager(this)
        adapter = MessageAdapter()

        manager.stackFromEnd = true
        manager.reverseLayout = true
        manager.isSmoothScrollbarEnabled = true

        var first = true

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        chatViewModel.readChat(chatId).observe(this) { chat ->

            binding.chatName.text = chat.name

            chatViewModel.readAllMessages(chat).observe(this) { messages ->
                adapter.setData(messages)
                if (first) { manager.scrollToPosition(manager.childCount) }
                first = false
            }

            binding.enter.setOnClickListener {

                val text = binding.mytext.text.toString()

                if (text.isNotEmpty()) {

                    chatViewModel.addMessage(chat, text)

                    binding.mytext.setText("")

                }

            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}