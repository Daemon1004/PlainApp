package com.example.plainapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.MainActivity
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

    private fun setService(service: SocketService?) {

        if (service != null) {

            val myUser = service.userLiveData
            setUser(myUser.value)
            myUser.observe(viewLifecycleOwner) { user -> setUser(user) }

        } else {

            setUser(user = null)

        }

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


