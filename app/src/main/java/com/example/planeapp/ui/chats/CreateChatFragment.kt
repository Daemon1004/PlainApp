package com.example.planeapp.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
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



            Navigation.findNavController(binding.root).popBackStack()

        }

        return binding.root
    }
}