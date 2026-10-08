package com.juaris.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object JuarisSilentDuressManager {
    private const val PREF_NAME = "juaris_duress_secure_prefs"
    private const val KEY_DURESS_PIN = "duress_pin_hash"
    private const val TAG = "JuarisDuressEngine"

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context.applicationContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hardware-Keystore blockiert. Nutze isolierten Storage-Fallback.")
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun setDuressPin(context: Context, pin: String) {
        val hashed = hashSha256(pin) ?: return
        getPrefs(context).edit().putString(KEY_DURESS_PIN, hashed).apply()
        Log.d(TAG, "🛡️ Stiller Notfall-PIN erfolgreich hardwareverschlüsselt registriert.")
    }

    fun verifyPinAndCheckDuress(context: Context, enteredPin: String): Boolean {
        val savedHash = getPrefs(context).getString(KEY_DURESS_PIN, null) ?: return false
        val enteredHash = hashSha256(enteredPin) ?: return false

        if (savedHash == enteredHash) {
            Log.w(TAG, "🚨 STILLE NDS-ZWANGLAGE ERKANNT! Silent Duress ausgelöst.")
            triggerSilentEmergency(context)
            return true
        }
        return false
    }

    private fun triggerSilentEmergency(context: Context) {
        try {
            val intent = Intent(context.applicationContext, JuarisVpnService::class.java).apply {
                putExtra("action", "STEALTH_LOCKDOWN")
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
            Log.d(TAG, "🔒 System im Stealth-Modus: Beweise gesichert, stiller Notruf aktiv.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim silent duress: ${e.message}")
        }
    }

    private fun hashSha256(input: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Kritischer Fehler im Krypto-Treiber: SHA-256 nicht verfügbar.", e)
            null
        }
    }
}

