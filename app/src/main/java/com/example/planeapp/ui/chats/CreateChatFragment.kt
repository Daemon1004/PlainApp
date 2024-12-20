package com.example.planeapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.planeapp.R
import com.example.planeapp.data.Chat
import com.example.planeapp.data.ChatViewModel
import com.example.planeapp.databinding.FragmentCreateChatBinding

class CreateChatFragment : Fragment() {

    private var _binding: FragmentCreateChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCreateChatBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {

            val chatName = binding.chatName.text.toString()

            if (chatName.isNotEmpty()) {

                val viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

                viewModel.addChat(Chat(0, chatName))

                Navigation.findNavController(binding.root).popBackStack()

                //Toast.makeText(requireContext(), getString(R.string.sql_constraint_error), Toast.LENGTH_LONG).show()

            } else {

                Toast.makeText(requireContext(), getString(R.string.empty_chat_name), Toast.LENGTH_LONG).show()

            }

        }

        return binding.root
    }
}