package com.example.planeapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.planeapp.data.ChatViewModel
import com.example.planeapp.databinding.FragmentChatsBinding

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
        val root: View = binding.root

        val manager = LinearLayoutManager(activity)
        adapter = ChatPreviewAdapter()

        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        chatViewModel.readAllData.observe(viewLifecycleOwner) { chats ->
            adapter.data = chats
        }

        /*
        adapter.data = listOf(
            ChatPreviewData(1, "Кирилл Михайлов", "дароу, как дела?", "14:25"),
            ChatPreviewData(2, "Максим Варабаев", "да все нормальн", "10:56") ,
            ChatPreviewData(3, "John Vafler", "im cooking right now", "09:12")
        )
        */

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



}


