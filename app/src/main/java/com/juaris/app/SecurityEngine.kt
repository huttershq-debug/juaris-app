package com.juaris.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator

class SecurityEngine(private val context: Context) {

    private val prefs = context.getSharedPreferences("juaris_secure_vault", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "JuarisZeroTrust"
        private const val HARDWARE_KEY_ALIAS = "JuarisHardwareRootKey"

        // Root- und Manipulationserkennung (Hardware Zero-Trust)
        fun isDeviceCompromised(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                return true
            }
            return false
        }
    }

    // Verankert die Security-Engine direkt im TEE / StrongBox des Smartphones
    fun verifyHardwareIntegrityAndBind(): Boolean {
        if (isDeviceCompromised()) {
            Log.e(TAG, "KRITISCH: Root- oder Manipulationsversuch im Betriebssystem erkannt!")
            return false
        }

        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(HARDWARE_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val builder = KeyGenParameterSpec.Builder(
                    HARDWARE_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        builder.setIsStrongBoxBacked(true)
                    } catch (e: Exception) {
                        Log.w(TAG, "StrongBox nicht verfügbar, verwende TEE Hardware-Enklave.")
                    }
                }

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
                Log.d(TAG, "Hardware-Schlüssel erfolgreich im sicheren TEE/StrongBox verankert.")
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der Hardware-Bindung: ${e.message}")
            return false
        }
    }

    // Deine Kern-Funktionen für Blocklisten und Bedrohungsanalyse
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

// Deine bewährten Ergebnis-Enums
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

