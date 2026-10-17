package com.pintapontus.chatting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var isConnecting by remember { mutableStateOf(false) }
        val logs = remember { mutableStateListOf<String>() }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                enabled = !isConnecting,
                onClick = {
                    logs.clear()
                    isConnecting = true
                    scope.launch {
                        try {
                            connect { logs.add(it) }
                        } catch (e: Exception) {
                            logs.add("Errore: ${e.message}")
                        } finally {
                            isConnecting = false
                        }
                    }
                }
            ) {
                Text(if (isConnecting) "Connessione..." else "Avvia WebRTC")
            }

            logs.forEach { Text(it) }
        }
    }
}