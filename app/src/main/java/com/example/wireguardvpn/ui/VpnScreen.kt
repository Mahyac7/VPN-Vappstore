package com.example.wireguardvpn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wireguard.android.backend.Tunnel

@Composable
fun VpnScreen(
    viewModel: VpnViewModel,
    onRequestConnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val state by viewModel.tunnelState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val connected = state == Tunnel.State.UP

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "WireGuard VPN",
            style = MaterialTheme.typography.headlineMedium,
        )

        Icon(
            imageVector = if (connected) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = null,
            tint = if (connected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(96.dp),
        )

        Text(
            text = when (state) {
                Tunnel.State.UP -> "Terhubung"
                Tunnel.State.DOWN -> "Terputus"
                else -> "Menghubungkan…"
            },
            style = MaterialTheme.typography.titleLarge,
        )

        OutlinedTextField(
            value = config,
            onValueChange = viewModel::onConfigChanged,
            label = { Text("Tempel WireGuard config (.conf)") },
            placeholder = { Text("[Interface]\nPrivateKey = ...\nAddress = 10.0.0.2/32\n...") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
            enabled = !connected,
        )

        if (connected) {
            OutlinedButton(
                onClick = viewModel::disconnect,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Disconnect") }
        } else {
            Button(
                onClick = {
                    if (viewModel.validateConfig()) onRequestConnect()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Connect") }
        }

        error?.let { msg ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
