package com.example.plainapp.ui.searchchat

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.plainapp.databinding.FragmentSearchChatBinding
import com.example.plainapp.ui.MainActivity
import com.example.plainapp.ui.profile.UserPreviewAdapter

class SearchChatFragment : Fragment() {

    private var _binding: FragmentSearchChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSearchChatBinding.inflate(inflater, container, false)

        val mainActivity = activity as MainActivity

        val manager = LinearLayoutManager(mainActivity)
        val adapter = UserPreviewAdapter(mainActivity)

        binding.recyclerView.layoutManager = manager
        binding.recyclerView.adapter = adapter

        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {  }
            override fun onFinish() {

                val text = binding.chatName.text.toString()

                mainActivity.serviceLiveData.value?.searchUsers(text) { users ->
                    mainActivity.runOnUiThread {

                        adapter.setData(users)

                        binding.progressBar.visibility = View.GONE

                    }
                }

            }
        }

        binding.chatName.addTextChangedListener {

            timer.cancel()

            val text = binding.chatName.text.toString()

            if (text.trim().length < 4) {

                binding.progressBar.visibility = View.GONE

            } else {

                binding.progressBar.visibility = View.VISIBLE

                timer.start()

            }

            adapter.setData(emptyList())

        }

        return binding.root
    }
}