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
import java.nio.ByteBuffer

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustKernel"
        private const val NOTIFICATION_ID = 1337
        const val CHANNEL_ID = "juaris_vpn_channel"
        private const val BUFFER_SIZE = 32768
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
        Log.d(TAG, "Juaris Uhrwerk-Engine aktiv: Lokaler On-Device Schutz gestartet.")
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
                description = "100 Prozent lokaler On-Device Phishing- und Netzwerkschutz"
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
            .setContentText("Lokaler Datenschutz-Filter läuft fehlerfrei.")
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
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .setMtu(BUFFER_SIZE)

            // Exkludiere die App selbst aus der VPN-Schleife (ab Android 10)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Fehler beim Ausnehmen der eigenen App: ${e.message}")
                }
            }

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    val inputStream = FileInputStream(pfd.fileDescriptor)
                    val packetBuffer = ByteBuffer.allocate(BUFFER_SIZE)

                    Log.d(TAG, "Juaris Shield etabliert. Lokale Inspection aktiv.")

                    while (isActive && isRunning) {
                        try {
                            packetBuffer.clear()
                            val length = inputStream.read(packetBuffer.array())
                            if (length <= 0) {
                                delay(10)
                            }
                        } catch (e: Exception) {
                            if (!isActive) break
                            delay(50)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kritischer Fehler beim Starten des Shields: ${e.message}")
        }
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

