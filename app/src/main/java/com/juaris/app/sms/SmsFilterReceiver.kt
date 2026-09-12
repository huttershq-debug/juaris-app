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
import com.juaris.app.JuarisDatabase
import com.juaris.app.R
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsFilterReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsFilter"
        private const val CHANNEL_ID = "juaris_important_alerts"

        private val SPAM_PATTERNS = listOf(
            "paket", "konto gesperrt", "zollgebühr", "klicken sie",
            "verification code", "banking update", "wallet locked", "gewonnen"
        )

        private val IMPORTANT_PATTERNS = listOf(
            "rechnung", "mahnung", "inkasso", "gericht", "finanzamt",
            "frist", "zahlungsaufforderung", "steuer", "bescheid", "termin"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("sms_prot", true)) {
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val db = JuarisDatabase.getDatabase(context)

            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: "Unbekannt"
                val body = message.messageBody ?: ""

                Log.d(TAG, "Eingehende SMS von $sender analysiert.")

                when {
                    isImportantNotice(body) -> {
                        Log.d(TAG, "WICHTIGE SMS/FRIST erkannt von $sender")
                        
                        // Direkt in die Datenbank schreiben für die Logs-Ansicht
                        CoroutineScope(Dispatchers.IO).launch {
                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    module = "SMS-Fristen-Wächter",
                                    description = "Frist/Wichtig von $sender: $body",
                                    status = "IMPORTANT"
                                )
                            )
                        }

                        showImportantNotification(
                            context,
                            "⚠️ Wichtige Nachricht / Frist von: $sender",
                            body
                        )
                    }
                    isPhishingOrSpam(body) -> {
                        Log.d(TAG, "SPAM erfolgreich blockiert von $sender")
                        abortBroadcast()

                        // Spam-Blockade direkt in die Datenbank schreiben
                        CoroutineScope(Dispatchers.IO).launch {
                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    module = "SMS-Filter",
                                    description = "Spam-SMS blockiert von $sender",
                                    status = "BLOCKED"
                                )
                            )
                        }
                    }
                    else -> {
                        // Normaler Durchlauf
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
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

