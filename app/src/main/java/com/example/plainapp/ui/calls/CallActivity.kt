package com.example.plainapp.ui.calls

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.plainapp.R
import com.example.plainapp.SocketService
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.databinding.ActivityCallBinding
import com.example.plainapp.observeOnce
import com.example.plainapp.webrtc.SignalingClient
import com.example.plainapp.webrtc.peer.StreamPeerConnectionFactory
import com.example.plainapp.webrtc.sessions.WebRtcSessionManagerImpl
import kotlin.properties.Delegates

class CallActivity : AppCompatActivity() {
    lateinit var binding : ActivityCallBinding
    private var isMute = false
    private var isCameraPause = false
    private var isSpeakerMode = true

    var serviceLiveData: MutableLiveData<SocketService?> = MutableLiveData<SocketService?>()

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        { serviceLiveData.value = (binder as SocketService.MyBinder).service }
        override fun onServiceDisconnected(className: ComponentName)
        { serviceLiveData.value = null }
    }

    private var myUser: User ?= null
    private var chatId by Delegates.notNull<Long>()
    private var participant: User ?= null

    private lateinit var signalingClient: SignalingClient
    private var sessionManager: WebRtcSessionManagerImpl? = null

    private var isCaller by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isCaller = intent.extras?.getString("offerArgs") == null
        chatId = intent.extras?.getLong("chatId")!!

        binding.apply {

            chatName.text = ""
            binding.remoteViewLoading.visibility = View.VISIBLE

            if (isCaller) {

                callLayout.visibility = View.VISIBLE
                responseLayout.visibility = View.GONE

            } else {

                Log.d("debug", "call: offerArgs: ${intent.extras?.getString("offerArgs")}")

                callLayout.visibility = View.GONE
                responseLayout.visibility = View.VISIBLE

            }

        }

        serviceLiveData.observeOnce(this) { initAfterServiceConnected() }

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        sessionManager?.disconnect()
        super.onDestroy()
    }

    private fun call() { sessionManager!!.onSessionScreenReady() }

    private fun answer() {

        binding.callLayout.visibility = View.VISIBLE
        binding.responseLayout.visibility = View.GONE

        val offer = intent.extras?.getString("offerArgs")!!

        sessionManager!!.handleOffer(offer)
        sessionManager!!.onSessionScreenReady()

    }

    private fun readParticipant(callback: () -> Unit) {

        val chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        chatViewModel.readChat(chatId).observe(this) { chat ->
            val participantId = if (chat.participant1 == myUser!!.id) chat.participant2 else chat.participant1
            chatViewModel.readUser(participantId).observe(this) { participant ->
                this.participant = participant
                callback()
            }
        }

    }

    private fun initAfterServiceConnected(){

        val service = serviceLiveData.value!!
        myUser = service.userLiveData.value

        readParticipant { binding.chatName.text = participant?.name ?: "?" }

        signalingClient = SignalingClient(service.mSocket, chatId)
        sessionManager = WebRtcSessionManagerImpl(this, signalingClient, StreamPeerConnectionFactory(this))

        sessionManager!!.initSurfaceViewRenderer(binding.remoteView)
        sessionManager!!.onRemoteVideoTrack { videoTrack ->
            runOnUiThread {
                binding.remoteViewLoading.visibility = View.GONE
            }
            videoTrack.addSink(binding.remoteView)
        }

        binding.localView.visibility = View.VISIBLE
        sessionManager!!.localVideoStart(binding.localView)

        if (!isCaller) {
            binding.apply {

                acceptButton.setOnClickListener {

                    Log.d("debug", "call: accept")

                    answer()

                }

                rejectButton.setOnClickListener {

                    Log.d("debug", "call: reject")

                    finish()

                }

            }
        } else { call() }

        binding.apply {

            switchCameraButton.setOnClickListener {
                sessionManager?.flipCamera()
            }

            micButton.setOnClickListener {
                if (isMute){
                    isMute = false
                    micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }else{
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                sessionManager?.enableMicrophone(isMute)
            }

            videoButton.setOnClickListener {
                if (isCameraPause){
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                }else{
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                sessionManager?.enableCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if (isSpeakerMode){
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                    //rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                }else{
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    //rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

                }

            }

            endCallButton.setOnClickListener {
                finish()
            }

        }

    }

}