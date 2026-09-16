package com.example.wireguardvpn.vpn

import com.wireguard.android.backend.Tunnel

/**
 * Minimal [Tunnel] implementation required by the WireGuard [com.wireguard.android.backend.GoBackend].
 *
 * The GoBackend manages the underlying VpnService and packet routing for us; we only need to
 * provide a name and a callback that fires whenever the tunnel state changes.
 */
class WgTunnel(
    private val name: String,
    private val onState: (Tunnel.State) -> Unit,
) : Tunnel {

    @Volatile
    var state: Tunnel.State = Tunnel.State.DOWN
        private set

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        state = newState
        onState(newState)
    }
}
