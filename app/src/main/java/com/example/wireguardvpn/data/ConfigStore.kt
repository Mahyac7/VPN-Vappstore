package com.example.wireguardvpn.data

import android.content.Context

/**
 * Persists the raw WireGuard config in SharedPreferences.
 *
 * NOTE: For a real product you should encrypt this (it contains the client private key),
 * e.g. with androidx.security:security-crypto EncryptedSharedPreferences. Kept plain here
 * for learning-project clarity.
 */
class ConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(config: String) {
        prefs.edit().putString(KEY_CONFIG, config).apply()
    }

    fun load(): String? = prefs.getString(KEY_CONFIG, null)

    companion object {
        private const val PREFS = "wg_prefs"
        private const val KEY_CONFIG = "wg_config"
    }
}
