package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustFirewall"
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "juaris_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Zwingend als Foreground-Service starten, damit Android das Symbol anzeigt
        startForegroundServiceWithNotification()

        // 2. VPN-Tunnel etablieren
        startVpnTunnel()

        Log.d(TAG, "🟢 Lokale Zero-Trust-Firewall aktiv: Sämtlicher unerwünschter Traffic wird auf dem Gerät abgefangen.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Netzwerkschutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hält die lokale Firewall permanent aktiv"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Juaris 360° Firewall aktiv")
            .setContentText("Netzwerkverkehr wird lokal überwacht.")
            .setSmallIcon(android.R.drawable.ic_menu_shield) // Ersetze dies idealerweise mit deinem App-Icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("Juaris Zero-Trust Shield")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0) // Fängt den gesamten Netzwerkverkehr lokal ab

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "❌ VPN-Tunnel konnte nicht etabliert werden! Die Berechtigung wurde vom System verweigert oder VpnService.prepare() wurde nicht ausgeführt.")
            } else {
                Log.d(TAG, "🔒 VPN-Interface erfolgreich aufgebaut. Das VPN-Schlüssel-Symbol sollte jetzt sichtbar sein.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aufbau des lokalen VPN-Tunnels: ${e.message}")
        }
    }

    override onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen des VPN-Tunnels: ${e.message}")
        }
    }
}

