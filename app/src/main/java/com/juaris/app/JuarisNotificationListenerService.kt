package com.juaris.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class JuarisNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "JuarisOmniGuard"
        const val SERVICE_CHANNEL_ID = "JuarisLiveProtectionChannel"
        const val NOTIFICATION_ID = 1337
    }

    private lateinit var aiCore: LocalAICore
    private lateinit var acousticDetector: AcousticThreatDetector
    
    // Kontrollierter Supervisor-Scope verhindert Datenverluste in der Room-DB
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        Log.d(TAG, "Juaris Omni-Wächter (Maximaler App- & E-Mail-Scan) initialisiert.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🟢 OMNI-WÄCHTER VERBUNDEN: NotificationListenerService aktiv!")

        // CRITICAL FIX 1: Der VPN-Autostart wurde in 'onListenerConnected' verschoben!
        // Das verhindert die gefürchtete 'ForegroundServiceStartNotAllowedException' ab Android 14/15 vollständig.
        try {
            val vpnIntent = Intent(applicationContext, JuarisVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(vpnIntent)
            } else {
                startService(vpnIntent)
            }
            Log.d(TAG, "🔌 JuarisVpnService-Firewall erfolgreich nachgezogen.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ VPN-Autostart fehlgeschlagen/verzögert: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "🔴 OMNI-WÄCHTER GETRENNT durch Android!")
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "Juaris Omni-Schutz",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Hält die absolute 24/7 Geräteschutz-Engine aktiv"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Juaris Security Suite aktiv")
            .setContentText("Omni-Wächter scannt alle Apps, E-Mails & Fristen in Echtzeit")
            .setSmallIcon(R.drawable.app_icon)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
           
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
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
        // CRITICAL FIX 2: Direkter startActivity-Aufruf aus dem Hintergrund entfernt!
        // Wir nutzen die offizielle 'triggerEmergencyAlarm'-Methode, die über FullScreenIntent
        // sicher die Android 14/15 Hintergrund-Blockaden durchbricht.
        EmergencyActivity.triggerEmergencyAlarm(applicationContext, reason)
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
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val combinedContent = "$title $text $bigText"
            if (combinedContent.isBlank()) return

            val fullContentString = "App: $packageName | Titel: $title | Inhalt: $combinedContent"
            val contentLower = fullContentString.lowercase()

            // 🚫 ULTRA-STRIKTER SPAM- & WERBEFILTER
            val adKeywords = listOf(
                "gewinn", "gutschein", "casino", "krypto", "bitcoin", "gratis", "rabatt", "deal",
                "newsletter", "cashback", "lotterie", "gewonnen", "sale", "voucher", "promo",
                "followers", "likes", "tiktok", "instagram", "snapchat", "netflix", "prime day",
                "prozent", "spar-", "empfehlung", "code:", "rabattcode", "lieferung unterwegs",
                "cashback", "sonderangebot", "werbung"
            )
            if (adKeywords.any { contentLower.contains(it) }) {
                return 
            }

            val isSafe = aiCore.evaluateContentSafety(fullContentString, packageName)

            if (!isSafe) {
                try {
                    notification.key?.let { cancelNotification(it) }
                } catch (e: Exception) {}

                saveNotificationToDb(
                    "360°-Omni-Wächter",
                    "Betrug in [${packageName.substringAfterLast('.')}] abgefangen: $title",
                    title,
                    combinedContent,
                    "BLOCKED"
                )

                // Nutzt den optimierten JuarisNotificationDispatcher für saubere Kanäle!
                JuarisNotificationDispatcher.sendPriorityAlert(
                    applicationContext,
                    "⚠️ Juaris Sicherheits-Warnung!",
                    "Betrugsversuch in ${packageName.substringAfterLast('.')} erkannt: $title",
                    isCritical = true
                )
            } else {
                // KORREKTUR 3: Die abgebrochene Logik sauber vollendet und abgesichert!
                when {
                    contentLower.contains("mahnung") || contentLower.contains("inkasso") || contentLower.contains("zahlungsaufforderung") || contentLower.contains("letzte frist") -> {
                        JuarisNotificationDispatcher.sendPriorityAlert(applicationContext, "🚨 WICHTIGE MAHNUNG", combinedContent.take(120), isCritical = true)
                        saveNotificationToDb("Finanz-Wächter", "🚨 Mahnung & Zahlungsfrist", title, combinedContent, "WARNING")
                    }
                    contentLower.contains("überweisung") || contentLower.contains("zahlung") || contentLower.contains("rechnung") || contentLower.contains("kontoänderung") -> {
                        JuarisNotificationDispatcher.sendPriorityAlert(applicationContext, "💳 Finanztransaktion erkannt", "Rechnungs- oder Zahlungsdaten lokal erfasst.", isCritical = false)
                        saveNotificationToDb("Finanz-Wächter", "💳 Zahlung / Rechnung", title, combinedContent, "INFO")
                    }
                }
            }
        }
    }

    private fun saveNotificationToDb(module: String, description: String, title: String, details: String, status: String) {
        serviceScope.launch {
            try {
                val db = JuarisDatabase.getDatabase(applicationContext)
                db.securityLogDao().insertLog(
                    SecurityLogEntity(
                        timestamp = System.currentTimeMillis(),
                        status = status,
                        module = module,
                        description = description,
                        details = "Titel: $title | Details: $details"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Schreiben in die Krypto-DB: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            acousticDetector.stopListening()
        } catch (e: Exception) {}
        serviceScope.cancel() // Verhindert offene Hintergrund-Tasks & Memory Leaks beim Beenden
    Log.d(TAG, "🛑 Juaris Omni-Wächter sicher heruntergefahren.")
  }
}
