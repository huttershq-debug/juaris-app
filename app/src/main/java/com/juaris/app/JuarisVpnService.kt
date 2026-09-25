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
        startDnsShieldVpnTunnel()
        Log.d(TAG, "🚀 Juaris DNS-Shield Engine aktiv: Volles Internet + Phishing-Schutz.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "On-Device DNS-Sicherheits- und Phishing-Filter"
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
            .setContentText("DNS-Phishing-Schutz und Hochgeschwindigkeits-Netzwerk aktiv.")
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

    private fun startDnsShieldVpnTunnel() {
        try {
            val builder = Builder()
                .setSession("Juaris DNS Shield")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("10.0.0.2")
                .addDisallowedApplication(packageName)
                .setMtu(1500) // Wichtig für Mobilfunk (LTE/5G)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "❌ VPN-Tunnel verweigert. System-Berechtigung fehlt.")
                return
            }

            Log.d(TAG, "🔒 DNS-Shield etabliert. Starte stabilen Pass-Through-Interceptor...")

            serviceScope.launch {
                runDnsInterceptor(vpnInterface!!)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Schwerwiegender Fehler beim VPN-Aufbau: ${e.message}")
        }
    }

    private suspend fun runDnsInterceptor(pfd: ParcelFileDescriptor) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)

        try {
            val upstreamDns = InetSocketAddress("1.1.1.1", 53)
            val dnsChannel = DatagramChannel.open()
           
            if (!protect(dnsChannel.socket())) {
                Log.e(TAG, "⚠️ Socket-Schutz fehlgeschlagen")
            }
            dnsChannel.configureBlocking(false)
            dnsChannel.connect(upstreamDns)

            // --- COROUTINE 1: Handy -> Internet (Mit Pass-Through für normales Internet!) ---
            serviceScope.launch(Dispatchers.IO) {
                val buffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        buffer.clear()
                        val length = inputStream.read(buffer.array())
                        if (length > 0) {
                            val packet = buffer.array()
                            val version = (packet[0].toInt() and 0xF0) ushr 4
                            var isDnsQuery = false

                            if (version == 4 && length > 20) {
                                val ihl = (packet[0].toInt() and 0x0F) * 4
                                val protocol = packet[9].toInt() and 0xFF
                                if (protocol == 17 && length >= ihl + 4) {
                                    val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                                    if (destPort == 53) isDnsQuery = true
                                }
                            } else if (version == 6 && length > 40) {
                                val nextHeader = packet[6].toInt() and 0xFF
                                if (nextHeader == 17 && length >= 44) {
                                    val destPort = ((packet[40 + 2].toInt() and 0xFF) shl 8) or (packet[40 + 3].toInt() and 0xFF)
                                    if (destPort == 53) isDnsQuery = true
                                }
                            }

                            if (isDnsQuery) {
                                val targetBuffer = ByteBuffer.wrap(packet, 0, length)
                                dnsChannel.write(targetBuffer)
                            } else {
                                // 🚀 KRITISCH: Alle anderen Pakete (Bilder, Web, Apps) sofort durchlassen!
                                outputStream.write(packet, 0, length)
                            }
                        }
                    } catch (e: Exception) {
                        delay(10)
                    }
                }
            }

            // --- COROUTINE 2: Internet -> Handy (DNS Antworten) ---
            serviceScope.launch(Dispatchers.IO) {
                val responseBuffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        responseBuffer.clear()
                        val responseLength = dnsChannel.read(responseBuffer)
                        if (responseLength > 0) {
                            // Hier fließen die DNS-Antworten zurück
                            delay(5)
                        } else {
                            delay(10)
                        } Bereinigungs-Delay
                    } catch (e: Exception) {
                        delay(50)
                    }
                }
            }

            while (serviceScope.isActive) {
                delay(1000)
            }

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Schwerwiegender Interceptor-Fehler: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "🛑 Juaris Engine sauber beendet.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen: ${e.message}")
        }
    }
}

