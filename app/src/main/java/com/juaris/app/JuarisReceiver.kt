package com.juaris.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Juaris Background Receiver
 * Lauscht im Hintergrund dezentral und offline auf Anrufe und SMS
 * und leitet sie direkt an die SecurityEngine weiter.
 */
class JuarisReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JuarisReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            
            // 1. ANRUF-ÜBERWACHUNG IM HINTERGRUND
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unbekannt"
                    Log.d(TAG, "Eingehender Anruf erkannt: $incomingNumber")
                    
                    // Lokaler Offline-Check durch die SecurityEngine
                    val result = SecurityEngine.analyzeIncomingCall(incomingNumber)
                    if (result == CallSecurityResult.BLOCK) {
                        Log.w(TAG, "ALARM: Gefährlicher Anruf von $incomingNumber lokal blockiert!")
                    }
                }
            }

            // 2. SMS-ÜBERWACHUNG IM HINTERGRUND
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (message in messages) {
                    val sender = message.displayOriginatingAddress ?: "Unbekannt"
                    val body = message.messageBody ?: ""
                    Log.d(TAG, "Eingehende SMS von $sender")

                    // Lokaler Offline-Check durch die SecurityEngine
                    val result = SecurityEngine.analyzeIncomingSms(sender, body)
                    if (result == SmsSecurityResult.QUARANTINE_AND_ALERT) {
                        Log.w(TAG, "ALARM: Phishing-SMS von $sender in Quarantäne verschoben!")
                    }
                }
            }
        }
    }
}
