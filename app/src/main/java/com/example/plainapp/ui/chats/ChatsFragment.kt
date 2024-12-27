package com.example.plainapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.R
import com.example.plainapp.data.ChatViewModel
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

        val manager = LinearLayoutManager(activity)
        adapter = ChatPreviewAdapter()

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        chatViewModel.readAllData.observe(viewLifecycleOwner) { chats ->
            adapter.setData(chats)
        }

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        binding.newChat.setOnClickListener {

            Navigation.findNavController(binding.root).navigate(R.id.navigation_create_chat)

        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}


