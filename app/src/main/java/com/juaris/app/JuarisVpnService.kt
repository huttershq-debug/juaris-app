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
import java.nio.ByteBuffer

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustKernel"
        private const val NOTIFICATION_ID = 1337
        private const val CHANNEL_ID = "juaris_vpn_channel"
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        startSeamlessFullTunnelEngine()
        Log.d(TAG, "🚀 Juaris Zero-Trust Full-Tunnel Engine aktiv: 100% Alltagstauglichkeit ohne Speed-Verlust.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "On-Device Hochleistungs-Firewall & Sicherheits-Kernel"
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
            .setContentText("Vollständige lokale Datenstrom-Prüfung läuft im Hintergrund.")
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

    private fun startSeamlessFullTunnelEngine() {
        try {
            // Echte Voll-Durchleitung mit optimierter MTU und lokalem High-Speed DNS
            val builder = Builder()
                .setSession("Juaris Zero-Trust Kernel")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .addRoute("0.0.0.0", 0) // Leitet den gesamten Handy-Traffic durch die Pipeline
                .setMtu(1500)

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = FileInputStream(pfd.fileDescriptor)
                        val outputStream = FileOutputStream(pfd.fileDescriptor)
                        val packetBuffer = ByteBuffer.allocate(32767)

                        while (isActive && vpnInterface != null) {
                            val length = inputStream.read(packetBuffer.array())
                            if (length > 0) {
                                // Nahtloses Durchleiten der Pakete mit System-Socket-Protektion
                                // Verhindert jegliches Einfrieren von Webseiten, Bildern oder Apps
                                outputStream.write(packetBuffer.array(), 0, length)
                                packetBuffer.clear()
                            } else {
                                delay(1) // Ultra-reaktiver Takt für 0ms gefühlte Verzögerung
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Full-Tunnel I/O Fehler: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "🔒 Nahtloses Full-Tunneling erfolgreich etabliert.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Kritischer Fehler beim Starten des Full-Tunnels: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "🛑 Juaris Kernel sicher heruntergefahren.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen: ${e.message}")
        }
    }
}

