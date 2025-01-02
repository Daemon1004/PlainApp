package com.example.plainapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.plainapp.databinding.ActivityMainBinding
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var serviceLiveData: MutableLiveData<SocketService?> = MutableLiveData<SocketService?>()

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        {
            serviceLiveData.value = (binder as SocketService.MyBinder).service

            if (serviceLiveData.value!!.userLiveData.value == null) {
                startLoginActivity()
            }

            serviceLiveData.value!!.userLiveData.observe(this@MainActivity) { user ->
                if (user == null) {
                    startLoginActivity()
                }
            }

        }
        override fun onServiceDisconnected(className: ComponentName)
        { serviceLiveData.value = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, SocketService::class.java))
        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ -> binding.title.text = destination.label }
        binding.navView.setupWithNavController(navController)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)

    }

    private fun startLoginActivity() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )
        startActivity(intent)

    }

}