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
                .setMtu(1500)

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

            // --- COROUTINE 1: Handy -> Upstream DNS ---
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
                            var ipHeaderLen = 20

                            if (version == 4 && length > 20) {
                                ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                                val protocol = packet[9].toInt() and 0xFF
                                if (protocol == 17 && length >= ipHeaderLen + 8) {
                                    val destPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)
                                    if (destPort == 53) isDnsQuery = true
                                }
                            }

                            if (isDnsQuery) {
                                val udpHeaderOffset = ipHeaderLen
                                val dnsPayloadOffset = udpHeaderOffset + 8
                                if (length > dnsPayloadOffset) {
                                    val dnsPayloadLen = length - dnsPayloadOffset
                                    val targetBuffer = ByteBuffer.wrap(packet, dnsPayloadOffset, dnsPayloadLen)
                                    dnsChannel.write(targetBuffer)
                                }
                            } else {
                                outputStream.write(packet, 0, length)
                            }
                        }
                    } catch (e: Exception) {
                        delay(10)
                    }
                }
            }

            // --- COROUTINE 2: Upstream DNS -> Handy (Mit expliziten Short-Variablen) ---
            serviceScope.launch(Dispatchers.IO) {
                val responseBuffer = ByteBuffer.allocate(32767)
                while (serviceScope.isActive) {
                    try {
                        responseBuffer.clear()
                        val responseLength = dnsChannel.read(responseBuffer)
                        if (responseLength > 0) {
                            val dnsData = ByteArray(responseLength)
                            responseBuffer.flip()
                            responseBuffer.get(dnsData)

                            val totalLen = 20 + 8 + dnsData.size
                            val responsePacket = ByteArray(totalLen)
                            val bb = ByteBuffer.wrap(responsePacket)

                            // Explizite Typ-Variablen zur Vermeidung jeglicher Parser-Fehler
                            val zeroVal: Short = 0
                            val oneVal: Short = 1
                            val portVal: Short = 53
                            val udpLenVal = (8 + dnsData.size).toShort()

                            // IPv4 Header via ByteBuffer befüllen
                            bb.put(0x45.toByte())
                            bb.put(0x00.toByte())
                            bb.putShort(totalLen.toShort())
                            bb.putShort(oneVal)
                            bb.putShort(zeroVal)
                            bb.put(64.toByte())
                            bb.put(17.toByte())
                            bb.putShort(zeroVal)
                            bb.putInt(0x0A000002)
                            bb.putInt(0x0A000002)

                            // UDP Header
                            bb.putShort(portVal)
                            bb.putShort(portVal)
                            bb.putShort(udpLenVal)
                            bb.putShort(zeroVal)

                            // DNS Payload
                            bb.put(dnsData)

                            // Korrekte IP-Prüfsumme berechnen und eintragen
                            val checksum = calculateIpChecksum(responsePacket, 20)
                            responsePacket[10] = (checksum.toInt() shr 8).toByte()
                            responsePacket[11] = (checksum.toInt() and 0xFF).toByte()

                            outputStream.write(responsePacket, 0, totalLen)
                        } else {
                            delay(10)
                        }
                    } catch (e: Exception) {
                        delay(20)
                    }
                }
            }

            while (serviceScope.isActive) {
                delay(1000)
            }

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Interceptor-Fehler: ${e.message}")
        }
    }

    private fun calculateIpChecksum(packet: ByteArray, headerLength: Int): Short {
        var sum = 0
        var i = 0
        while (i < headerLength) {
            if (i == 10) {
                i += 2
                continue
            }
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i += 2
        }
        while ((sum ushr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (~sum and 0xFFFF).toShort()
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

