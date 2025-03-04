/*
 * Copyright 2023 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.plainapp.webrtc.sessions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.getSystemService
import com.example.plainapp.ui.calls.VideoTextureViewRenderer
import com.example.plainapp.webrtc.peer.StreamPeerConnectionFactory
import com.example.plainapp.webrtc.SignalingClient
import com.example.plainapp.webrtc.SignalingCommand
import com.example.plainapp.webrtc.audio.AudioHandler
import com.example.plainapp.webrtc.audio.AudioSwitchHandler
import com.example.plainapp.webrtc.peer.StreamPeerConnection
import com.example.plainapp.webrtc.peer.StreamPeerType
import com.example.plainapp.webrtc.utils.stringify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerationAndroid
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.RendererCommon
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID

private const val ICE_SEPARATOR = '$'

val LocalWebRtcSessionManager: ProvidableCompositionLocal<WebRtcSessionManager> =
  staticCompositionLocalOf { error("WebRtcSessionManager was not initialized!") }

class WebRtcSessionManagerImpl(
  private val context: Context,
  override val signalingClient: SignalingClient,
  override val peerConnectionFactory: StreamPeerConnectionFactory
) : WebRtcSessionManager {

  private val sessionManagerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  // used to send local video track to the fragment
  private val _localVideoTrackFlow = MutableSharedFlow<VideoTrack>()
  override val localVideoTrackFlow: SharedFlow<VideoTrack> = _localVideoTrackFlow

  // used to send remote video track to the sender
  private val _remoteVideoTrackFlow = MutableSharedFlow<VideoTrack>()
  override val remoteVideoTrackFlow: SharedFlow<VideoTrack> = _remoteVideoTrackFlow

  // declaring video constraints and setting OfferToReceiveVideo to true
  // this step is mandatory to create valid offer and answer
  private val mediaConstraints = MediaConstraints().apply {
    mandatory.addAll(
      listOf(
        MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
        MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
      )
    )
  }

  // getting front camera
  private val videoCapturer: VideoCapturer by lazy { buildCameraCapturer() }
  private val cameraManager by lazy { context.getSystemService<CameraManager>() }
  private val cameraEnumerator: Camera2Enumerator by lazy {
    Camera2Enumerator(context)
  }

  private val resolution: CameraEnumerationAndroid.CaptureFormat
    get() {
      val frontCamera = cameraEnumerator.deviceNames.first { cameraName ->
        cameraEnumerator.isFrontFacing(cameraName)
      }
      val supportedFormats = cameraEnumerator.getSupportedFormats(frontCamera) ?: emptyList()
      return supportedFormats.firstOrNull {
        (it.width == 720 || it.width == 480 || it.width == 360)
      } ?: error("There is no matched resolution!")
    }

  // we need it to initialize video capturer
  private val surfaceTextureHelper = SurfaceTextureHelper.create(
    "SurfaceTextureHelperThread",
    peerConnectionFactory.eglBaseContext
  )

  private val videoSource by lazy {
    peerConnectionFactory.makeVideoSource(videoCapturer.isScreencast).apply {
      videoCapturer.initialize(surfaceTextureHelper, context, this.capturerObserver)
      videoCapturer.startCapture(resolution.width, resolution.height, 30)
    }
  }

  private val localVideoTrack: VideoTrack by lazy {
    peerConnectionFactory.makeVideoTrack(
      source = videoSource,
      trackId = "Video${UUID.randomUUID()}"
    )
  }

  override fun initSurfaceViewRenderer(surface: SurfaceViewRenderer) {
    /*
    surface.run {
      init(peerConnectionFactory.eglBaseContext, object : RendererCommon.RendererEvents {
        override fun onFirstFrameRendered() = Unit
        override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) = Unit
      })
    }

     */
    surface.run {
      setEnableHardwareScaler(true)
      setMirror(true)
      init(peerConnectionFactory.eglBaseContext, null)
    }
  }

  override fun localVideoStart(surface: SurfaceViewRenderer) {
    initSurfaceViewRenderer(surface)
    Log.d(this::class.java.name, "[localVideoStart] ${localVideoTrack.id()} ${localVideoTrack.state()} ${localVideoTrack.kind()} ${localVideoTrack.enabled()}" )
    localVideoTrack.addSink(surface)
  }

  /** Audio properties */

  private val audioHandler: AudioHandler by lazy {
    AudioSwitchHandler(context)
  }

  private val audioManager by lazy {
    context.getSystemService<AudioManager>()
  }

  private val audioConstraints: MediaConstraints by lazy {
    buildAudioConstraints()
  }

  private val audioSource by lazy {
    peerConnectionFactory.makeAudioSource(audioConstraints)
  }

  private val localAudioTrack: AudioTrack by lazy {
    peerConnectionFactory.makeAudioTrack(
      source = audioSource,
      trackId = "Audio${UUID.randomUUID()}"
    )
  }

  private var offer: String? = null

  private val peerConnection: StreamPeerConnection by lazy {
    peerConnectionFactory.makePeerConnection(
      coroutineScope = sessionManagerScope,
      configuration = peerConnectionFactory.rtcConfig,
      type = StreamPeerType.SUBSCRIBER,
      mediaConstraints = mediaConstraints,
      onIceCandidateRequest = { iceCandidate, _ ->
        signalingClient.sendCommand(
          SignalingCommand.ICE,
          "${iceCandidate.sdpMid}$ICE_SEPARATOR${iceCandidate.sdpMLineIndex}$ICE_SEPARATOR${iceCandidate.sdp}"
        )
      },
      onVideoTrack = { rtpTransceiver ->
        val track = rtpTransceiver?.receiver?.track() ?: return@makePeerConnection
        if (track.kind() == MediaStreamTrack.VIDEO_TRACK_KIND) {
          val videoTrack = track as VideoTrack
          sessionManagerScope.launch {
            _remoteVideoTrackFlow.emit(videoTrack)
            Log.d(this::class.java.name, "[onVideoTrack] ${videoTrack.id()} ${videoTrack.state()} ${videoTrack.kind()} ${videoTrack.enabled()}" )
            onRemoteVideoTrackCallback?.invoke(videoTrack)
          }
        }
      }
    )
  }

  init {
    sessionManagerScope.launch {
      signalingClient.signalingCommandFlow
        .collect { commandToValue ->
          when (commandToValue.first) {
            SignalingCommand.OFFER -> handleOffer(commandToValue.second)
            SignalingCommand.ANSWER -> handleAnswer(commandToValue.second)
            SignalingCommand.ICE -> handleIce(commandToValue.second)
          }
        }
    }
  }

  override fun onSessionScreenReady(callback: (() -> Unit)?) {
    setupAudio()
    peerConnection.connection.addTrack(localVideoTrack)
    peerConnection.connection.addTrack(localAudioTrack)
    sessionManagerScope.launch {
      // sending local video track to show local video from start
      _localVideoTrackFlow.emit(localVideoTrack)

      if (offer != null) sendAnswer(callback) else sendOffer(callback)
    }
  }

  private var onRemoteVideoTrackCallback: ((VideoTrack) -> Unit)? = null
  override fun onRemoteVideoTrack(callback: (VideoTrack) -> Unit) {
    onRemoteVideoTrackCallback = callback
  }

  override fun flipCamera() {
    (videoCapturer as? Camera2Capturer)?.switchCamera(null)
  }

  override fun enableMicrophone(enabled: Boolean) {
    audioManager?.isMicrophoneMute = !enabled
  }

  override fun enableCamera(enabled: Boolean) {
    if (enabled) {
      videoCapturer.startCapture(resolution.width, resolution.height, 30)
    } else {
      videoCapturer.stopCapture()
    }
  }

  override fun disconnect() {
    // dispose audio & video tracks.
    remoteVideoTrackFlow.replayCache.forEach { videoTrack ->
      videoTrack.dispose()
    }
    localVideoTrackFlow.replayCache.forEach { videoTrack ->
      videoTrack.dispose()
    }
    localAudioTrack.dispose()
    localVideoTrack.dispose()

    // dispose audio handler and video capturer.
    audioHandler.stop()
    videoCapturer.stopCapture()
    videoCapturer.dispose()

    // dispose signaling clients and socket.
    signalingClient.dispose()
  }

  private suspend fun sendOffer(callback: (() -> Unit)? = null) {
    val offer = peerConnection.createOffer().getOrThrow()
    val result = peerConnection.setLocalDescription(offer)
    result.onSuccess {
      signalingClient.sendCommand(SignalingCommand.OFFER, offer.description)
      callback?.invoke()
    }
    Log.d(this::class.java.name, "[SDP] send offer: ${offer.stringify()}" )
  }

  private suspend fun sendAnswer(callback: (() -> Unit)? = null) {
    peerConnection.setRemoteDescription(
      SessionDescription(SessionDescription.Type.OFFER, offer)
    )
    val answer = peerConnection.createAnswer().getOrThrow()
    val result = peerConnection.setLocalDescription(answer)
    result.onSuccess {
      signalingClient.sendCommand(SignalingCommand.ANSWER, answer.description)
      callback?.invoke()
    }
    Log.d(this::class.java.name, "[SDP] send answer: ${answer.stringify()}" )
  }

  fun handleOffer(sdp: String) {
    Log.d(this::class.java.name, "[SDP] handle offer: $sdp" )
    offer = sdp
  }

  private suspend fun handleAnswer(sdp: String) {
    Log.d(this::class.java.name, "[SDP] handle answer: $sdp" )
    peerConnection.setRemoteDescription(
      SessionDescription(SessionDescription.Type.ANSWER, sdp)
    )
  }

  private suspend fun handleIce(iceMessage: String) {
    val iceArray = iceMessage.split(ICE_SEPARATOR)
    peerConnection.addIceCandidate(
      IceCandidate(
        iceArray[0],
        iceArray[1].toInt(),
        iceArray[2]
      )
    )
  }

  private fun buildCameraCapturer(): VideoCapturer {
    val manager = cameraManager ?: throw RuntimeException("CameraManager was not initialized!")

    val ids = manager.cameraIdList
    var foundCamera = false
    var cameraId = ""

    for (id in ids) {
      val characteristics = manager.getCameraCharacteristics(id)
      val cameraLensFacing = characteristics[CameraCharacteristics.LENS_FACING]

      if (cameraLensFacing == CameraMetadata.LENS_FACING_FRONT) {
        foundCamera = true
        cameraId = id
      }
    }

    if (!foundCamera && ids.isNotEmpty()) {
      cameraId = ids.first()
    }

    val camera2Capturer = Camera2Capturer(context, cameraId, null)
    return camera2Capturer
  }

  private fun buildAudioConstraints(): MediaConstraints {
    val mediaConstraints = MediaConstraints()
    val items = listOf(
      MediaConstraints.KeyValuePair(
        "googEchoCancellation",
        true.toString()
      ),
      MediaConstraints.KeyValuePair(
        "googAutoGainControl",
        true.toString()
      ),
      MediaConstraints.KeyValuePair(
        "googHighpassFilter",
        true.toString()
      ),
      MediaConstraints.KeyValuePair(
        "googNoiseSuppression",
        true.toString()
      ),
      MediaConstraints.KeyValuePair(
        "googTypingNoiseDetection",
        true.toString()
      )
    )

    return mediaConstraints.apply {
      with(optional) {
        add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        addAll(items)
      }
    }
  }

  private fun setupAudio() {
    Log.d(this::class.java.name, "[setupAudio] #sfu; no args" )
    audioHandler.start()
    audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val devices = audioManager?.availableCommunicationDevices ?: return
      val deviceType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER

      val device = devices.firstOrNull { it.type == deviceType } ?: return

      val isCommunicationDeviceSet = audioManager?.setCommunicationDevice(device)
      Log.d(this::class.java.name, "[setupAudio] #sfu; isCommunicationDeviceSet: $isCommunicationDeviceSet" )
    }
  }
}
