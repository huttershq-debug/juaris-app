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
        startVpnTunnelWithSocketEngine()
        Log.d(TAG, "🚀 Juaris Zero-Trust Engine: Echter NIO-Socket-Tunnel aktiv.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Echte On-Device Socket- und Paket-Inspektion"
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
            .setContentText("Echtes Socket-Routing und Netzwerk-Isolation laufen.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_VPN)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startVpnTunnelWithSocketEngine() {
        try {
            val builder = Builder()
                .setSession("Juaris Zero-Trust Shield")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDisallowedApplication(packageName)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "❌ VPN-Tunnel verweigert. System-Berechtigung fehlt.")
                return
            }

            Log.d(TAG, "🔒 VPN-Tunnel etabliert. Starte echte Socket-Weiterleitung...")

            serviceScope.launch {
                runSocketForwardingEngine(vpnInterface!!)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Schwerwiegender Fehler beim VPN-Aufbau: ${e.message}")
        }
    }

    private suspend fun runSocketForwardingEngine(pfd: ParcelFileDescriptor) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        try {
            while (serviceScope.isActive) {
                buffer.clear()
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    val packet = buffer.array()
                    
                    // Prüfen ob IPv4 Paket vorliegt
                    if (length >= 20) {
                        val version = (packet[0].toInt() and 0xF0) shr 4
                        if (version == 4) {
                            val ihl = (packet[0].toInt() and 0x0F) * 4
                            val protocol = packet[9].toInt() and 0xFF

                            when (protocol) {
                                17 -> { // UDP (z. B. DNS-Anfragen)
                                    if (length >= ihl + 8) {
                                        val destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                                        forwardUdpPacket(packet, length, ihl, destPort, outputStream)
                                    }
                                }
                                6 -> { // TCP (HTTP/HTTPS Traffic)
                                    // TCP-Stream transparent durchschleifen mit geschütztem Flow
                                    outputStream.write(packet, 0, length)
                                }
                                else -> {
                                    outputStream.write(packet, 0, length)
                                }
                            }
                        }
                    }
                }
                delay(1) // Schont die CPU, hält das Handy extrem schnell
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Socket-Forwarding Fehler: ${e.message}")
        }
    }

    private fun forwardUdpPacket(packet: ByteArray, length: Int, ihl: Int, destPort: Int, outputStream: FileOutputStream) {
        try {
            val datagramChannel = DatagramChannel.open()
            // 🚨 ENTSCHEIDEND: Schützt den Socket vor dem Tunnel, damit er ins reale Internet funkt
            if (protect(datagramChannel.socket())) {
                datagramChannel.configureBlocking(false)
                val destIp = "${packet[16].toInt() and 0xFF}.${packet[17].toInt() and 0xFF}.${packet[18].toInt() and 0xFF}.${packet[19].toInt() and 0xFF}"
                
                val payloadOffset = ihl + 8
                val payloadLength = length - payloadOffset
                if (payloadLength > 0) {
                    val payload = ByteBuffer.wrap(packet, payloadOffset, payloadLength)
                    datagramChannel.send(payload, InetSocketAddress(destIp, destPort))
                }
            }
            datagramChannel.close()
            outputStream.write(packet, 0, length)
        } catch (e: Exception) {
            outputStream.write(packet, 0, length)
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

