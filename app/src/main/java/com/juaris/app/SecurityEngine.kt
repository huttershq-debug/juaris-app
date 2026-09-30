package com.juaris.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator

class SecurityEngine(private val context: Context) {

    // KORREKTUR 1: Nutzt die unverschlüsselten Public Prefs zur crashsicheren Abfrage in Hintergrund-Workern
    private val prefs = context.getSharedPreferences("juaris_public_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "JuarisSecurityEngine"
        private const val HARDWARE_KEY_ALIAS = "JuarisHardwareRootKey"

        // Lokale Whitelists und bekannte Phishing-Indikatoren (Offline-Heuristik)
        private val PHISHING_KEYWORDS = listOf(
            "konto gesperrt", "sofort verifizieren", "gewinn", "bitcoin wallet",
            "kreditkarte abgelaufen", "dringend handeln", "bank-login", "security alert",
            "paket zugestellt", "zollgebühr", "post.at/paket", "paypal sicherheit",
            "rechnung im anhang", "passwort zurücksetzen", "unauthorisierter zugriff"
        )
       
        private val TRUSTED_DOMAINS = listOf(
            "google.com", "apple.com", "microsoft.com", "banking", "gov.at", "gv.at", "finanzonline.at"
        )

        // Root- und Manipulationserkennung (Hardware Zero-Trust)
        fun isDeviceCompromised(): Boolean {
            val paths = arrayOf(
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/view/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
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

    sealed class ThreatResult {
        data class Safe(val message: String) : ThreatResult()
        data class Suspicious(val reason: String) : ThreatResult()
        data class Blocked(val reason: String) : ThreatResult()
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

    /**
     * Analysiert einen Text (SMS, E-Mail-Notification, Chat) in Echtzeit auf Phishing-Merkmale.
     */
    suspend fun analyzeText(content: String): ThreatResult = withContext(Dispatchers.Default) {
        val lowerContent = content.lowercase()
       
        var matchCount = 0
        var matchedKeyword = ""
       
        for (keyword in PHISHING_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                matchCount++
                matchedKeyword = keyword
            }
        }

        val containsSuspiciousUrl = containsMaliciousUrlPattern(lowerContent)

        val result = when {
            matchCount >= 2 || containsSuspiciousUrl -> {
                Log.w(TAG, "Bedrohung erkannt! Schlüsselwort: '$matchedKeyword', Verdächtige URL: $containsSuspiciousUrl")
                ThreatResult.Blocked("Phishing-Verdacht! Gefährliches Muster erkannt: '$matchedKeyword'")
            }
            matchCount == 1 -> {
                ThreatResult.Suspicious("Warnung: Ungewöhnlicher Begriff gefunden ('$matchedKeyword').")
            }
            else -> {
                ThreatResult.Safe("Nachricht als sicher eingestuft.")
            }
        }

        // Automatischer Log-Eintrag in die lokale Datenbank
        when (result) {
            is ThreatResult.Blocked -> logThreatToDatabase("Local-AI-Heuristik", result.reason, "BLOCKED")
            is ThreatResult.Suspicious -> logThreatToDatabase("Local-AI-Heuristik", result.reason, "WARNING")
            is ThreatResult.Safe -> {}
        }

        return@withContext result
    }

    private fun containsMaliciousUrlPattern(text: String): Boolean {
        val urlPattern = "(http://|https://|www\\.)([a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+)(/[\\w-]*)*".toRegex()
        val matchResults = urlPattern.findAll(text)
       
        for (match in matchResults) {
            val url = match.value.lowercase()
            val isTrusted = TRUSTED_DOMAINS.any { domain -> url.contains(domain) }
            if (!isTrusted && (url.contains("secure") || url.contains("login") || url.contains("update") || url.contains("verify") || url.contains("account") || url.contains("bank"))) {
                return true
            }
        }
        return false
    }

    suspend fun logThreatToDatabase(module: String, description: String, status: String, details: String = "Lokale Heuristik-Prüfung aktiv") {
        withContext(Dispatchers.IO) {
            try {
                val db = JuarisDatabase.getDatabase(context)
                val logEntity = SecurityLogEntity(
                    timestamp = System.currentTimeMillis(),
                    status = status,
                    module = module,
                    description = description,
                    details = details
                )
                db.securityLogDao().insertLog(logEntity)
            } catch (e: Exception) {
                Log.e(TAG, "Fehler beim Speichern des Logs: ${e.localizedMessage}")
            }
        }
    }

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

    // KORREKTUR 2: Synchronisiert die Rückgabe-Enums exakt mit den Erwartungen deiner Core-Filter (SmsFilterReceiver & EmailScanWorker)
    fun analyzeIncomingSms(smsText: String): SmsSecurityResult {
        val lower = smsText.lowercase()
        return when {
            PHISHING_KEYWORDS.any { lower.contains(it) } || lower.contains("http") -> SmsSecurityResult.BLOCK
            lower.contains("spam") || lower.contains("werbung") -> SmsSecurityResult.SPAM
            else -> SmsSecurityResult.SAFE
        }
    }

    fun analyzeIncomingEmail(emailContent: String): EmailSecurityResult {
        val lower = emailContent.lowercase()
        return when {
            PHISHING_KEYWORDS.any { lower.contains(it) } || lower.contains("http") -> EmailSecurityResult.BLOCK
            lower.contains("spam") || lower.contains("werbung") -> EmailSecurityResult.SPAM
            else -> EmailSecurityResult.SAFE
        }
    }
}

enum class CallSecurityResult { ALLOW, BLOCK, QUARANTINE_AND_ALERT }
enum class SmsSecurityResult { SAFE, SPAM, PHISHING, QUARANTINE_AND_ALERT, BLOCK }
enum class EmailSecurityResult { SAFE, SPAM, PHISHING, QUARANTINE_AND_ALERT, BLOCK }

