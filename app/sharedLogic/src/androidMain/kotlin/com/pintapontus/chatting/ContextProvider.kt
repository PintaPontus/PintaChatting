package com.pintapontus.chatting

import android.content.Context

/**
 * Application context, inizializzato una sola volta all'avvio dell'app Android
 * (vedi ChattingApplication nel modulo androidApp).
 */
lateinit var appContext: Context
    private set

fun initAppContext(context: Context) {
    // Si usa sempre l'applicationContext per evitare di trattenere una Activity (memory leak)
    appContext = context.applicationContext
}