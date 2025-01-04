package com.example.plainapp.rtc.models

import kotlinx.serialization.Serializable

@Serializable
class IceCandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Double,
    val sdpCandidate: String
)