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

import android.util.Log
import com.example.plainapp.webrtc.utils.addRtcIceCandidate
import com.example.plainapp.webrtc.utils.createValue
import com.example.plainapp.webrtc.utils.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription

/**
 * Wrapper around the WebRTC connection that contains tracks.
 *
 * @param coroutineScope The scope used to listen to stats events.
 * @param type The internal type of the PeerConnection. Check [StreamPeerType].
 * @param mediaConstraints Constraints used for the connections.
 * @param onStreamAdded Handler when a new [MediaStream] gets added.
 * @param onNegotiationNeeded Handler when there's a new negotiation.
 * @param onIceCandidate Handler whenever we receive [IceCandidate]s.
 */
class StreamPeerConnection(
  private val coroutineScope: CoroutineScope,
  private val type: StreamPeerType,
  private val mediaConstraints: MediaConstraints,
  private val onStreamAdded: ((MediaStream) -> Unit)?,
  private val onNegotiationNeeded: ((StreamPeerConnection, StreamPeerType) -> Unit)?,
  private val onIceCandidate: ((IceCandidate, StreamPeerType) -> Unit)?,
  private val onVideoTrack: ((RtpTransceiver?) -> Unit)?
) : PeerConnection.Observer {

  /**
   * The wrapped connection for all the WebRTC communication.
   */
  lateinit var connection: PeerConnection
    private set

  /**
   * Used to manage the stats observation lifecycle.
   */
  private var statsJob: Job? = null

  /**
   * Used to pool together and store [IceCandidate]s before consuming them.
   */
  private val pendingIceMutex = Mutex()
  private val pendingIceCandidates = mutableListOf<IceCandidate>()

  /**
   * Contains stats events for observation.
   */
  private val statsFlow: MutableStateFlow<RTCStatsReport?> = MutableStateFlow(null)

  init {
    Log.i(this::class.java.name, "<init> #sfu; mediaConstraints: $mediaConstraints")
  }

  /**
   * Initialize a [StreamPeerConnection] using a WebRTC [PeerConnection].
   *
   * @param peerConnection The connection that holds audio and video tracks.
   */
  fun initialize(peerConnection: PeerConnection) {
    Log.d(this::class.java.name, "[initialize] #sfu; peerConnection: $peerConnection")
    this.connection = peerConnection
  }

  /**
   * Used to create an offer whenever there's a negotiation that we need to process on the
   * publisher side.
   *
   * @return [Result] wrapper of the [SessionDescription] for the publisher.
   */
  suspend fun createOffer(): Result<SessionDescription> {
    Log.d(this::class.java.name, "[createOffer] #sfu; no args")
    return createValue { connection.createOffer(it, mediaConstraints) }
  }

  /**
   * Used to create an answer whenever there's a subscriber offer.
   *
   * @return [Result] wrapper of the [SessionDescription] for the subscriber.
   */
  suspend fun createAnswer(): Result<SessionDescription> {
    Log.d(this::class.java.name, "[createAnswer] #sfu; no args")
    return createValue { connection.createAnswer(it, mediaConstraints) }
  }

  /**
   * Used to set up the SDP on underlying connections and to add [pendingIceCandidates] to the
   * connection for listening.
   *
   * @param sessionDescription That contains the remote SDP.
   * @return An empty [Result], if the operation has been successful or not.
   */
  suspend fun setRemoteDescription(sessionDescription: SessionDescription): Result<Unit> {
    Log.d(this::class.java.name, "[setRemoteDescription] #sfu; answerSdp: $sessionDescription")
    return setValue {
      connection.setRemoteDescription(
        it,
        SessionDescription(
          sessionDescription.type,
          sessionDescription.description.mungeCodecs()
        )
      )
    }.also {
      pendingIceMutex.withLock {
        pendingIceCandidates.forEach { iceCandidate ->
          Log.i(this::class.java.name, "[setRemoteDescription] #sfu; #subscriber; pendingRtcIceCandidate: $iceCandidate")
          connection.addRtcIceCandidate(iceCandidate)
        }
        pendingIceCandidates.clear()
      }
    }
  }

  /**
   * Sets the local description for a connection either for the subscriber or publisher based on
   * the flow.
   *
   * @param sessionDescription That contains the subscriber or publisher SDP.
   * @return An empty [Result], if the operation has been successful or not.
   */
  suspend fun setLocalDescription(sessionDescription: SessionDescription): Result<Unit> {
    val sdp = SessionDescription(
      sessionDescription.type,
      sessionDescription.description.mungeCodecs()
    )
    Log.d(this::class.java.name, "[setLocalDescription] #sfu; offerSdp: ${sessionDescription}")
    return setValue { connection.setLocalDescription(it, sdp) }
  }

  /**
   * Adds an [IceCandidate] to the underlying [connection] if it's already been set up, or stores
   * it for later consumption.
   *
   * @param iceCandidate To process and add to the connection.
   * @return An empty [Result], if the operation has been successful or not.
   */
  suspend fun addIceCandidate(iceCandidate: IceCandidate): Result<Unit> {
    if (connection.remoteDescription == null) {
      Log.w(this::class.java.name, "[addIceCandidate] #sfu; postponed (no remoteDescription): $iceCandidate")
      pendingIceMutex.withLock {
        pendingIceCandidates.add(iceCandidate)
      }
      return Result.failure(RuntimeException("RemoteDescription is not set"))
    }
    Log.d(this::class.java.name, "[addIceCandidate] #sfu; rtcIceCandidate: $iceCandidate")
    return connection.addRtcIceCandidate(iceCandidate).also {
      Log.v(this::class.java.name, "[addIceCandidate] #sfu; completed: $it")
    }
  }

  /**
   * Peer connection listeners.
   */

  /**
   * Triggered whenever there's a new [RtcIceCandidate] for the call. Used to update our tracks
   * and subscriptions.
   *
   * @param candidate The new candidate.
   */
  override fun onIceCandidate(candidate: IceCandidate?) {
    Log.i(this::class.java.name, "[onIceCandidate] #sfu; candidate: $candidate")
    if (candidate == null) return

    onIceCandidate?.invoke(candidate, type)
  }

  /**
   * Triggered whenever there's a new [MediaStream] that was added to the connection.
   *
   * @param stream The stream that contains audio or video.
   */
  override fun onAddStream(stream: MediaStream?) {
    Log.i(this::class.java.name, "[onAddStream] #sfu; stream: $stream")
    if (stream != null) {
      onStreamAdded?.invoke(stream)
    }
  }

  /**
   * Triggered whenever there's a new [MediaStream] or [MediaStreamTrack] that's been added
   * to the call. It contains all audio and video tracks for a given session.
   *
   * @param receiver The receiver of tracks.
   * @param mediaStreams The streams that were added containing their appropriate tracks.
   */
  override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
    Log.i(this::class.java.name, "[onAddTrack] #sfu; receiver: $receiver, mediaStreams: $mediaStreams")
    mediaStreams?.forEach { mediaStream ->
      Log.v(this::class.java.name, "[onAddTrack] #sfu; mediaStream: $mediaStream")
      mediaStream.audioTracks?.forEach { remoteAudioTrack ->
        Log.v(this::class.java.name, "[onAddTrack] #sfu; remoteAudioTrack: $remoteAudioTrack")
        remoteAudioTrack.setEnabled(true)
      }
      onStreamAdded?.invoke(mediaStream)
    }
  }

  /**
   * Triggered whenever there's a new negotiation needed for the active [PeerConnection].
   */
  override fun onRenegotiationNeeded() {
    Log.i(this::class.java.name, "[onRenegotiationNeeded] #sfu; no args")
    onNegotiationNeeded?.invoke(this, type)
  }

  /**
   * Triggered whenever a [MediaStream] was removed.
   *
   * @param stream The stream that was removed from the connection.
   */
  override fun onRemoveStream(stream: MediaStream?) {}

  /**
   * Triggered when the connection state changes.  Used to start and stop the stats observing.
   *
   * @param newState The new state of the [PeerConnection].
   */
  override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
    Log.i(this::class.java.name, "[onIceConnectionChange] #sfu; newState: $newState")
    when (newState) {
      PeerConnection.IceConnectionState.CLOSED,
      PeerConnection.IceConnectionState.FAILED,
      PeerConnection.IceConnectionState.DISCONNECTED -> statsJob?.cancel()
      PeerConnection.IceConnectionState.CONNECTED -> statsJob = observeStats()
      else -> Unit
    }
  }

  /**
   * @return The [RTCStatsReport] for the active connection.
   */
  fun getStats(): StateFlow<RTCStatsReport?> {
    return statsFlow
  }

  /**
   * Observes the local connection stats and emits it to [statsFlow] that users can consume.
   */
  private fun observeStats() = coroutineScope.launch {
    while (isActive) {
      delay(10_000L)
      connection.getStats {
        Log.v(this::class.java.name, "[observeStats] #sfu; stats: $it")
        statsFlow.value = it
      }
    }
  }

  override fun onTrack(transceiver: RtpTransceiver?) {
    Log.i(this::class.java.name, "[onTrack] #sfu; transceiver: $transceiver")
    onVideoTrack?.invoke(transceiver)
  }

  /**
   * Domain - [PeerConnection] and [PeerConnection.Observer] related callbacks.
   */
  override fun onRemoveTrack(receiver: RtpReceiver?) {
    Log.i(this::class.java.name, "[onRemoveTrack] #sfu; receiver: $receiver")
  }

  override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
    Log.d(this::class.java.name, "[onSignalingChange] #sfu; newState: $newState")
  }

  override fun onIceConnectionReceivingChange(receiving: Boolean) {
    Log.i(this::class.java.name, "[onIceConnectionReceivingChange] #sfu; receiving: $receiving")
  }

  override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
    Log.i(this::class.java.name, "[onIceGatheringChange] #sfu; newState: $newState")
  }

  override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
    Log.i(this::class.java.name, "[onIceCandidatesRemoved] #sfu; iceCandidates: $iceCandidates")
  }

  override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
    Log.e(this::class.java.name, "[onIceCandidateError] #sfu; event: $event")
  }

  override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
    Log.i(this::class.java.name, "[onConnectionChange] #sfu; newState: $newState")
  }

  override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
    Log.i(this::class.java.name, "[onSelectedCandidatePairChanged] #sfu; event: $event")
  }

  override fun onDataChannel(channel: DataChannel?): Unit = Unit

  override fun toString(): String =
    "StreamPeerConnection(constraints=$mediaConstraints)"

  private fun String.mungeCodecs(): String {
    return this.replace("vp9", "VP9").replace("vp8", "VP8").replace("h264", "H264")
  }
}
