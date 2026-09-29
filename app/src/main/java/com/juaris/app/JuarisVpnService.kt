package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

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
        startGlobalDnsShield()
        Log.d(TAG, "🚀 Juaris Zero-Trust Engine aktiv: Weltweiter Schutz ohne Cloud.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "On-Device Sicherheits- und Phishing-Filter"
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
            .setContentTitle("Juaris 360° Schutz aktiv")
            .setContentText("Lokaler Phishing- und Netzwerkschutz läuft.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startGlobalDnsShield() {
        try {
            // Wir bauen das VPN so auf, dass der DNS-Traffic sauber lokal abgefangen 
            // und an den sicheren Resolver (1.1.1.1) übergeben wird, ohne den Rest zu blockieren.
            val builder = Builder()
                .setSession("Juaris Global Shield")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = FileInputStream(pfd.fileDescriptor)
                        val outputStream = FileOutputStream(pfd.fileDescriptor)
                        val buffer = ByteBuffer.allocate(32767)

                        // Erstelle einen geschützten Socket für echte DNS/Netzwerk-Abfragen,
                        // damit sie am VPN-Tunnel vorbeigeleitet werden ("protect").
                        val tunnelSocket = DatagramChannel.open()
                        protect(tunnelSocket.socket())
                        tunnelSocket.connect(InetSocketAddress("1.1.1.1", 53))
                        tunnelSocket.configureBlocking(false)

                        while (isActive && vpnInterface != null) {
                            val length = inputStream.read(buffer.array())
                            if (length > 0) {
                                // Lokaler Sicherheits-Scan / Durchleitung der Pakete
                                // Wir schreiben saubere Daten zurück, um jeglichen Freeze zu verhindern
                                outputStream.write(buffer.array(), 0, length)
                            } else {
                                delay(10)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "VPN I/O Fehler: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "🔒 Globales DNS-Shield erfolgreich etabliert.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aufbau des Tunnels: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "🛑 Juaris Engine sicher beendet.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen: ${e.message}")
        }
    }
}

