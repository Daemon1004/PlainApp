package com.example.plainapp.rtc

import kotlinx.serialization.Serializable

@Serializable
class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Double,
    val sdpCandidate: String
)