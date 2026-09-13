package com.juaris.app

import android.content.Context

class SecurityEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)

    fun getBlockedNumbers(): Set<String> {
        return prefs.getStringSet("blocked_numbers", emptySet()) ?: emptySet()
    }

    fun isNumberBlocked(phoneNumber: String): Boolean {
        val blockedSet = getBlockedNumbers()
        return blockedSet.any { pattern ->
            phoneNumber.contains(pattern) || phoneNumber == pattern
        }
    }

    fun analyzeIncomingCall(phoneNumber: String): CallSecurityResult {
        return if (isNumberBlocked(phoneNumber)) {
            CallSecurityResult.BLOCK
        } else {
            CallSecurityResult.ALLOW
        }
    }

    fun analyzeIncomingSms(smsText: String): SmsSecurityResult {
        val lower = smsText.lowercase()
        return when {
            lower.contains("phishing") || lower.contains("malware") -> SmsSecurityResult.QUARANTINE_AND_ALERT
            lower.contains("spam") -> SmsSecurityResult.SPAM
            else -> SmsSecurityResult.SAFE
        }
    }

    fun analyzeIncomingEmail(emailContent: String): EmailSecurityResult {
        val lower = emailContent.lowercase()
        return when {
            lower.contains("phishing") || lower.contains("malware") -> EmailSecurityResult.QUARANTINE_AND_ALERT
            lower.contains("spam") -> EmailSecurityResult.SPAM
            else -> EmailSecurityResult.SAFE
        }
    }
}

enum class CallSecurityResult {
    ALLOW,
    BLOCK,
    QUARANTINE_AND_ALERT
}

enum class SmsSecurityResult {
    SAFE,
    SPAM,
    PHISHING,
    QUARANTINE_AND_ALERT,
    BLOCK
}

enum class EmailSecurityResult {
    SAFE,
    SPAM,
    PHISHING,
    QUARANTINE_AND_ALERT,
    BLOCK
}

