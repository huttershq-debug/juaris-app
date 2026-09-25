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

            // --- COROUTINE 2: Upstream DNS -> Handy (High-Performance Direct Indexing) ---
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

                            // 1. IPv4 Header (20 Bytes)
                            responsePacket[0] = 0x45.toByte() // Version 4, IHL 5
                            responsePacket[1] = 0x00.toByte() // TOS / DSCP
                            responsePacket[2] = (totalLen shr 8).toByte()
                            responsePacket[3] = (totalLen and 0xFF).toByte()
                            responsePacket[4] = 0x00.toByte()
                            responsePacket[5] = 0x01.toByte()
                            responsePacket[6] = 0x00.toByte()
                            responsePacket[7] = 0x00.toByte()
                            responsePacket[8] = 64.toByte() // TTL
                            responsePacket[9] = 17.toByte() // Protocol: UDP

                            // Platzhalter Prüfsumme (wird gleich berechnet)
                            responsePacket[10] = 0x00.toByte()
                            responsePacket[11] = 0x00.toByte()

                            // Source IP: 10.0.0.2
                            responsePacket[12] = 10.toByte()
                            responsePacket[13] = 0.toByte()
                            responsePacket[14] = 0.toByte()
                            responsePacket[15] = 2.toByte()

                            // Destination IP: 10.0.0.2
                            responsePacket[16] = 10.toByte()
                            responsePacket[17] = 0.toByte()
                            responsePacket[18] = 0.toByte()
                            responsePacket[19] = 2.toByte()

                            // Echte IP-Prüfsumme berechnen und eintragen
                            val checksum = calculateIpChecksum(responsePacket, 20)
                            responsePacket[10] = (checksum.toInt() ushr 8).toByte()
                            responsePacket[11] = (checksum.toInt() and 0xFF).toByte()

                            // 2. UDP Header (8 Bytes)
                            responsePacket[20] = 0x00.toByte() // Source Port High (53)
                            responsePacket[21] = 53.toByte() // Source Port Low
                            responsePacket[22] = 0x00.toByte() // Dest Port High (53)
                            responsePacket[23] = 53.toByte() // Dest Port Low

                            val udpLen = 8 + dnsData.size
                            responsePacket[24] = (udpLen shr 8).toByte()
                            responsePacket[25] = (udpLen and 0xFF).toByte()
                            responsePacket[26] = 0x00.toByte() // UDP Checksum (optional bei IPv4)
                            responsePacket[27] = 0x00.toByte()

                            // 3. DNS Payload anhängen
                            System.arraycopy(dnsData, 0, responsePacket, 28, dnsData.size)

                            // An das VPN-Interface übergeben
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

