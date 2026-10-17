package com.pintapontus.chatting

import io.ktor.client.webrtc.IosWebRtc
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
actual fun getClient(): WebRtcClient {
    return WebRtcClient(IosWebRtc) {
        defaultConnectionConfig = {
            iceServers = listOf(WebRtc.IceServer("stun:stun.l.google.com:19302"))
        }
    }
}