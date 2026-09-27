package com.juaris.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.juaris.app.JuarisDatabase
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsFilterReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val db = JuarisDatabase.getDatabase(context)

            for (sms in messages) {
                val messageBody = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: "Unbekannt"

                // Lokale Phishing-Erkennung für SMS
                val isSuspicious = messageBody.contains("http", ignoreCase = true) ||
                        messageBody.contains("banking", ignoreCase = true) ||
                        messageBody.contains("paket", ignoreCase = true) ||
                        messageBody.contains("konto", ignoreCase = true) ||
                        messageBody.contains("verifizieren", ignoreCase = true)

                val status = if (isSuspicious) "BLOCKED" else "ALLOWED"
                val module = "SMS-Shield"
                val description = if (isSuspicious) "Phishing-Verdacht in SMS erkannt" else "Eingehende SMS geprüft"
                val details = "Absender: $sender | Text: $messageBody"

                scope.launch {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = status,
                            module = module,
                            description = description,
                            details = details
                        )
                    )
                }
            }
        }
    }
}

