package com.juaris.app

import android.util.Log

enum class SecurityStatus {
    SAFE, BLOCK
}

enum class CallSecurityResult { ALLOW, BLOCK }
enum class SmsSecurityResult { ALLOW, QUARANTINE_AND_ALERT }
enum class EmailSecurityResult { SAFE, WARN_USER, BLOCK }

object SecurityEngine {

    private const val TAG = "JuarisSecurity"
    private val blockedNumbers = setOf("+43123456789", "+49987654321")

    fun evaluate(): SecurityStatus {
        Log.d(TAG, "Evaluating security status")
        return SecurityStatus.SAFE
    }

    fun analyzeText(text: String): SecurityStatus {
        val analyzer = LocalPhishingAnalyzer()
        val result = analyzer.analyzeText(text)
        return if (result.isSuspicious) SecurityStatus.BLOCK else SecurityStatus.SAFE
    }

    fun analyzeIncomingCall(phoneNumber: String): CallSecurityResult {
        Log.d(TAG, "Prüfe Anruf von: $phoneNumber")
        return if (blockedNumbers.contains(phoneNumber)) {
            CallSecurityResult.BLOCK
        } else {
            CallSecurityResult.ALLOW
        }
    }

    fun analyzeIncomingSms(sender: String, messageBody: String): SmsSecurityResult {
        val analyzer = LocalPhishingAnalyzer()
        val result = analyzer.analyzeText(messageBody)
        return if (result.isSuspicious) {
            SmsSecurityResult.QUARANTINE_AND_ALERT
        } else {
            SmsSecurityResult.ALLOW
        }
    }

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

