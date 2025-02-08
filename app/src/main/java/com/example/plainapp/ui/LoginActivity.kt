package com.example.plainapp.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.plainapp.R
import com.example.plainapp.SocketService
import com.example.plainapp.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    private var _binding: ActivityLoginBinding? = null
    private val binding get() = _binding!!

    var service: SocketService? = null

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        { service = (binder as SocketService.MyBinder).service }
        override fun onServiceDisconnected(className: ComponentName)
        { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        binding.button.setOnClickListener {

            val phoneNumberView: EditText = findViewById(R.id.textPhone)
            val phoneNumber = phoneNumberView.text.toString()

            if (phoneNumber != "") {

                logIn(phoneNumber)

            } else {

                Toast.makeText(applicationContext, getString(R.string.empty_phonenumber), Toast.LENGTH_LONG).show()

            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private var notfound = false
    private fun logIn(phoneNumber: String) {

        if (service == null) return

        if (!notfound) {

            service!!.userLiveData.observe(this) { user ->
                if (user != null) this.finish()
            }
            service!!.logIn(phoneNumber) {
                runOnUiThread {

                    notfound = true

                    binding.textPhone.visibility = View.GONE
                    binding.userName.visibility = View.VISIBLE
                    binding.userNickname.visibility = View.VISIBLE

                }
            }

        } else {

            service!!.reg(phoneNumber, binding.userName.text.toString(), binding.userNickname.text.toString()) { error ->
                runOnUiThread { Toast.makeText(this, error, Toast.LENGTH_SHORT).show() }
            }

        }

    }
}