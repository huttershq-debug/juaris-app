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
import kotlinx.coroutines.launch

class JuarisNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "JuarisOmniGuard"
        const val SERVICE_CHANNEL_ID = "JuarisLiveProtectionChannel"
        private const val ALERT_CHANNEL_ID = "juaris_threat_alerts_v3"
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
        Log.d(TAG, "Juaris Omni-Wächter (Maximaler App- & E-Mail-Scan) gestartet.")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🟢 OMNI-WÄCHTER VERBUNDEN: NotificationListenerService aktiv!")
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
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
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
        try {
            val intent = Intent(applicationContext, EmergencyActivity::class.java).apply {
                putExtra("reason", reason)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der EmergencyActivity", e)
        }
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

            // 🚫 ULTRA-STRIKTER SPAM- & WERBEFILTER (Blockiert Marketing, Social-Media-Müll, Shopping etc.)
            val adKeywords = listOf(
                "gewinn", "gutschein", "casino", "krypto", "bitcoin", "gratis", "rabatt", "deal",
                "newsletter", "cashback", "lotterie", "gewonnen", "sale", "voucher", "promo",
                "followers", "likes", "tiktok", "instagram", "snapchat", "netflix", "prime day",
                "prozent", "spar-", "empfehlung", "code:", "rabattcode", "lieferung unterwegs",
                "cashback", "sonderangebot", "werbung"
            )
            if (adKeywords.any { contentLower.contains(it) }) {
                return // Ignoriert Werbung und Spam vollständig
            }

            // 1. KI-Sicherheitsprüfung (Erkennt Phishing, Betrug, Angriffe)
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

                showThreatScreenAlert(
                    applicationContext,
                    "⚠️ Juaris Sicherheits-Warnung!",
                    "Betrugsversuch in ${packageName.substringAfterLast('.')} erkannt: $title"
                )
            } else {
                // 2. MAXIMALE E-MAIL-, TERMIN- & VERSORGER-ANALYSE (GMX, Gmail, Outlook, Banking, Kalender etc.)
                when {
                    // Finanzen, Rechnungen & Mahnungen (Rot / WARNING)
                    contentLower.contains("mahnung") || contentLower.contains("inkasso") || contentLower.contains("zahlungsaufforderung") || contentLower.contains("letzte frist") -> {
                        showPriorityPopup("🚨 WICHTIGE MAHNUNG", combinedContent.take(120), "finance_high")
                        saveNotificationToDb("Finanz-Wächter", "🚨 Mahnung & Zahlungsfrist", title, combinedContent, "WARNING")
                    }
                    contentLower.contains("überweisung") || contentLower.contains("zahlung") || contentLower.contains("rechnung") || contentLower.contains("lastschrift") || contentLower.contains("bescheid") || contentLower.contains("konto") -> {
                        showPriorityPopup("💳 Rechnungs- & Zahlungs-Hinweis", combinedContent.take(120), "finance_info")
                        saveNotificationToDb("Finanz-Wächter", "💳 Rechnung / Zahlung", title, combinedContent, "WARNING")
                    }

                    // Versorger, Gas, Strom, Wasser, Zähler, Ausbau, Handwerker, Termine, E-Mails (Gold / IMPORTANT)
                    contentLower.contains("gas") || contentLower.contains("strom") || contentLower.contains("wasser") || 
                    contentLower.contains("zähler") || contentLower.contains("ausbau") || contentLower.contains("ablesung") || 
                    contentLower.contains("wartung") || contentLower.contains("handwerker") || contentLower.contains("installateur") || 
                    contentLower.contains("termin") || contentLower.contains("arzt") || contentLower.contains("klinik") ||
                    contentLower.contains("geburtstag") || contentLower.contains("hochzeitstag") ||
                    packageName.contains("gmx") || packageName.contains("mail") || packageName.contains("outlook") || packageName.contains("gmail") -> {
                        
                        val alertHeading = when {
                            contentLower.contains("gas") || contentLower.contains("strom") || contentLower.contains("wasser") || contentLower.contains("zähler") || contentLower.contains("ausbau") -> "🔧 Versorger- & Zähler-Termin"
                            contentLower.contains("arzt") || contentLower.contains("klinik") || contentLower.contains("therapie") -> "🩺 Gesundheit & Arzt-Termin"
                            contentLower.contains("geburtstag") -> "🎂 Geburtstag heute!"
                            contentLower.contains("hochzeitstag") || contentLower.contains("jahrestag") -> "💍 Wichtiger Jahrestag!"
                            else -> "📅 Wichtige Nachricht / E-Mail"
                        }

                        showPriorityPopup(alertHeading, combinedContent.take(120), "service_alert")
                        saveNotificationToDb("Life-Companion", alertHeading, title, combinedContent, "IMPORTANT")
                    }
                }
            }
        }
    }

    private fun saveNotificationToDb(moduleName: String, description: String, title: String, text: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = JuarisDatabase.getDatabase(applicationContext)
                db.securityLogDao().insertLog(
                    SecurityLogEntity(
                        timestamp = System.currentTimeMillis(),
                        module = moduleName,
                        description = description,
                        status = status,
                        details = "Titel: $title | Inhalt: $text"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Speichern in die DB", e)
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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alertNotification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), alertNotification)
    }

    private fun showPriorityPopup(title: String, message: String, category: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
       
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Juaris Notfall- & Prioritätswarnungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wichtige Termine, Mahnungen und Sicherheitsalarme"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val priorityNotification = NotificationCompat.Builder(applicationContext, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), priorityNotification)
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


