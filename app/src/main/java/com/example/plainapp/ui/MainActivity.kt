package com.example.plainapp.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.plainapp.R
import com.example.plainapp.SocketService
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.databinding.ActivityMainBinding
import com.example.plainapp.ui.chats.ChatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var serviceLiveData: MutableLiveData<SocketService?> = MutableLiveData<SocketService?>()

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        {
            val service = (binder as SocketService.MyBinder).service
            serviceLiveData.value = service

            val connectionStatus = service.connectedStatus

            if (service.userLiveData.value == null) { startLoginActivity() }
            service.userLiveData.observe(this@MainActivity)
            { user -> if (user == null) { startLoginActivity() } }

            connectionStatus.value?.let { showConnectionProblem(it) }
            connectionStatus.observe(this@MainActivity)
            { connected -> showConnectionProblem(connected) }

        }
        override fun onServiceDisconnected(className: ComponentName)
        { serviceLiveData.value = null }
    }

    private fun showConnectionProblem(connected: Boolean) {

        binding.connectionProblemsLayout.visibility = if (connected) View.GONE else View.VISIBLE

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showConnectionProblem(false)

        startService(Intent(this, SocketService::class.java))
        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ -> binding.title.text = destination.label }
        binding.navView.setupWithNavController(navController)


        val perms = emptyList<String>().toMutableList()

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            perms += Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED)
            perms += Manifest.permission.RECORD_AUDIO

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            perms += Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS) == PackageManager.PERMISSION_DENIED)
            perms += Manifest.permission.MODIFY_AUDIO_SETTINGS

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_DENIED)
            perms += Manifest.permission.ACCESS_NETWORK_STATE

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED)
            perms += Manifest.permission.INTERNET

        if (perms.isNotEmpty())
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)


        /*
        val packageName = packageName
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }

         */

    }

    private fun startLoginActivity() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )
        startActivity(intent)

    }

    fun fromSearchToChats(user: User) {

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(R.id.action_navigation_create_chat_to_navigation_chats)

        val timer = object : CountDownTimer(2000, 2000) {
            override fun onTick(millisUntilFinished: Long) {  }
            override fun onFinish() {

                val chatViewModel = ViewModelProvider(this@MainActivity)[ChatViewModel::class.java]

                chatViewModel.readChatIdUser(user.id).observe(this@MainActivity) { chatId ->

                    if (chatId != null) {

                        val intent = Intent(this@MainActivity, ChatActivity::class.java)
                        intent.putExtra("chatId", chatId)
                        startActivity(intent)

                    }

                }

            }
        }
        timer.start()

    }

}