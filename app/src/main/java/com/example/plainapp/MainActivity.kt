package com.example.plainapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.plainapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var service: SocketService? = null

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        { service = (binder as SocketService.MyBinder).service }
        override fun onServiceDisconnected(className: ComponentName)
        { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startService(Intent(this, SocketService::class.java))
        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ -> binding.title.text = destination.label }
        navView.setupWithNavController(navController)
    }

}