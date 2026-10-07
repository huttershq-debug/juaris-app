package com.juaris.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.juaris.app.JuarisDatabase
import com.juaris.app.JuarisNotificationDispatcher
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * JUARIS PASSIVER SMS WÄCHTER
 * Hört passiv auf eingehende SMS-Nachrichten und prüft diese lokal auf Phishing-Merkmale,
 * ohne in den System-Nachrichtenfluss einzugreifen oder gegen Google-Richtlinien zu verstoßen.
 */
class SmsFilterReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JuarisSmsReceiver"
       
        private val receiverJob = SupervisorJob()
        private val receiverScope = CoroutineScope(Dispatchers.IO + receiverJob)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                pendingResult.finish()
                return
            }

            val db = JuarisDatabase.getDatabase(context.applicationContext)

            receiverScope.launch {
                try {
                    for (sms in messages) {
                        val messageBody = sms.messageBody ?: continue
                        val sender = sms.originatingAddress ?: "Unbekannt"

                        // Lokale Heuristik für Phishing-Links
                        val isSuspicious = messageBody.contains("http", ignoreCase = true) ||
                                messageBody.contains("banking", ignoreCase = true) ||
                                messageBody.contains("paket", ignoreCase = true) ||
                                messageBody.contains("konto", ignoreCase = true) ||
                                messageBody.contains("verifizieren", ignoreCase = true)

                        if (isSuspicious) {
                            // 1. Protokollierung des erkannten Betrugsversuchs
                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    status = "WARNING",
                                    module = "SMS-Wächter",
                                    description = "Phishing-Verdacht bei Nachricht von [$sender]",
                                    details = "Absender: $sender | Textauszug: ${messageBody.take(60)}..."
                                )
                            )

                            // 2. Sofortiger Alarm an den Nutzer
                            JuarisNotificationDispatcher.sendPriorityAlert(
                                context = context.applicationContext,
                                title = "🚨 Verdächtige SMS erkannt!",
                                message = "Eine Nachricht von $sender enthält potenzielle Phishing-Merkmale. Bitte Vorsicht!",
                                isCritical = true
                            )
                        } else {
                            // Datenschutzkonformes Logging ohne Klartextspeicherung normaler SMS
                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    status = "SAFE",
                                    module = "SMS-Wächter",
                                    description = "Eingehende SMS verifiziert",
                                    details = "Absender: Anonymisiert | Integrität: Sicher"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler bei der lokalen SMS-Analyse: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}


