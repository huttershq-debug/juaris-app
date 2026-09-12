package com.juaris.app.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.juaris.app.R

class SmsFilterReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsFilter"
        private const val CHANNEL_ID = "juaris_important_alerts"

        // Der ganze unnötige Blödsinn & Spam
        private val SPAM_PATTERNS = listOf(
            "paket", "konto gesperrt", "zollgebühr", "klicken sie",
            "verification code", "banking update", "wallet locked", "gewonnen"
        )

        // Kritische Inhalte & Fristen, die sofort gemeldet werden müssen
        private val IMPORTANT_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "termin"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Prüfen, ob der SMS-Schutz in der App überhaupt aktiv ist
        val prefs = context.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_prot", true)) {
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: "Unbekannt"
                val body = message.messageBody ?: ""

                Log.d(TAG, "Eingehende SMS von $sender analysiert.")

                when {
                    isImportantNotice(body) -> {
                        // WICHTIGE Nachricht / Frist erkannt -> Durchlassen & Alarm senden
                        Log.d(TAG, "WICHTIGE SMS/FRIST erkannt von $sender")
                        showImportantNotification(
                            context,
                            "⚠️ Wichtige Nachricht / Frist von: $sender",
                            body
                        )
                    }
                    isPhishingOrSpam(body) -> {
                        // SPAM: Wegfiltern / Unterdrücken
                        Log.d(TAG, "SPAM erfolgreich blockiert von $sender")
                        abortBroadcast()
                        logBlockedSmsLocally(context, sender, body)
                    }
                    else -> {
                        // Normaler Alltag -> durchlassen
                    }
                }
            }
        }
    }

    private fun isPhishingOrSpam(text: String): Boolean {
        val lowerText = text.lowercase()
        return SPAM_PATTERNS.any { lowerText.contains(it) }
    }

    private fun isImportantNotice(text: String): Boolean {
        val lowerText = text.lowercase()
        return IMPORTANT_PATTERNS.any { lowerText.contains(it) }
    }

    private fun showImportantNotification(context: Context, title: String, message: String) {
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

    private fun logBlockedSmsLocally(context: Context, sender: String, body: String) {
        Log.d(TAG, "BLOCKIERT: SMS von $sender wurde lokal gefiltert.")
    }
}


