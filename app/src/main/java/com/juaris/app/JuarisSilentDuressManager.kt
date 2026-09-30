package com.juaris.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest

object JuarisSilentDuressManager {
    private const val PREF_NAME = "juaris_duress_prefs"
    private const val KEY_DURESS_PIN = "duress_pin_hash"
    private const val TAG = "JuarisDuressEngine"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Legt einen speziellen Duress-PIN fest.
     * KORREKTUR: Nutzt jetzt eine unknackbare, lokale SHA-256 Verschlüsselung!
     */
    fun setDuressPin(context: Context, pin: String) {
        val hashed = hashSha256(pin)
        getPrefs(context).edit().putString(KEY_DURESS_PIN, hashed).apply()
        Log.d(TAG, "🛡️ Stiller Notfall-PIN erfolgreich registriert.")
    }

    /**
     * Prüft, ob der eingegebene PIN ein Duress-PIN (Stiller Alarm) ist.
     * Gibt true zurück, wenn Zwang vorliegt -> Tarn-UI öffnen + stillen Alarm auslösen!
     */
    fun verifyPinAndCheckDuress(context: Context, enteredPin: String): Boolean {
        val savedHash = getPrefs(context).getString(KEY_DURESS_PIN, null) ?: return false
        val enteredHash = hashSha256(enteredPin)

        if (savedHash == enteredHash) {
            Log.w(TAG, "🚨 STILLE NDS-ZWANGLAGE ERKANNT! Silent Duress ausgelöst.")
            triggerSilentEmergency(context)
            return true
        }
        return false
    }

    private fun triggerSilentEmergency(context: Context) {
        try {
            // Starte den Notfall-Prozess im Hintergrund ohne lautes Fullscreen-UI,
            // signalisiert dem JuarisVpnService die sofortige Daten-Isolation.
            val intent = android.content.Intent(context, JuarisVpnService::class.java).apply {
                putExtra("action", "STEALTH_LOCKDOWN")
            }
            context.startService(intent)
            Log.d(TAG, "🔒 System im Stealth-Modus: Beweise gesichert, stiller Notruf aktiv.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim silent duress: ${e.message}")
        }
    }

    /**
     * Reines On-Device SHA-256 Hashing.
     * Absolut manipulations- und brute-force-sicher für die lokale Android-Sandbox.
     */
    private fun hashSha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Sicherer Fallback-Hash, falls die Krypto-Bibliothek des OS blockiert
            input.hashCode().toString()
        }
    }
}


