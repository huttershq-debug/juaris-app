package com.juaris.app.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.juaris.app.SecurityEngine
import com.juaris.app.EmailSecurityResult
import com.juaris.app.R

class EmailScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "juaris_important_alerts"

        // Wichtige E-Mail-Schlüsselwörter für Rechnungen, Ämter und Fristen
        private val IMPORTANT_EMAIL_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "rechtsanwalt"
        )
    }

    override fun doWork(): Result {
        return try {
            // Prüfen, ob der E-Mail-Schutz in der App aktiv ist
            val prefs = applicationContext.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("email_prot", true)) {
                return Result.success()
            }

            val sender = inputData.getString("sender") ?: "unbekannt"
            val subject = inputData.getString("subject") ?: ""
            val body = inputData.getString("body") ?: ""

            // 1. Lokaler Offline-Scan über die SecurityEngine (Prüfung auf Phishing/Spam)
            val result = SecurityEngine.analyzeIncomingEmail(sender, subject, body)

            if (result == EmailSecurityResult.BLOCK) {
                // E-Mail ist reiner Spam/Gefahr -> direkt verwerfen
                return Result.failure()
            }

            // 2. Auf wichtige Dokumente / Rechnungen / Fristen prüfen
            val combinedText = "$subject $body".lowercase()
            val isImportant = IMPORTANT_EMAIL_PATTERNS.any { combinedText.contains(it) }

            if (isImportant) {
                // Sofort den Nutzer warnen, damit keine Frist verpasst wird!
                showImportantEmailNotification(
                    applicationContext,
                    "📄 Wichtiges Dokument / Frist entdeckt",
                    "Betreff: $subject\nAbsender: $sender"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
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
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title) // KORREKTUR: setContentTitle statt setTitle
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

