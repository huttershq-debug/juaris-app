package com.localshield.v5.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsFilterReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsFilter"
        private val SPAM_PATTERNS = listOf(
            "paket", "konto gesperrt", "zollgebühr", "klicken sie",
            "verification code", "banking update", "wallet locked"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (message in messages) {
                val sender = message.displayOriginatingAddress ?: "Unbekannt"
                val body = message.messageBody ?: ""

                Log.d(TAG, "Eingehende SMS von $sender analysiert.")

                if (isPhishingOrSpam(body)) {
                    abortBroadcast()
                    logBlockedSmsLocally(context, sender, body)
                }
            }
        }
    }

    private fun isPhishingOrSpam(text: String): Boolean {
        val lowerText = text.lowercase()
        return SPAM_PATTERNS.any { lowerText.contains(it) }
    }

    private fun logBlockedSmsLocally(context: Context, sender: String, body: String) {
        Log.d(TAG, "BLOCKIERT: SMS von $sender wurde lokal gefiltert.")
    }
}
