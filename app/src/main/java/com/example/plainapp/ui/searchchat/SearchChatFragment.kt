package com.example.plainapp.ui.searchchat

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.plainapp.databinding.FragmentSearchChatBinding

class SearchChatFragment : Fragment() {

    private var _binding: FragmentSearchChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSearchChatBinding.inflate(inflater, container, false)

        /*binding.button.setOnClickListener {

            val chatName = binding.chatName.text.toString()

            if (chatName.isNotEmpty()) {

                val viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

                //viewModel.addChat(Chat(0, chatName))

                Navigation.findNavController(binding.root).popBackStack()

                //Toast.makeText(requireContext(), getString(R.string.sql_constraint_error), Toast.LENGTH_LONG).show()

            } else {

                Toast.makeText(requireContext(), getString(R.string.empty_chat_name), Toast.LENGTH_LONG).show()

            }

        } */

        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {  }
            override fun onFinish() {

                binding.progressBar.visibility = View.GONE

            }
        }

        binding.chatName.addTextChangedListener {

            binding.progressBar.visibility = View.VISIBLE

            timer.cancel()
            timer.start()

        }

        return binding.root
    }
}