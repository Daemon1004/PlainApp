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

package com.example.plainapp.webrtc.peer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SimulcastVideoEncoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class StreamPeerConnectionFactory constructor(
  private val context: Context
) {

  val eglBaseContext: EglBase.Context by lazy {
    EglBase.create().eglBaseContext
  }

  /**
   * Default video decoder factory used to unpack video from the remote tracks.
   */
  private val videoDecoderFactory by lazy {
    DefaultVideoDecoderFactory(
      eglBaseContext
    )
  }

  // rtcConfig contains STUN and TURN servers list
  val rtcConfig = PeerConnection.RTCConfiguration(
    arrayListOf(
      // adding google's standard server
      PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
      //PeerConnection.IceServer.builder("stun:stun.stunprotocol.org:3478").createIceServer()
    )
  ).apply {
    // it's very important to use new unified sdp semantics PLAN_B is deprecated
    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
    continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
    iceTransportsType = PeerConnection.IceTransportsType.ALL
    candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
  }

  /**
   * Default encoder factory that supports Simulcast, used to send video tracks to the server.
   */
  private val videoEncoderFactory by lazy {
    val hardwareEncoder = HardwareVideoEncoderFactory(eglBaseContext, true, true)
    SimulcastVideoEncoderFactory(hardwareEncoder, SoftwareVideoEncoderFactory())
  }

  /**
   * Factory that builds all the connections based on the extensive configuration provided under
   * the hood.
   */
  private val factory by lazy {
    PeerConnectionFactory.initialize(
      PeerConnectionFactory.InitializationOptions.builder(context)
        .setEnableInternalTracer(true)
        .setFieldTrials("WebRTC-IgnoreLoopbackNetwork/Enabled/")
        .setFieldTrials("WebRTC-SoftwareH264/Enabled/")
        .setInjectableLogger({ message, severity, label ->
          when (severity) {
            Logging.Severity.LS_VERBOSE -> {
              Log.v(this::class.java.name, "[onLogMessage] label: $label, message: $message")
            }
            Logging.Severity.LS_INFO -> {
              Log.i(this::class.java.name, "[onLogMessage] label: $label, message: $message")
            }
            Logging.Severity.LS_WARNING -> {
              Log.w(this::class.java.name, "[onLogMessage] label: $label, message: $message")
            }
            Logging.Severity.LS_ERROR -> {
              Log.e(this::class.java.name, "[onLogMessage] label: $label, message: $message")
            }
            Logging.Severity.LS_NONE -> {
              Log.d(this::class.java.name, "[onLogMessage] label: $label, message: $message")
            }
            else -> {}
          }
        }, Logging.Severity.LS_VERBOSE)
        .createInitializationOptions()
    )

    val opts = PeerConnectionFactory.Options()
    opts.networkIgnoreMask = 0
    opts.disableEncryption = false
    opts.disableNetworkMonitor = false

    PeerConnectionFactory.builder()
      .setOptions(opts)
      .setVideoDecoderFactory(videoDecoderFactory)
      .setVideoEncoderFactory(videoEncoderFactory)
      .setAudioDeviceModule(
        JavaAudioDeviceModule
          .builder(context)
          .setUseHardwareAcousticEchoCanceler(true)
          .setUseHardwareNoiseSuppressor(true)
          .setAudioRecordErrorCallback(object :
              JavaAudioDeviceModule.AudioRecordErrorCallback {
              override fun onWebRtcAudioRecordInitError(p0: String?) {
                Log.w(this::class.java.name, "[onWebRtcAudioRecordInitError] $p0")
              }

              override fun onWebRtcAudioRecordStartError(
                p0: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                p1: String?
              ) {
                Log.w(this::class.java.name, "[onWebRtcAudioRecordInitError] $p1")
              }

              override fun onWebRtcAudioRecordError(p0: String?) {
                Log.w(this::class.java.name, "[onWebRtcAudioRecordError] $p0")
              }
            })
          .setAudioTrackErrorCallback(object :
              JavaAudioDeviceModule.AudioTrackErrorCallback {
              override fun onWebRtcAudioTrackInitError(p0: String?) {
                Log.w(this::class.java.name, "[onWebRtcAudioTrackInitError] $p0")
              }

              override fun onWebRtcAudioTrackStartError(
                p0: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                p1: String?
              ) {
                Log.w(this::class.java.name, "[onWebRtcAudioTrackStartError] $p0")
              }

              override fun onWebRtcAudioTrackError(p0: String?) {
                Log.w(this::class.java.name, "[onWebRtcAudioTrackError] $p0")
              }
            })
          .setAudioRecordStateCallback(object :
              JavaAudioDeviceModule.AudioRecordStateCallback {
              override fun onWebRtcAudioRecordStart() {
                Log.d(this::class.java.name, "[onWebRtcAudioRecordStart] no args")
              }

              override fun onWebRtcAudioRecordStop() {
                Log.d(this::class.java.name, "[onWebRtcAudioRecordStop] no args")
              }
            })
          .setAudioTrackStateCallback(object :
              JavaAudioDeviceModule.AudioTrackStateCallback {
              override fun onWebRtcAudioTrackStart() {
                Log.d(this::class.java.name, "[onWebRtcAudioTrackStart] no args")
              }

              override fun onWebRtcAudioTrackStop() {
                Log.d(this::class.java.name, "[onWebRtcAudioTrackStop] no args")
              }
            })
          .createAudioDeviceModule().also {
            it.setMicrophoneMute(false)
            it.setSpeakerMute(false)
          }
      )
      .createPeerConnectionFactory()
  }

  /**
   * Builds a [StreamPeerConnection] that wraps the WebRTC [PeerConnection] and exposes several
   * helpful handlers.
   *
   * @param coroutineScope Scope used for asynchronous operations.
   * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
   * @param type The type of connection, either a subscriber of a publisher.
   * @param mediaConstraints Constraints used for audio and video tracks in the connection.
   * @param onStreamAdded Handler when a new [MediaStream] gets added.
   * @param onNegotiationNeeded Handler when there's a new negotiation.
   * @param onIceCandidateRequest Handler whenever we receive [IceCandidate]s.
   * @return [StreamPeerConnection] That's fully set up and can be observed and used to send and
   * receive tracks.
   */
  fun makePeerConnection(
    coroutineScope: CoroutineScope,
    configuration: PeerConnection.RTCConfiguration,
    type: StreamPeerType,
    mediaConstraints: MediaConstraints,
    onStreamAdded: ((MediaStream) -> Unit)? = null,
    onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)? = null,
    onIceCandidateRequest: ((IceCandidate, StreamPeerType) -> Unit)? = null,
    onVideoTrack: ((RtpTransceiver?) -> Unit)? = null
  ): StreamPeerConnection {
    val peerConnection = StreamPeerConnection(
      coroutineScope = coroutineScope,
      type = type,
      mediaConstraints = mediaConstraints,
      onStreamAdded = onStreamAdded,
      onNegotiationNeeded = onNegotiationNeeded,
      onIceCandidate = onIceCandidateRequest,
      onVideoTrack = onVideoTrack
    )
    val connection = makePeerConnectionInternal(
      configuration = configuration,
      observer = peerConnection
    )
    return peerConnection.apply { initialize(connection) }
  }

  /**
   * Builds a [PeerConnection] internally that connects to the server and is able to send and
   * receive tracks.
   *
   * @param configuration The [PeerConnection.RTCConfiguration] used to set up the connection.
   * @param observer Handler used to observe different states of the connection.
   * @return [PeerConnection] that's fully set up.
   */
  private fun makePeerConnectionInternal(
    configuration: PeerConnection.RTCConfiguration,
    observer: PeerConnection.Observer?
  ): PeerConnection {
    return requireNotNull(
      factory.createPeerConnection(
        configuration,
        observer
      )
    )
  }

  /**
   * Builds a [VideoSource] from the [factory] that can be used for regular video share (camera)
   * or screen sharing.
   *
   * @param isScreencast If we're screen sharing using this source.
   * @return [VideoSource] that can be used to build tracks.
   */
  fun makeVideoSource(isScreencast: Boolean): VideoSource =
    factory.createVideoSource(isScreencast)

  /**
   * Builds a [VideoTrack] from the [factory] that can be used for regular video share (camera)
   * or screen sharing.
   *
   * @param source The [VideoSource] used for the track.
   * @param trackId The unique ID for this track.
   * @return [VideoTrack] That represents a video feed.
   */
  fun makeVideoTrack(
    source: VideoSource,
    trackId: String
  ): VideoTrack = factory.createVideoTrack(trackId, source)

  /**
   * Builds an [AudioSource] from the [factory] that can be used for audio sharing.
   *
   * @param constraints The constraints used to change the way the audio behaves.
   * @return [AudioSource] that can be used to build tracks.
   */
  fun makeAudioSource(constraints: MediaConstraints = MediaConstraints()): AudioSource =
    factory.createAudioSource(constraints)

  /**
   * Builds an [AudioTrack] from the [factory] that can be used for regular video share (camera)
   * or screen sharing.
   *
   * @param source The [AudioSource] used for the track.
   * @param trackId The unique ID for this track.
   * @return [AudioTrack] That represents an audio feed.
   */
  fun makeAudioTrack(
    source: AudioSource,
    trackId: String
  ): AudioTrack = factory.createAudioTrack(trackId, source)
}
