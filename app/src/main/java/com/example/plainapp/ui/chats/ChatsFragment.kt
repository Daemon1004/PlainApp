package com.example.plainapp.ui.chats

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.ui.MainActivity
import com.example.plainapp.R
import com.example.plainapp.SocketService
import com.example.plainapp.data.Chat
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.databinding.FragmentChatsBinding

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatPreviewAdapter

    private lateinit var chatViewModel: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChatsBinding.inflate(inflater, container, false)

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        val manager = LinearLayoutManager(activity)
        adapter = ChatPreviewAdapter(chatViewModel, viewLifecycleOwner, (activity as MainActivity))

        val mainActivity = activity as MainActivity
        val serviceLiveData = mainActivity.serviceLiveData

        if (serviceLiveData.value != null) { setService(serviceLiveData.value) }
        serviceLiveData.observe(viewLifecycleOwner) { service -> setService(service) }

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        binding.newChat.setOnClickListener {

            Navigation.findNavController(binding.root).navigate(R.id.navigation_create_chat)

        }

        return binding.root
    }

    private var timer: CountDownTimer ?= null

    override fun onStart() {
        super.onStart()

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onTick(millisUntilFinished: Long) { adapter.notifyDataSetChanged() }
            override fun onFinish() { start() }
        }
        (timer as CountDownTimer).start()

    }

    override fun onStop() {

        if (timer != null) (timer as CountDownTimer).cancel()

        super.onStop()
    }

    private fun setService(service: SocketService?) {

        if (service != null) {

            val myUser = service.userLiveData
            setUser(myUser.value)
            myUser.observe(viewLifecycleOwner) { user -> setUser(user) }
            showLoading(service.updatingChatsStatus.value!!)
            service.updatingChatsStatus.observe(viewLifecycleOwner) { updating -> showLoading(updating) }

        } else {

            setUser(user = null)
            showLoading(true)

        }

    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    private fun setUser(user: User?) {

        if (user != null) {
            chatViewModel.readAllChats.observe(viewLifecycleOwner) { chats ->
                adapter.setData(chats)
            }
        } else {
            val list: List<Chat> = emptyList()
            adapter.setData(list)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}


