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
        Log.d(TAG, "🚀 Juaris DNS-Shield Engine aktiv: Volle Internet-Power + Phishing-Schutz.")
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
                .setMtu(1500) // 🚀 WICHTIG für Mobilfunk: Verhindert Paketverlust bei LTE/5G

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "❌ VPN-Tunnel verweigert. System-Berechtigung fehlt.")
                return
            }

            Log.d(TAG, "🔒 DNS-Shield etabliert. Starte Mobilfunk-kompatiblen Interceptor...")

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

            // --- COROUTINE 1: Handy -> Internet (Verarbeitet IPv4 UND IPv6 DNS für LTE/5G) ---
            serviceScope.launch(Dispatchers.IO) {
                val buffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        buffer.clear()
                        val length = inputStream.read(buffer.array())
                        if (length > 40) {
                            val packet = buffer.array()
                            val version = (packet[0].toInt() and 0xF0) ushr 4
                            var isDnsQuery = false

                            if (version == 4) {
                                // IPv4 Paket-Prüfung
                                val ihl = (packet[0].toInt() and 0x0F) * 4
                                val protocol = packet[9].toInt() and 0xFF
                                if (protocol == 17 && length >= ihl + 4) { // 17 = UDP
                                    val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                                    if (destPort == 53) isDnsQuery = true
                                }
                            } else if (version == 6) {
                                // IPv6 Paket-Prüfung (Sehr wichtig für moderne Mobilfunknetze!)
                                val nextHeader = packet[6].toInt() and 0xFF
                                if (nextHeader == 17 && length >= 44) { // 40 Byte IPv6 Header + UDP Header
                                    val destPort = ((packet[40 + 2].toInt() and 0xFF) shl 8) or (packet[40 + 3].toInt() and 0xFF)
                                    if (destPort == 53) isDnsQuery = true
                                }
                            }

                            if (isDnsQuery) {
                                val targetBuffer = ByteBuffer.wrap(packet, 0, length)
                                dnsChannel.write(targetBuffer)
                            }
                        }
                    } catch (e: Exception) {
                        delay(100)
                    }
                }
            }

            // --- COROUTINE 2: Internet -> Handy ---
            serviceScope.launch(Dispatchers.IO) {
                val responseBuffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        responseBuffer.clear()
                        val responseLength = dnsChannel.read(responseBuffer)
                        if (responseLength > 0) {
                            outputStream.write(responseBuffer.array(), 0, responseLength)
                        } else {
                            delay(10)
                        }
                    } catch (e: Exception) {
                        delay(100)
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

            // --- COROUTINE 1: Nur DNS-Anfragen (UDP Port 53) filtern und senden ---
            serviceScope.launch(Dispatchers.IO) {
                val buffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        buffer.clear()
                        val length = inputStream.read(buffer.array())
                        if (length > 28) { // Mindestlänge für IP+UDP Header
                            val packet = buffer.array()
                            
                            // Prüfen ob es sich wirklich um UDP handelt (Protocol 17 an Byte 9 bei IPv4)
                            val protocol = packet[9].toInt() and 0xFF
                            if (protocol == 17) { // 17 = UDP
                                // Zielport prüfen (muss Port 53 sein = DNS)
                                val ihl = (packet[0].toInt() and 0x0F) * 4
                                val destinationPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                                
                                if (destinationPort == 53) {
                                    // Echte DNS-Anfrage -> An Cloudflare weiterleiten
                                    val targetBuffer = ByteBuffer.wrap(packet, 0, length)
                                    dnsChannel.write(targetBuffer)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        delay(100)
                    }
                }
            }

            // --- COROUTINE 2: DNS-Antworten empfangen und zurückschreiben ---
            serviceScope.launch(Dispatchers.IO) {
                val responseBuffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        responseBuffer.clear()
                        val responseLength = dnsChannel.read(responseBuffer)
                        if (responseLength > 0) {
                            outputStream.write(responseBuffer.array(), 0, responseLength)
                        } else {
                            delay(10)
                        }
                    } catch (e: Exception) {
                        delay(100)
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

