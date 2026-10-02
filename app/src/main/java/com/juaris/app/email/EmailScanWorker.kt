package com.juaris.app.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.juaris.app.JuarisDatabase
import com.juaris.app.SecurityEngine
import com.juaris.app.R
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * JUARIS EMAIL SCAN WORKER (Zero-Cloud Kernel)
 * Analysiert im Hintergrund eintreffende E-Mail-Inhalte vollkommen offline
 * auf betrügerische Manipulationstaktiken (Phishing) und wichtige Fristen.
 */
class EmailScanWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "JuarisEmailWorker"
        private const val CHANNEL_ID = "juaris_important_alerts"

        // Multilinguale, lokale Phishing- und Dokumenten-Indikatoren
        private val IMPORTANT_EMAIL_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "rechtsanwalt"
        )
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val context = applicationContext
                
                // Nutze das unverschlüsselte SharedPreferences-Flag für den schnellen Hintergrund-Zugriff
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
                
                // KORREKTUR 1: Aufruf an das harmonisierte 'analyzeText'-Verfahren der SecurityEngine gekoppelt!
                // Eliminiert den 'Unresolved reference'-Kompilierabsturz restlos.
                val result = securityEngine.analyzeText(fullEmailContent)

                if (result is SecurityEngine.ThreatResult.Blocked) {
                    // Durch suspend und den IO-Context wird das Log JETZT atomar geschrieben, bevor der Task terminiert!
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = "BLOCKED",
                            module = "E-Mail-Heuristik",
                            description = "Phishing-Mail blockiert von Absender: $sender",
                            details = "Betreff: $subject | Grund: ${result.reason}"
                        )
                    )
                    return@withContext Result.failure()
                }

                val combinedText = "$subject $body".lowercase(Locale.ROOT)
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
                // KORREKTUR 2: 'e.printStackTrace()' restlos gelöscht! 
                // Verhindert das Auslesen interner Speicher-Traces aus dem unverschlüsselten OS-Logcat.
                Log.e(TAG, "Kritischer Fehler im E-Mail-Hintergrund-Scanner: ${e.message}")
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

        // KORREKTUR 3: Eindeutige, dynamische ID-Generierung über den Inhalts-Hash verhindert, 
        // dass sich wichtige parallele E-Mail-Fristen im Benachrichtigungs-Menü gegenseitig löschen!
        val uniqueNotificationId = (title + message).hashCode()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(uniqueNotificationId, notification)
    }
}
