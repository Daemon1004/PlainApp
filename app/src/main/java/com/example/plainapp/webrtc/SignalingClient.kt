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

package com.example.plainapp.webrtc

import android.util.Log
import com.example.plainapp.SocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SignalingClient(service: SocketService, private val chatId: Long) {
  private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val mSocket = service.mSocket

  // session flow to send information about the session state to the subscribers
  private val _sessionStateFlow = MutableStateFlow(WebRTCSessionState.Offline)
  val sessionStateFlow: StateFlow<WebRTCSessionState> = _sessionStateFlow

  // signaling commands to send commands to value pairs to the subscribers
  private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, String>>()
  val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, String>> = _signalingCommandFlow

  fun sendCommand(signalingCommand: SignalingCommand, message: String) {
    Log.d(this::class.java.name, "[sendCommand] $signalingCommand $message" )
    mSocket.send(signalingCommand.serverSignal, message, chatId.toString())
  }

  init {

    mSocket.on(SignalingCommand.OFFER.serverSignal) { text -> handleSignalingCommand(SignalingCommand.OFFER, text.toString()) }
    mSocket.on(SignalingCommand.ICE.serverSignal) { text -> handleSignalingCommand(SignalingCommand.ICE, text.toString()) }
    mSocket.on(SignalingCommand.ANSWER.serverSignal) { text -> handleSignalingCommand(SignalingCommand.ANSWER, text.toString()) }
    mSocket.on(SignalingCommand.STATE.serverSignal) { text -> handleStateMessage(text.toString()) }

  }

  private var callbacks = emptyList<(WebRTCSessionState) -> Unit>().toMutableList()
  fun onStateChangeListener(callback: (WebRTCSessionState) -> Unit) { callbacks += callback }
  fun removeStateChangeListener(callback: (WebRTCSessionState) -> Unit) { callbacks -= callback }

  private fun handleStateMessage(message: String) {
    val value = WebRTCSessionState.valueOf(message)
    _sessionStateFlow.value = value
    for (callback in callbacks) callback(value)
  }

  fun handleSignalingCommand(command: SignalingCommand, text: String) {
    val value = text
    Log.d(this::class.java.name, "received signaling: $command $value" )
    signalingScope.launch {
      _signalingCommandFlow.emit(command to value)
    }
  }

  fun dispose() {
    _sessionStateFlow.value = WebRTCSessionState.Offline
    signalingScope.cancel()
  }
}

enum class WebRTCSessionState {
  Active, // Offer and Answer messages has been sent
  Creating, // Creating session, offer has been sent
  Ready, // Both clients available and ready to initiate session
  Impossible, // We have less than two clients connected to the server
  Offline // unable to connect signaling server
}

enum class SignalingCommand(val serverSignal: String) {
  STATE("state"), // Command for WebRTCSessionState
  OFFER("offer"), // to send or receive offer
  ANSWER("answer"), // to send or receive answer
  ICE("ice candidate") // to send and receive ice candidates
}
