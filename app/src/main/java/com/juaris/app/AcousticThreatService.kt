package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AcousticThreatService : Service() {

    private lateinit var acousticThreatDetector: AcousticThreatDetector

    companion object {
        private const val CHANNEL_ID = "acoustic_threat_channel"
        private const val NOTIFICATION_ID = 1002
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Startet den Foreground-Service mit Typ Mikrofon gemäß Android 14+ Vorgaben
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Juaris Akustik-Wächter")
            .setContentText("Überwacht die Umgebung auf akustische Bedrohungen...")
            .setSmallIcon(R.mipmap.ic_launcher_foreground) // Passe dies an dein Icon an falls nötig
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Initialisiert und startet den Detektor
        acousticThreatDetector = AcousticThreatDetector(this) {
            // Wird ausgeführt, wenn eine akustische Bedrohung erkannt wird
            triggerEmergencyResponse()
        }
        acousticThreatDetector.startListening()
    }

    private fun triggerEmergencyResponse() {
        // Hier kannst du die Notfall-Logik auslösen (z.B. Broadcast an Mesh oder öffnen der EmergencyActivity)
        val intent = Intent(this, EmergencyActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::acousticThreatDetector.isInitialized) {
            acousticThreatDetector.stopListening()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Akustischer Bedrohungsschutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Dauerhafter Schutz durch lokale Audio-Anomalieerkennung"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}


