package com.juaris.app.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.juaris.app.JuarisDatabase
import com.juaris.app.SecurityEngine
import com.juaris.app.EmailSecurityResult
import com.juaris.app.R
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmailScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "juaris_important_alerts"

        private val IMPORTANT_EMAIL_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "rechtsanwalt"
        )
    }

    override fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("email_prot", true)) {
                return Result.success()
            }

            val sender = inputData.getString("sender") ?: "unbekannt"
            val subject = inputData.getString("subject") ?: ""
            val body = inputData.getString("body") ?: ""
            val db = JuarisDatabase.getDatabase(applicationContext)

            val result = SecurityEngine.analyzeIncomingEmail(sender, subject, body)

            if (result == EmailSecurityResult.BLOCK) {
                // Blockierte E-Mail in DB loggen
                CoroutineScope(Dispatchers.IO).launch {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "E-Mail-Heuristik",
                            description = "Phishing-Mail blockiert von $sender",
                            status = "BLOCKED"
                        )
                    )
                }
                return Result.failure()
            }

            val combinedText = "$subject $body".lowercase()
            val isImportant = IMPORTANT_EMAIL_PATTERNS.any { combinedText.contains(it) }

            if (isImportant) {
                // Wichtiges Dokument / Frist in DB loggen
                CoroutineScope(Dispatchers.IO).launch {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "E-Mail-Fristen-Wächter",
                            description = "Wichtiges Dokument/Frist von $sender: $subject",
                            status = "IMPORTANT"
                        )
                    )
                }

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
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

