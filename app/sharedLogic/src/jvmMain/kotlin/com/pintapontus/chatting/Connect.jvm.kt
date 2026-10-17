package com.pintapontus.chatting

import io.ktor.client.webrtc.JvmWebRtc
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
actual fun getClient(): WebRtcClient {
    return WebRtcClient(JvmWebRtc) {
        defaultConnectionConfig = {
            iceServers = listOf(WebRtc.IceServer("stun:stun.l.google.com:19302"))
        }
    }
}