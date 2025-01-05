package com.example.plainapp

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
import com.example.plainapp.data.ChatViewModel
import com.example.plainapp.data.User
import com.example.plainapp.data.observeOnce
import com.example.plainapp.databinding.ActivityCallBinding
import com.example.plainapp.rtc.PeerConnectionObserver
import com.example.plainapp.rtc.RTCAudioManager
import com.example.plainapp.rtc.RTCClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import kotlin.properties.Delegates

class CallActivity : AppCompatActivity() {
    lateinit var binding : ActivityCallBinding
    private var rtcClient : RTCClient ?= null
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
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

    private var isCaller by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.chatName.text = ""
        isCaller = intent.extras?.getString("offerArgs") == null
        chatId = intent.extras?.getLong("chatId")!!

        binding.apply {

            if (isCaller) {

                callLayout.visibility = View.VISIBLE
                responseLayout.visibility = View.GONE

            } else {

                Log.d("debug", "call: offerArgs: ${intent.extras?.getString("offerArgs")}")

                callLayout.visibility = View.GONE
                responseLayout.visibility = View.VISIBLE

            }

        }

        serviceLiveData.observeOnce(this) { init() }

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        rtcClient?.endCall()
        super.onDestroy()
    }

    private fun init(){

        val service = serviceLiveData.value!!
        val mSocket = service.mSocket
        myUser = service.userLiveData.value

        val chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        chatViewModel.readChat(chatId).observe(this) { chat ->
            val participantId = if (chat.participant1 == myUser!!.id) chat.participant2 else chat.participant1
            chatViewModel.readUser(participantId).observe(this) { participant
                binding.chatName.text = participant?.name ?: "?"
            }
        }

        rtcClient = RTCClient(application, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)
                val jsonCandidate = Json.encodeToString(p0)
                Log.d("debug", "call: emit ice candidate $jsonCandidate")
                mSocket.emit("ice candidate", jsonCandidate, chatId)
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
                Log.d("debug", "call: onAddStream: $p0")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                Log.d("debug", "call: onConnectionChange: $newState")

                if (newState == PeerConnection.PeerConnectionState.CONNECTED)
                    runOnUiThread { binding.remoteViewLoading.visibility = View.GONE }
                else
                    runOnUiThread { binding.remoteViewLoading.visibility = View.VISIBLE }

            }

            override fun onRenegotiationNeeded() {
                super.onRenegotiationNeeded()
                Log.d("debug", "call: onRenegotiationNeeded")

                if (isCaller) {

                    rtcClient?.call { sdp, type ->

                        val json = JSONObject()
                        json.put("type", type)
                        json.put("sdp", sdp)

                        Log.d("debug", "call: emit offer - json = $json, chatId = $chatId")

                        mSocket.emit("offer", json.toString(), chatId.toString())

                    }

                    mSocket.once("answer") { answerArgs ->

                        Log.d("debug", "call: get answer")

                        val session = SessionDescription(
                            SessionDescription.Type.ANSWER,
                            JSONObject(answerArgs[0].toString()).get("sdp").toString()
                        )

                        rtcClient?.onRemoteSessionReceived(session)

                    }

                }

            }
        })

        mSocket.on("ice candidate") { iceCandidateArgs ->
            Log.d("debug", "call: get ice candidate ${iceCandidateArgs[0]}")
            val candidate = Json.decodeFromString<IceCandidate>(iceCandidateArgs[0].toString())
            rtcClient?.addIceCandidate(candidate)
        }

        if (!isCaller) {
            binding.apply {

                acceptButton.setOnClickListener {

                    Log.d("debug", "call: accept")

                    callLayout.visibility = View.VISIBLE
                    responseLayout.visibility = View.GONE

                    val offerArgs = intent.extras?.getString("offerArgs")

                    val session = SessionDescription(
                        SessionDescription.Type.OFFER,
                        JSONObject(offerArgs!!).get("sdp").toString()
                    )

                    Log.d("debug", "call: session: ${session.type} ${session.description}")

                    rtcClient?.onRemoteSessionReceived(session)
                    rtcClient?.answer { sdp, type ->

                        val json = JSONObject()
                        json.put("sdp", sdp)
                        json.put("type", type)

                        Log.d("debug", "call: emit answer - json = $json, chatId = $chatId")

                        mSocket.emit("answer", json.toString(), chatId.toString())

                    }

                }

                rejectButton.setOnClickListener {

                    Log.d("debug", "call: reject")

                    finish()

                }

            }
        }

        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        rtcClient?.initializeSurfaceView(binding.localView)
        rtcClient?.initializeSurfaceView(binding.remoteView)
        rtcClient?.startLocalVideo(binding.localView)

        binding.apply {

            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }

            micButton.setOnClickListener {
                if (isMute){
                    isMute = false
                    micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
                }else{
                    isMute = true
                    micButton.setImageResource(R.drawable.ic_baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }

            videoButton.setOnClickListener {
                if (isCameraPause){
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                }else{
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }

            audioOutputButton.setOnClickListener {
                if (isSpeakerMode){
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_hearing_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                }else{
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.ic_baseline_speaker_up_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

                }

            }

            endCallButton.setOnClickListener {
                finish()
            }

        }

    }

}