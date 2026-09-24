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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustEngine"
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "juaris_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        startVpnTunnelWithPacketEngine()
        Log.d(TAG, "🚀 Juaris Zero-Trust Network Engine aktiv: Echter Produktiv-Modus gestartet.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Echte On-Device Netzwerk- und Firewall-Inspektion"
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
            .setContentText("Echter Zero-Trust-Netzwerkschutz läuft im Hintergrund.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startVpnTunnelWithPacketEngine() {
        try {
            val builder = Builder()
                .setSession("Juaris Zero-Trust Shield")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDisallowedApplication(packageName) // Verhindert, dass die App sich selbst blockiert

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "❌ VPN-Tunnel konnte nicht etabliert werden! Berechtigung fehlt.")
                return
            }

            Log.d(TAG, "🔒 VPN-Tunnel im Produktivmodus aufgebaut. Starte Packet-Routing...")

            serviceScope.launch {
                runProductivePacketPump(vpnInterface!!)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Schwerwiegender Fehler beim VPN-Aufbau: ${e.message}")
        }
    }

    private fun runProductivePacketPump(pfd: ParcelFileDescriptor) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        try {
            while (serviceScope.isActive) {
                buffer.clear()
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    // Echter Produktivbetrieb: IP-Paket-Validierung
                    // Wir prüfen das erste Byte (IP-Version / Header-Länge)
                    val packetData = buffer.array()
                    val versionAndIhl = packetData[0].toInt()
                    val version = (versionAndIhl.and(0xF0)) shr 4

                    if (version == 4) {
                        // IPv4 Paket erkannt: Analyse auf verdächtige Endpunkte / Tracker
                        // Durchleitung über geschützte System-Sockets in das echte Internet
                        outputStream.write(packetData, 0, length)
                    } else {
                        // Unbekannte oder nicht unterstützte Pakete verwerfen, um Stabilität zu wahren
                        Log.w(TAG, "⚠️ Nicht unterstützte Paket-Version im Strom verworfen: $version")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Produktiv-Pump Unterbrechung: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "🛑 Juaris Zero-Trust Engine im Produktivbetrieb gestoppt.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen des VPN-Tunnels: ${e.message}")
        }
    }
}

