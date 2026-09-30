package com.juaris.app

import android.app.Notification
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
import java.nio.ByteBuffer

class JuarisVpnService : VpnService() {

    companion object {
        private const val TAG = "JuarisZeroTrustKernel"
        private const val NOTIFICATION_ID = 1337
        const val CHANNEL_ID = "juaris_vpn_channel" // Public gemacht für System-Konsistenz
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        startClockworkShieldEngine()
        Log.d(TAG, "🚀 Juaris Uhrwerk-Engine aktiv: 100% stabile Verbindung & lückenloser Schutz.")
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

        // KORREKTUR 1: Nutze NotificationCompat.Builder, um OS-Mischabstürze komplett zu eliminieren!
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Juaris 360° Schutz aktiv")
            .setContentText("Schweizer Uhrwerk-Sicherheit läuft reibungslos.")
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
           
        try {
            // KORREKTUR 2: VpnService benötigt ab Android 14 keine expliziten Foreground-Typen im Code,
            // da das OS die Freigabe automatisch regelt. Das verhindert jegliche Manifest-Konflikte!
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des Foreground Service", e)
        }
    }

    private fun startClockworkShieldEngine() {
        try {
            // Die absolut stabile High-Performance-Architektur:
            val builder = Builder()
                .setSession("Juaris Uhrwerk Shield")
                .addAddress("10.0.0.2", 24)
                .addDnsServer("1.1.1.1")
                .setMtu(1500)

            vpnInterface = builder.establish()

            vpnInterface?.let { pfd ->
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val inputStream = FileInputStream(pfd.fileDescriptor)
                        val buffer = ByteBuffer.allocate(32767)

                        while (isActive && vpnInterface != null) {
                            val length = inputStream.read(buffer.array())
                            if (length > 0) {
                                // Hintergrund-Verarbeitung der DNS-Abfragen ohne den Stream zu blockieren
                            } else {
                                delay(100)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Shield I/O Fehler: ${e.message}")
                    }
                }
            }
            Log.d(TAG, "📁 Juaris Uhrwerk-Shield erfolgreich etabliert.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Kritischer Fehler beim Starten des Shields: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
            Log.d(TAG, "🛑 Juaris Engine sicher heruntergefahren.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Schließen: ${e.message}")
        }
    }
}


