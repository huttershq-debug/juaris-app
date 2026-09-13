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
} // <--- Diese schließende Klammer hat gefehlt!

enum class CallSecurityResult {
    ALLOW,
    BLOCK
}

enum class SmsSecurityResult {
    SAFE,
    SPAM,
    PHISHING
}

enum class EmailSecurityResult {
    SAFE,
    SPAM,
    PHISHING
}


