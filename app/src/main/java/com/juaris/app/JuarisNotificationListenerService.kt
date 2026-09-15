package com.juaris.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

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
            
            // Bekannte E-Mail-Apps auf Android
            val emailPackages = listOf(
                "com.google.android.gm", // Gmail
                "com.microsoft.office.outlook", // Outlook
                "com.samsung.android.email.provider" // Samsung Mail
            )

            if (emailPackages.contains(packageName)) {
                val extras = notification.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val fullEmailContent = "$title: $text"

                // Lokale KI-Prüfung via LocalAICore (100% On-Device, keine Cloud!)
                val isSafe = aiCore.evaluateContentSafety(fullEmailContent, packageName)
                if (!isSafe) {
                    Log.w("JuarisEmailScan", "Phishing / Betrug in E-Mail-Notification erkannt: $fullEmailContent")
                    
                    // Optional: Direkt als Security-Log in die lokale Room-Datenbank schreiben
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        val db = JuarisDatabase.getDatabase(applicationContext)
                        db.securityLogDao().insertLog(
                            SecurityLogEntity(
                                timestamp = System.currentTimeMillis(),
                                module = "E-Mail-Heuristik",
                                description = "Phishing-Verdacht in E-Mail abgefangen: $title",
                                status = "BLOCKED"
                            )
                        )
                    }
                }
            }
        }
    }
}
