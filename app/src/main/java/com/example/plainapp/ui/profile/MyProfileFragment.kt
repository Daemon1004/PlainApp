package com.example.plainapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.plainapp.ui.MainActivity
import com.example.plainapp.R
import com.example.plainapp.databinding.FragmentMyProfileBinding
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class MyProfileFragment : Fragment() {

    private var binding: FragmentMyProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentMyProfileBinding.inflate(inflater, container, false)

        val mainActivity = activity as MainActivity

        mainActivity.serviceLiveData.value?.userLiveData?.observe(viewLifecycleOwner) { user ->

            if (user != null) {

                val createdAt = LocalDateTime.ofInstant(Instant.parse(user.createdAt),
                    OffsetDateTime.now().offset).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                binding!!.myName.text = user.name
                binding!!.myNickname.text = user.nickname ?: getString(R.string.empty_nickname)
                binding!!.myBio.setText(user.bio ?: getString(R.string.empty_biography))
                binding!!.birthdate.text = user.birthdate ?: "?"
                binding!!.createdAt.text = createdAt

            } else {

                binding!!.myName.text = "..."
                binding!!.myNickname.text = "..."
                binding!!.myBio.setText("...")
                binding!!.birthdate.text = "..."
                binding!!.createdAt.text = "..."

            }

        }

        var lastBio = ""
        binding!!.bioEdit.setOnClickListener {

            if (mainActivity.serviceLiveData.value != null) {

                binding!!.bioEdit.visibility = View.GONE
                binding!!.bioAccept.visibility = View.VISIBLE
                binding!!.myBio.isEnabled = true

                lastBio = binding!!.myBio.text.toString()

            }

        }

        binding!!.bioAccept.setOnClickListener {

            binding!!.bioAccept.visibility = View.GONE
            binding!!.bioEdit.visibility = View.VISIBLE
            binding!!.myBio.isEnabled = false

            mainActivity.serviceLiveData.value!!.newBio(binding!!.myBio.text.toString()) { success, text ->
                mainActivity.runOnUiThread {
                    if (!success) {
                        binding!!.myBio.setText(lastBio)
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }

        binding!!.nameEdit.setOnClickListener {

            binding!!.editNameLayout.visibility = View.VISIBLE

            binding!!.newName.setText(binding!!.myName.text.toString())

        }

        binding!!.newNameAccept.setOnClickListener {

            binding!!.editNameLayout.visibility = View.GONE

            binding!!.nameEdit.isEnabled = false
            mainActivity.serviceLiveData.value!!.newName(binding!!.newName.text.toString()) { success, text ->
                mainActivity.runOnUiThread {
                    if (success) {
                        binding!!.myName.text = binding!!.newName.text
                    } else {
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                    binding!!.nameEdit.isEnabled = true
                }
            }

        }

        binding!!.exitButton.setOnClickListener {

            mainActivity.serviceLiveData.value?.logOut()

        }
        
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}