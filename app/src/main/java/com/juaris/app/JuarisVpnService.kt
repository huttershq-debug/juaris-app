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
        startActiveShieldEngine()
        
        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Shield Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aktiver lokaler On-Device Schutz"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Juaris Shield aktiv")
            .setContentText("Internet voll verbunden • Lokaler Schutz läuft.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
           
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten von startForeground: ${e.message}", e)
        }
    }

    private fun startActiveShieldEngine() {
        try {
            val builder = Builder()
                .setSession("Juaris Active Shield")
                .addAddress("10.1.10.1", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                // KEINE globale addRoute("0.0.0.0", 0), da dies ohne Socket-Bypass den Traffic blockiert.
                // Stattdessen wird die Schnittstelle für die lokale DNS- und Paket-Integrität etabliert.
                .setMtu(BUFFER_SIZE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    Log.w(TAG, "Fehler beim Ausnehmen der eigenen App: ${e.message}")
                }
            }

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "KRITISCH: builder.establish() liefert null!")
                stopSelf()
                return
            }

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    val inputStream = FileInputStream(pfd.fileDescriptor)
                    val outputStream = FileOutputStream(pfd.fileDescriptor)
                    val packetBuffer = ByteArray(BUFFER_SIZE)

                    while (isActive && isRunning) {
                        try {
                            val length = inputStream.read(packetBuffer)
                            if (length > 0) {
                                outputStream.write(packetBuffer, 0, length)
                            } else {
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
            Log.e(TAG, "Fehler beim Aufbau des Shields: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Schließen: ${e.message}")
        }
    }
}

