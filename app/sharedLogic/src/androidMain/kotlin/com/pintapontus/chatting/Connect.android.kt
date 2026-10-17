package com.pintapontus.chatting

import io.ktor.client.webrtc.AndroidWebRtc
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
actual fun getClient(): WebRtcClient {
    return WebRtcClient(AndroidWebRtc) {
        context = appContext // Required: provide Android context
        defaultConnectionConfig = {
            iceServers = listOf(WebRtc.IceServer("stun:stun.l.google.com:19302"))
        }
    }
}