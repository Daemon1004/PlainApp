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
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SignalingClient(private val mSocket: Socket, private val chatId: Long) {
  private val signalingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  // signaling commands to send commands to value pairs to the subscribers
  private val _signalingCommandFlow = MutableSharedFlow<Pair<SignalingCommand, String>>()
  val signalingCommandFlow: SharedFlow<Pair<SignalingCommand, String>> = _signalingCommandFlow

  fun sendCommand(signalingCommand: SignalingCommand, message: String) {
    Log.d(this::class.java.name, "[sendCommand] $signalingCommand (${signalingCommand.serverSignal}) $message" )
    mSocket.emit(signalingCommand.serverSignal, message, chatId.toString())
  }

  init {

    mSocket.on(SignalingCommand.OFFER.serverSignal) { text -> handleSignalingCommand(SignalingCommand.OFFER, text[0].toString()) }
    mSocket.on(SignalingCommand.ICE.serverSignal) { text -> handleSignalingCommand(SignalingCommand.ICE, text[0].toString()) }
    mSocket.on(SignalingCommand.ANSWER.serverSignal) { text -> handleSignalingCommand(SignalingCommand.ANSWER, text[0].toString()) }
  }

  private fun handleSignalingCommand(command: SignalingCommand, text: String) {
    Log.d(this::class.java.name, "received signaling: $command $text" )
    signalingScope.launch {
      _signalingCommandFlow.emit(command to text)
    }
  }

  fun dispose() {
    signalingScope.cancel()
  }
}

enum class SignalingCommand(val serverSignal: String) {
  OFFER("offer"), // to send or receive offer
  ANSWER("answer"), // to send or receive answer
  ICE("ice candidate") // to send and receive ice candidates
}
