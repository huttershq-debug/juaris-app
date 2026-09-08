package com.juaris.app

import android.content.Context

class LocalPhishingAnalyzer(private val context: Context) {
    // Füge hier die Logik zur lokalen Phishing-Analyse hinzu
}
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

     // Ersetze die alte fun analyzeIncomingSms durch diese Version:
    fun analyzeIncomingSms(sender: String, messageBody: String): SmsSecurityResult {
        val analyzer = LocalPhishingAnalyzer()
        val result = analyzer.analyzeText(messageBody)
        
        return if (result.isSuspicious) {
            SmsSecurityResult.QUARANTINE_AND_ALERT
        } else {
            SmsSecurityResult.SAFE
        }
    }

    // Ersetze die alte fun analyzeIncomingEmail durch diese Version:
    fun analyzeIncomingEmail(sender: String, subject: String, body: String): EmailSecurityResult {
        val analyzer = LocalPhishingAnalyzer()
        val result = analyzer.analyzeText("$subject $body")
        
        return if (result.isSuspicious) {
            EmailSecurityResult.BLOCK
        } else {
            EmailSecurityResult.SAFE
        }
    }
}

enum class CallSecurityResult { ALLOW, BLOCK }
enum class SmsSecurityResult { ALLOW, QUARANTINE_AND_ALERT }
enum class EmailSecurityResult { SAFE, WARN_USER }
