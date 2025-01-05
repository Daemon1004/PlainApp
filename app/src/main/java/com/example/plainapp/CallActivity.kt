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
import com.example.plainapp.data.observeOnce
import com.example.plainapp.databinding.ActivityCallBinding
import com.example.plainapp.rtc.RTCClient
import com.example.plainapp.rtc.IceCandidateModel
import com.example.plainapp.rtc.PeerConnectionObserver
import com.example.plainapp.rtc.RTCAudioManager
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity() {


    lateinit var binding : ActivityCallBinding
    private var userName:String?=null
    private var rtcClient : RTCClient?=null
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true



    var serviceLiveData: MutableLiveData<SocketService?> = MutableLiveData<SocketService?>()

    private val sConn = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder)
        {
            serviceLiveData.value = (binder as SocketService.MyBinder).service

            /*
            if (serviceLiveData.value!!.userLiveData.value == null) {
                startLoginActivity()
            }

            serviceLiveData.value!!.userLiveData.observe(this@MainActivity) { user ->
                if (user == null) {
                    startLoginActivity()
                }
            }

             */

        }
        override fun onServiceDisconnected(className: ComponentName)
        { serviceLiveData.value = null }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serviceLiveData.observeOnce(this) { init() }

        bindService(Intent(this, SocketService::class.java), sConn, Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        rtcClient?.endCall()
        super.onDestroy()
    }

    private fun init(){
        val service = serviceLiveData.value!!
        val myUser = service.userLiveData.value!!
        val mSocket = service.mSocket
        val chatId = intent.extras?.getLong("chatId")

        userName = myUser.name

        rtcClient = RTCClient(application, object : PeerConnectionObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                rtcClient?.addIceCandidate(p0)
                val candidateMap = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )
                val jsonCandidate = (candidateMap as Map<*, *>?)?.let { JSONObject(it) }
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

                /*
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {



                }

                 */

            }
        })

        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        setWhoToCallLayoutGone()
        setCallLayoutVisible()
        rtcClient?.initializeSurfaceView(binding.localView)
        rtcClient?.initializeSurfaceView(binding.remoteView)
        rtcClient?.startLocalVideo(binding.localView)

        binding.apply {
            /*
            callBtn.setOnClickListener {
                socketRepository?.sendMessageToSocket("start_call", MessageModel(userName,targetUserNameEt.text.toString(),null))
                target = targetUserNameEt.text.toString()
            }

             */

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

        mSocket.on("ice candidate") { iceCandidateArgs ->
            Log.d("debug", "call: get ice candidate ${iceCandidateArgs[0]}")
            val receivingCandidate = Json.decodeFromString<IceCandidateModel>(iceCandidateArgs[0].toString())
            rtcClient?.addIceCandidate(IceCandidate(receivingCandidate.sdpMid,
                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()), receivingCandidate.sdpCandidate))
        }


        val offerArgs = intent.extras?.getString("offerArgs")

        if (offerArgs != null) {

            Log.d("debug", "offerArgs: $offerArgs")

            val session = SessionDescription(
                SessionDescription.Type.OFFER,
                offerArgs[0].toString()
            )

            Log.d("debug", "session: ${session.description}")

            rtcClient?.onRemoteSessionReceived(session)
            rtcClient?.answer { sdp, type ->

                val json = JSONObject()
                json.put("sdp", sdp)
                json.put("type", type)

                Log.d("debug", "call: emit answer - json = $json, chatId = $chatId")

                mSocket.emit("answer", json.toString(), chatId.toString())

            }

            binding.remoteViewLoading.visibility = View.GONE

        } else {

            rtcClient?.call { sdp, type ->

                val json = JSONObject()
                json.put("sdp", sdp)
                json.put("type", type)

                Log.d("debug", "call: emit offer - json = $json, chatId = $chatId")

                mSocket.emit("offer", json.toString(), chatId.toString())

            }

            mSocket.on("answer") { answerArgs ->

                Log.d("debug", "call: get answer")

                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    answerArgs[0].toString()
                )
                rtcClient?.onRemoteSessionReceived(session)

                runOnUiThread { binding.remoteViewLoading.visibility = View.GONE }

            }

        }

    }

    private fun setIncomingCallLayoutGone(){
        binding.incomingCallLayout.visibility = View.GONE
    }
    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility = View.GONE
    }

    private fun setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
}