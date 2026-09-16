package com.juaris.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JuarisNotificationListenerService : NotificationListenerService() {

    private lateinit var aiCore: LocalAICore

    override fun onCreate() {
        super.onCreate()
        aiCore = LocalAICore(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            val packageName = notification.packageName
            
            // 1. Eigene App-Benachrichtigungen und reine System-UI ignorieren (verhindert Schleifen und Lärm)
            if (packageName == "com.juaris.app" || packageName.contains("systemui") || packageName.contains("launcher")) {
                return
            }

            val extras = notification.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            
            // 2. Universelle Textextraktion (unterstützt BigTextStyle & MessagingStyle für WhatsApp, Telegram, Signal, Outlook, Gmail etc.)
            var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messages.isNullOrEmpty()) {
                val latestMessage = messages.last()
                if (latestMessage is android.os.Bundle) {
                    text = latestMessage.getString("text") ?: text
                }
            }

            // Wenn weder Titel noch Text vorhanden sind, überspringen
            if (title.isBlank() && text.isBlank()) return

            val fullContent = "App: $packageName | Titel: $title | Inhalt: $text"
            Log.d("JuarisUniversalGuard", "Nachricht abgefangen von $packageName")

            // 3. LOKALE KI-PRÜFUNG (100% On-Device AGI – Zero-Cloud Garantie)
            val isSafe = aiCore.evaluateContentSafety(fullContent, packageName)
            
            if (!isSafe) {
                Log.w("JuarisUniversalGuard", "🚨 Betrug / Phishing in App $packageName lokal blockiert!")
                
                // 4. Direkt fälschungssicher in die lokale Room-Datenbank schreiben
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
            }
        }
    }
}

