package com.example.plainapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.plainapp.MainActivity
import com.example.plainapp.R
import com.example.plainapp.databinding.FragmentProfileBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentProfileBinding.inflate(inflater, container, false)

        val mainActivity = activity as MainActivity

        val user = mainActivity.service?.user

        if (user != null) {

            val createdAt = LocalDateTime.ofInstant(Instant.parse(user.createdAt), OffsetDateTime.now().offset)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

            binding!!.myName.text = user.name
            binding!!.myNickname.text = user.nickname ?: getString(R.string.empty_nickname)
            binding!!.myBio.text = user.bio ?: getString(R.string.empty_biography)
            binding!!.birthdate.text = user.birthdate ?: "?"
            binding!!.createdAt.text = createdAt

        }
        
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}