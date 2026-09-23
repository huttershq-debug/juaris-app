package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
    private lateinit var acousticDetector: AcousticThreatDetector

    override fun onCreate() {
        super.onCreate()
        aiCore = LocalAICore(applicationContext)
       
        try {
            acousticDetector = AcousticThreatDetector(applicationContext) {
                triggerEmergencyProtocol("Akustischer Notfall (Schrei/Gewalt) erkannt!")
            }
            acousticDetector.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des akustischen Wächters", e)
        }

        startForegroundServiceWithNotification()
        Log.d(TAG, "Juaris 24/7 Universal-Wächter & Mikrofon-Bodyguard gestartet.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🟢 SYSTEM ERFOLGREICH VERBUNDEN: NotificationListenerService ist aktiv!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "🔴 SYSTEM GETRENNT: Android hat den NotificationListenerService abgewiesen!")
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Juaris Live-Schutz",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hält den Zero-Cloud Schutz & Mikrofon-Wächter aktiv"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Juaris Security Suite aktiv")
            .setContentText("24/7 Live-Schutz, Mikrofon- & KI-Wächter aktiv")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten des Foreground Service", e)
        }
    }

    private fun triggerEmergencyProtocol(reason: String) {
        Log.w(TAG, "🚨 NOTFALL-PROTOKOLL AUSGELÖST: $reason")
        val intent = Intent("com.juaris.app.ACTION_EMERGENCY_TRIGGER").apply {
            putExtra("reason", reason)
            setPackage("com.juaris.app")
        }
        sendBroadcast(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            val packageName = notification.packageName
            if (packageName == "com.juaris.app" || packageName.contains("systemui") || packageName.contains("launcher")) {
                return
            }

            val extras = notification.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (title.isBlank() && text.isBlank()) return

            val fullContent = "App: $packageName | Titel: $title | Inhalt: $text"
            val isSafe = aiCore.evaluateContentSafety(fullContent, packageName)

            if (!isSafe) {
                try {
                    notification.key?.let { cancelNotification(it) }
                } catch (e: Exception) {}

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = JuarisDatabase.getDatabase(applicationContext)
                        db.securityLogDao().insertLog(
                            SecurityLogEntity(
                                timestamp = System.currentTimeMillis(),
                                module = "360°-Universal-Wächter",
                                description = "Betrug in [${packageName.substringAfterLast('.')}] abgefangen: $title",
                                status = "BLOCKED"
                            )
                        )
                    } catch (e: Exception) {}
                }
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
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), alertNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            acousticDetector.stopListening()
        } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}

