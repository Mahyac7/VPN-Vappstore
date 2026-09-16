package com.example.wireguardvpn.vpn

import android.content.Context
import android.util.Log
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.StringReader

/**
 * Singleton wrapper around the WireGuard [GoBackend].
 *
 * Responsibilities:
 *  - Parse a raw WireGuard `.conf` string into a [Config].
 *  - Bring the tunnel up / down through the userspace Go backend (no root required).
 *  - Expose the current [Tunnel.State] as a [StateFlow] for the UI.
 */
object VpnManager {

    private const val TAG = "VpnManager"
    private const val TUNNEL_NAME = "wg-personal"

    private var backend: Backend? = null

    private val _state = MutableStateFlow(Tunnel.State.DOWN)
    val state: StateFlow<Tunnel.State> = _state.asStateFlow()

    private val tunnel = WgTunnel(TUNNEL_NAME) { newState ->
        _state.value = newState
    }

    private fun getBackend(context: Context): Backend {
        return backend ?: GoBackend(context.applicationContext).also { backend = it }
    }

    /** Parse a raw WireGuard config string into a [Config], or throw with a clear message. */
    fun parseConfig(raw: String): Config {
        BufferedReader(StringReader(raw)).use { reader ->
            return Config.parse(reader)
        }
    }

    suspend fun connect(context: Context, rawConfig: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val config = parseConfig(rawConfig)
                getBackend(context).setState(tunnel, Tunnel.State.UP, config)
                Log.i(TAG, "Tunnel brought UP")
            }.onFailure { Log.e(TAG, "connect() failed", it) }
        }

    suspend fun disconnect(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                getBackend(context).setState(tunnel, Tunnel.State.DOWN, null)
                Log.i(TAG, "Tunnel brought DOWN")
            }.onFailure { Log.e(TAG, "disconnect() failed", it) }
        }

    /** True while the tunnel is up. */
    fun isConnected(): Boolean = _state.value == Tunnel.State.UP
}
