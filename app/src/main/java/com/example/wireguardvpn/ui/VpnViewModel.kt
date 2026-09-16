package com.example.wireguardvpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wireguardvpn.data.ConfigStore
import com.example.wireguardvpn.vpn.VpnManager
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ConfigStore(app)

    private val _config = MutableStateFlow(store.load().orEmpty())
    val config: StateFlow<String> = _config.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val tunnelState: StateFlow<Tunnel.State> = VpnManager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Tunnel.State.DOWN)

    fun onConfigChanged(value: String) {
        _config.value = value
    }

    fun saveConfig() {
        store.save(_config.value)
    }

    /** Validate the config locally; returns true if it parses. */
    fun validateConfig(): Boolean {
        return runCatching { VpnManager.parseConfig(_config.value) }
            .onFailure { _error.value = "Config tidak valid: ${it.message}" }
            .isSuccess
    }

    fun connect() {
        _error.value = null
        saveConfig()
        viewModelScope.launch {
            VpnManager.connect(getApplication(), _config.value)
                .onFailure { _error.value = "Gagal connect: ${it.message}" }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            VpnManager.disconnect(getApplication())
                .onFailure { _error.value = "Gagal disconnect: ${it.message}" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
