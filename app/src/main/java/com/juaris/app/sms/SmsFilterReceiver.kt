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

                // Lokale Phishing-/Betrugserkennung im SMS-Inhalt
                val isSuspicious = messageBody.contains("http", ignoreCase = true) ||
                        messageBody.contains("banking", ignoreCase = true) ||
                        messageBody.contains("paket", ignoreCase = true)

                val status = if (isSuspicious) "BLOCKED" else "ALLOWED"
                val details = "SMS von $sender: $messageBody"

                scope.launch {
                    db.securityLogDao().insertLog(
                        SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            status = status,
                            details = details
                        )
                    )
                }
            }
        }
    }
}


