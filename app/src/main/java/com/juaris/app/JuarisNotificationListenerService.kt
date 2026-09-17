package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JuarisNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "JuarisUniversalGuard"
        const val SERVICE_CHANNEL_ID = "JuarisLiveProtectionChannel"
        private const val ALERT_CHANNEL_ID = "juaris_threat_alerts"
        const val NOTIFICATION_ID = 1337
    }

    private lateinit var aiCore: LocalAICore

    override fun onCreate() {
        super.onCreate()
        aiCore = LocalAICore(applicationContext)
        startForegroundServiceWithNotification()
        Log.d(TAG, "Juaris 24/7 Universal-Wächter gestartet.")
    }

     override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🟢 SYSTEM ERFOLGREICH VERBUNDEN: NotificationListenerService ist aktiv!")
        
        // Das zeigt dir den Erfolg direkt als Pop-up auf dem Handy-Bildschirm:
        android.widget.Toast.makeText(
            this, 
            "🟢 Juaris: Benachrichtigungs-Zugriff verbunden!", 
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Juaris Live-Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hält den Zero-Cloud Schutz aktiv und überwacht Benachrichtigungen"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Juaris Security Suite aktiv")
            .setContentText("24/7 Live-Schutz & Lokale KI-Heuristik laufen")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

        // KRITISCH FÜR ANDROID 14+: Übergabe des Foreground-Service-Typs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14 (API 34+)
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            val packageName = notification.packageName

            // Eigene App und System-UI/Launcher ignorieren, um Endlosschleifen zu vermeiden
            if (packageName == "com.juaris.app" || packageName.contains("systemui") || packageName.contains("launcher")) {
                return
            }

            val extras = notification.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Erweiterte Extraktion für Messenger (WhatsApp, Telegram etc.)
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messages.isNullOrEmpty()) {
                val latestMessage = messages.last()
                if (latestMessage is android.os.Bundle) {
                    text = latestMessage.getString("text") ?: text
                }
            }

            if (title.isBlank() && text.isBlank()) return

            val fullContent = "App: $packageName | Titel: $title | Inhalt: $text"
            Log.d(TAG, "Nachricht abgefangen von $packageName")

            // Lokale KI-Prüfung auf Betrug / Phishing
            val isSafe = aiCore.evaluateContentSafety(fullContent, packageName)

            if (!isSafe) {
                Log.w(TAG, "⚠️ Betrug / Phishing in App $packageName lokal blockiert!")

                // In lokale Room-Datenbank schreiben
                CoroutineScope(Dispatchers.IO).launch {
                    val db = JuarisDatabase.getDatabase(applicationContext)
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "360°-Universal-Wächter",
                            description = "Bedrohung in [${packageName.substringAfterLast('.')}] abgefangen: $title",
                            status = "BLOCKED"
                        )
                    )
                }

                // Notfall-Alarm direkt auf den Bildschirm werfen
                showThreatScreenAlert(
                    applicationContext,
                    "⚠️ Juaris Sicherheits-Warnung!",
                    "Betrugsversuch in ${packageName.substringAfterLast('.')} erkannt: $title"
                )
            }
        }
    }

    private fun showThreatScreenAlert(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Juaris Notfall-Sicherheitswarnungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warnungen bei akuten Phishing- und Betrugsversuchen"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alertNotification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.hologram_avatar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), alertNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}

