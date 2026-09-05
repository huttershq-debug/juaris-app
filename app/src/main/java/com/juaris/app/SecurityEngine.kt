package com.juaris.app

import android.util.Log

/**
 * Juaris Security Engine
 * Lokale Offline-Überwachung für Anrufe, SMS und E-Mails.
 */
object SecurityEngine {

    private const val TAG = "JuarisSecurity"

    private val blockedNumbers = setOf("+43123456789", "+49987654321")
    private val maliciousKeywords = listOf("gewinn", "konto gesperrt", "krypto", "urgent", "phishing")

    // 1. ANRUF-SCHUTZ
    fun analyzeIncomingCall(phoneNumber: String): CallSecurityResult {
        Log.d(TAG, "Prüfe Anruf von: $phoneNumber")
        return if (blockedNumbers.contains(phoneNumber)) {
            Log.w(TAG, "WARNUNG: Gefährlicher Anruf blockiert: $phoneNumber")
            CallSecurityResult.BLOCK
        } else {
            CallSecurityResult.ALLOW
        }
    }

    // 2. SMS-SCHUTZ
    fun analyzeIncomingSms(sender: String, messageBody: String): SmsSecurityResult {
        Log.d(TAG, "Prüfe SMS von $sender")
        val lowercaseBody = messageBody.lowercase()
        val containsThreat = maliciousKeywords.any { keyword -> lowercaseBody.contains(keyword) }
        return if (containsThreat) {
            Log.w(TAG, "WARNUNG: Phishing-SMS von $sender abgefangen!")
            SmsSecurityResult.QUARANTINE_AND_ALERT
        } else {
            SmsSecurityResult.ALLOW
        }
    }

    // 3. E-MAIL-SCHUTZ
    fun analyzeIncomingEmail(sender: String, subject: String, body: String): EmailSecurityResult {
        Log.d(TAG, "Prüfe E-Mail von $sender mit Betreff: $subject")
        val combinedContent = "$subject $body".lowercase()
        val containsThreat = maliciousKeywords.any { keyword -> combinedContent.contains(keyword) }
        return if (containsThreat) {
            Log.w(TAG, "WARNUNG: Gefährliche E-Mail erkannt von $sender!")
            EmailSecurityResult.WARN_USER
        } else {
            EmailSecurityResult.SAFE
        }
    }
}

enum class CallSecurityResult { ALLOW, BLOCK }
enum class SmsSecurityResult { ALLOW, QUARANTINE_AND_ALERT }
enum class EmailSecurityResult { SAFE, WARN_USER }
