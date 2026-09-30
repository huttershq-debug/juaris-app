package com.juaris.app.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.juaris.app.JuarisDatabase
import com.juaris.app.SecurityEngine
import com.juaris.app.EmailSecurityResult
import com.juaris.app.R
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Umgestellt auf CoroutineWorker für lückenlose, absturzsichere Hintergrund-Scans!
class EmailScanWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "juaris_important_alerts"

        private val IMPORTANT_EMAIL_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "rechtsanwalt"
        )
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val context = applicationContext
                
                // Nutze den unverschlüsselten Modus für globale Flags, um kryptografische Abstürze zu verhindern
                val prefs = context.getSharedPreferences("juaris_public_prefs", Context.MODE_PRIVATE)
                if (!prefs.getBoolean("email_prot", true)) {
                    return@withContext Result.success()
                }

                val sender = inputData.getString("sender") ?: "unbekannt"
                val subject = inputData.getString("subject") ?: ""
                val body = inputData.getString("body") ?: ""
                val db = JuarisDatabase.getDatabase(context)
                val securityEngine = SecurityEngine(context)

                val fullEmailContent = "From: $sender \nSubject: $subject \nBody: $body"
                val result = securityEngine.analyzeIncomingEmail(fullEmailContent)

                if (result == EmailSecurityResult.BLOCK) {
                    // Durch suspend und IO-Context wird der Log JETZT sicher geschrieben, bevor das System terminiert!
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "BLOCKED",
                            module = "E-Mail-Heuristik",
                            description = "Phishing-Mail blockiert von Absender: $sender",
                            details = "Betreff: $subject"
                        )
                    )
                    return@withContext Result.failure()
                }

                val combinedText = "$subject $body".lowercase()
                val isImportant = IMPORTANT_EMAIL_PATTERNS.any { combinedText.contains(it) }

                if (isImportant) {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "IMPORTANT",
                            module = "E-Mail-Fristen-Wächter",
                            description = "Wichtiges Dokument / Frist erkannt",
                            details = "Absender: $sender | Betreff: $subject"
                        )
                    )

                    showImportantEmailNotification(
                        context,
                        "Wichtiges Dokument / Frist entdeckt",
                        "Betreff: $subject\nAbsender: $sender"
                    )
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun showImportantEmailNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Wichtige Fristen & Dokumente",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen für Rechnungen, Inkasso, Gerichte und Fristen"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon) // KORREKTUR: Nutzt dein offizielles, transparentes Vektor-Icon!
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}

