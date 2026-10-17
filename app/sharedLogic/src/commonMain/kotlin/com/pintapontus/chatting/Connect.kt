package com.pintapontus.chatting

import io.ktor.client.webrtc.*
import io.ktor.utils.io.ExperimentalKtorApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalKtorApi::class)
expect fun getClient(): WebRtcClient

// ---------------------------------------------------------------------------
// HANDSHAKE (SDP)
// ---------------------------------------------------------------------------

/** CALLER: crea l'offerta, la imposta come descrizione locale e ne restituisce l'SDP da inviare via signaling. */
@OptIn(ExperimentalKtorApi::class)
suspend fun createOfferSdp(caller: WebRtcPeerConnection): String {
    val offer = caller.createOffer()
    caller.setLocalDescription(offer)
    return offer.sdp
}

/** CALLEE: accetta l'SDP dell'offerta, crea la risposta e ne restituisce l'SDP da rimandare al caller. */
@OptIn(ExperimentalKtorApi::class)
suspend fun acceptOffer(callee: WebRtcPeerConnection, remoteOfferSdp: String): String {
    callee.setRemoteDescription(
        WebRtc.SessionDescription(WebRtc.SessionDescriptionType.OFFER, remoteOfferSdp)
    )
    val answer = callee.createAnswer()
    callee.setLocalDescription(answer)
    return answer.sdp
}

/** CALLER: applica la risposta ricevuta dal callee. */
@OptIn(ExperimentalKtorApi::class)
suspend fun acceptAnswer(caller: WebRtcPeerConnection, remoteAnswerSdp: String) {
    caller.setRemoteDescription(
        WebRtc.SessionDescription(WebRtc.SessionDescriptionType.ANSWER, remoteAnswerSdp)
    )
}

// ---------------------------------------------------------------------------
// ICE CANDIDATES
// ---------------------------------------------------------------------------

/**
 * Inoltra i candidati ICE locali di [from] verso [to].
 *
 * Nella realtà ogni candidato viaggerebbe sul canale di signaling (candidate, sdpMid, sdpMLineIndex).
 * Qui [from] e [to] vivono nello stesso processo, quindi li passiamo direttamente.
 *
 * - Il collector parte in modo UNDISPATCHED, così non perde candidati emessi subito dopo setLocalDescription.
 * - [remoteDescriptionSet] ritarda addIceCandidate finché il peer di destinazione
 *   non ha impostato la remote description (altrimenti l'aggiunta fallirebbe).
 */
fun CoroutineScope.forwardIceCandidates(
    from: WebRtcPeerConnection,
    to: WebRtcPeerConnection,
    remoteDescriptionSet: CompletableDeferred<Unit>,
): Job = launch(start = CoroutineStart.UNDISPATCHED) {
    from.iceCandidates.collect { candidate ->
        remoteDescriptionSet.await()
        to.addIceCandidate(WebRtc.IceCandidate(candidate.candidate, candidate.sdpMid, candidate.sdpMLineIndex))
    }
}

// ---------------------------------------------------------------------------
// DATA CHANNEL
// ---------------------------------------------------------------------------

/** CALLEE: resta in ascolto e restituisce il canale quando il caller lo apre. */
fun CoroutineScope.awaitIncomingDataChannel(
    callee: WebRtcPeerConnection,
): Pair<Job, CompletableDeferred<WebRtcDataChannel>> {
    val incoming = CompletableDeferred<WebRtcDataChannel>()
    val job = launch(start = CoroutineStart.UNDISPATCHED) {
        callee.dataChannelEvents.collect { event ->
            if (event is DataChannelEvent.Open) incoming.complete(event.channel)
        }
    }
    return job to incoming
}

// ---------------------------------------------------------------------------
// FLUSSO COMPLETO
// ---------------------------------------------------------------------------

@OptIn(ExperimentalKtorApi::class)
suspend fun connect(onLog: (String) -> Unit = {}) = coroutineScope {
    val client = getClient()
    val caller = client.createPeerConnection()
    val callee = client.createPeerConnection()
    onLog("Peer connection create")

    // Segnali "remote description impostata" per ciascun peer
    val calleeReady = CompletableDeferred<Unit>()
    val callerReady = CompletableDeferred<Unit>()

    // 1) Prima di tutto: avvia ICE e ascolto del data channel (prima di setLocalDescription!)
    val iceJobs = listOf(
        forwardIceCandidates(caller, callee, calleeReady),
        forwardIceCandidates(callee, caller, callerReady),
    )
    val (channelListener, incomingChannel) = awaitIncomingDataChannel(callee)

    // 2) Il caller crea il data channel PRIMA dell'offerta,
    //    altrimenti l'SDP non conterrà la sezione "application" necessaria.
    val callerChannel = caller.createDataChannel("chat")

    // 3) Handshake SDP
    val offerSdp = createOfferSdp(caller)            // caller -> (signaling) -> callee
    onLog("Offer creata e inviata al callee")
    val answerSdp = acceptOffer(callee, offerSdp)    // callee -> (signaling) -> caller
    onLog("Answer creata e inviata al caller")
    calleeReady.complete(Unit)
    acceptAnswer(caller, answerSdp)
    callerReady.complete(Unit)
    onLog("Handshake completato, scambio ICE in corso...")

    // 4) Il callee riceve il canale, il caller invia il messaggio
    try {
        withTimeout(15_000.milliseconds) {
            val calleeChannel = incomingChannel.await()
            onLog("Data channel aperto")

            callerChannel.send("Ciao dal caller!")
            val received = calleeChannel.receiveText()
            onLog("Callee ha ricevuto: $received")

            // 5) Chiusura del canale
            callerChannel.close()
            calleeChannel.close()
            onLog("Canale chiuso")
        }
    } finally {
        // Pulizia
        channelListener.cancel()
        iceJobs.forEach { it.cancel() }
        caller.close()
        callee.close()
        client.close()
    }
}