package com.example.plainapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.plainapp.databinding.ActivityResponseCallBinding


class ResponseCallActivity : AppCompatActivity() {

    private var _binding: ActivityResponseCallBinding? = null
    private val binding get() = _binding!!

    var result: String = "IGNORE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityResponseCallBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.acceptButton.setOnClickListener {

            result = "ACCEPT"
            finish()

        }

        binding.rejectButton.setOnClickListener {

            result = "REJECT"
            finish()

        }

    }

    override fun onDestroy() {

        val i = Intent()
        i.setAction("brActionFloatingServiceOnActivityResult")
        i.putExtra("action", "initTextToSpeech")
        i.putExtra("result", result)
        sendBroadcast(i)

        Log.d("debug", "ResponseCallActivity sendBroadcast()")

        super.onDestroy()
    }

}