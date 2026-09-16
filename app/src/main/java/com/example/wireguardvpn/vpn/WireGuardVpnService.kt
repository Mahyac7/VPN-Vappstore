package com.example.wireguardvpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wireguardvpn.MainActivity
import com.example.wireguardvpn.R
import com.wireguard.android.backend.GoBackend

/**
 * The [GoBackend] provides its own internal VpnService, but Android requires the app to declare a
 * VpnService component in the manifest and to run a foreground notification while a tunnel is
 * active. We extend the library's [GoBackend.VpnService] so the manifest declaration is satisfied
 * and we can attach a persistent notification.
 */
class WireGuardVpnService : GoBackend.VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return super.onStartCommand(intent, flags, startId)
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows the WireGuard tunnel status" }
            nm.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("VPN tunnel is active")
            .setSmallIcon(R.drawable.ic_vpn)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "wg_vpn_status"
        private const val NOTIFICATION_ID = 1001
    }
}
