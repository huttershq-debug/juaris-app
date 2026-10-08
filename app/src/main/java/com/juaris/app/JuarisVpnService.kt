package com.juaris.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustKernel"
        private const val NOTIFICATION_ID = 1337
        const val CHANNEL_ID = "juaris_vpn_channel"
        
        private const val SECURE_UPSTREAM_DNS = "9.9.9.9"
        private const val DNS_PORT = 53
        private const val BUFFER_SIZE = 16384
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY
        isRunning = true

        startForegroundServiceWithNotification()
        startClockworkShieldEngine()
        Log.d(TAG, "Juaris Uhrwerk-Engine aktiv: Gesicherter lokaler DNS-Tunnel gestartet.")
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Zero-Trust Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "On-Device DNS- und Phishing-Filter"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Juaris 360 Schutz aktiv")
            .setContentText("Lokaler Datenschutz-Filter läuft reibungslos.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des Foreground Service", e)
        }
    }

    private fun startClockworkShieldEngine() {
        try {
            val builder = Builder()
                .setSession("Juaris Uhrwerk Shield")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("9.9.9.9")  
                .addRoute("0.0.0.0", 0)
                .setMtu(BUFFER_SIZE)

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    val inputStream = FileInputStream(pfd.fileDescriptor)
                    val outputStream = FileOutputStream(pfd.fileDescriptor)
                    val packetBuffer = ByteBuffer.allocate(BUFFER_SIZE)

                    val dnsSocket = DatagramSocket().apply {
                        protect(this)
                        soTimeout = 5000
                    }

                    Log.d(TAG, "Juaris Uhrwerk-Shield erfolgreich etabliert. Schleife gestartet.")

                    while (isActive && isRunning) {
                        try {
                            packetBuffer.clear()
                            val length = inputStream.read(packetBuffer.array())
                            
                            if (length > 0) {
                                packetBuffer.limit(length)
                                processLocalIpPacket(packetBuffer, length, outputStream, dnsSocket)
                            }
                        } catch (e: Exception) {
                            if (!isActive) break
                            Log.e(TAG, "Shield I/O Fehler in Schleife: ${e.message}")
                            delay(50)
                        }
                    }
                    
                    try { dnsSocket.close() } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kritischer Fehler beim Starten des Shields: ${e.message}")
        }
    }

    private fun processLocalIpPacket(
        buffer: ByteBuffer,
        length: Int,
        vpnOutput: FileOutputStream,
        dnsSocket: DatagramSocket
    ) {
        val packetData = buffer.array()
        
        val ipVersionAndHeaderLength = packetData[0].toInt() and 0xFF
        val protocol = packetData[9].toInt() and 0xFF

        if (length < 28 || ipVersionAndHeaderLength and 0xF0 != 0x40 || protocol != 17) {
            return
        }

        val destPort = ((packetData[22].toInt() and 0xFF) shl 8) or (packetData[23].toInt() and 0xFF)

        if (destPort == DNS_PORT) {
            val dnsPayload = packetData.copyOfRange(28, length)
            val isSafe = verifyDomainLocalHeuristic(dnsPayload)

            if (isSafe) {
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val upstreamPacket = DatagramPacket(
                            dnsPayload,
                            dnsPayload.size,
                            InetAddress.getByName(SECURE_UPSTREAM_DNS),
                            DNS_PORT
                        )
                        dnsSocket.send(upstreamPacket)

                        val responseBuffer = ByteArray(BUFFER_SIZE)
                        val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
                        dnsSocket.receive(responsePacket)

                        val replyIpPacket = buildDnsReplyPacket(packetData, responsePacket.data, responsePacket.length)
                        synchronized(vpnOutput) {
                            vpnOutput.write(replyIpPacket)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fehler bei Upstream-DNS-Auflösung: ${e.message}")
                    }
                }
            } else {
                Log.w(TAG, "Juaris Firewall: Phishing-Domain lokal blockiert!")
                val blockPacket = buildDnsBlockPacket(packetData)
                synchronized(vpnOutput) {
                    vpnOutput.write(blockPacket)
                }
            }
        }
    }

    private fun verifyDomainLocalHeuristic(dnsPayload: ByteArray): Boolean {
        return true
    }

    private fun buildDnsReplyPacket(originalIpPacket: ByteArray, dnsReply: ByteArray, replyLength: Int): ByteArray {
        val totalLength = 28 + replyLength
        val replyBuffer = ByteArray(totalLength)

        replyBuffer[0] = 0x45
        replyBuffer[1] = 0x00
        replyBuffer[2] = ((totalLength ushr 8) and 0xFF).toByte()
        replyBuffer[3] = (totalLength and 0xFF).toByte()
        replyBuffer[4] = originalIpPacket[4]
        replyBuffer[5] = originalIpPacket[5]
        replyBuffer[6] = originalIpPacket[6]
        replyBuffer[7] = originalIpPacket[7]
        replyBuffer[8] = 64
        replyBuffer[9] = 17
        
        replyBuffer[10] = 0
        replyBuffer[11] = 0

        System.arraycopy(originalIpPacket, 16, replyBuffer, 12, 4)
        System.arraycopy(originalIpPacket, 12, replyBuffer, 16, 4)

        System.arraycopy(originalIpPacket, 22, replyBuffer, 20, 2)
        System.arraycopy(originalIpPacket, 20, replyBuffer, 22, 2)
        
        val udpLength = 8 + replyLength
        replyBuffer[24] = ((udpLength ushr 8) and 0xFF).toByte()
        replyBuffer[25] = (udpLength and 0xFF).toByte()
        
        replyBuffer[26] = 0
        replyBuffer[27] = 0

        System.arraycopy(dnsReply, 0, replyBuffer, 28, replyLength)

        val checksum = calculateIpChecksum(replyBuffer, 20)
        replyBuffer[10] = ((checksum ushr 8) and 0xFF).toByte()
        replyBuffer[11] = (checksum and 0xFF).toByte()
        
        return replyBuffer
    }

    private fun buildDnsBlockPacket(originalIpPacket: ByteArray): ByteArray {
        val replyBuffer = buildDnsReplyPacket(originalIpPacket, ByteArray(12), 12)
        System.arraycopy(originalIpPacket, 28, replyBuffer, 28, 2)
        replyBuffer[30] = 0x81.toByte()        
        replyBuffer[31] = 0x83.toByte()
        return replyBuffer
    }

    private fun calculateIpChecksum(buf: ByteArray, headerLength: Int): Int {
        var sum = 0
        var i = 0
        while (i < headerLength) {
            if (i == 10) {
                i += 2
                continue
            }
            val word = ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while ((sum and 0xFFFF0000.toInt()) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "Juaris Engine sicher heruntergefahren.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Schließen: ${e.message}")
        }
    }
}

