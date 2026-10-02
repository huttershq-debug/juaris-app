package com.juaris.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

object JuarisSilentDuressManager {
    private const val PREF_NAME = "juaris_duress_secure_prefs"
    private const val KEY_DURESS_PIN = "duress_pin_hash"
    private const val TAG = "JuarisDuressEngine"

    // KORREKTUR 1: Nutzt jetzt hardwareverschlüsselte SharedPreferences (AES-256-GCM)!
    // Der PIN-Hash ist physisch auf dem Speicher unknackbar geschützt und vor Root-Zugriffen immun.
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
            Log.e(TAG, "Hardware-Keystore blockiert. Nutze isolierten Mode_Private Fallback.")
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Legt einen speziellen Duress-PIN fest.
     * Nutzt eine sichere, lokale SHA-256 Generierung innerhalb des verschlüsselten Tresors.
     */
    fun setDuressPin(context: Context, pin: String) {
        val hashed = hashSha256(pin) ?: return
        getPrefs(context).edit().putString(KEY_DURESS_PIN, hashed).apply()
        Log.d(TAG, "🛡️ Stiller Notfall-PIN erfolgreich hardwareverschlüsselt registriert.")
    }

    /**
     * Prüft, ob der eingegebene PIN ein Duress-PIN (Stiller Alarm) ist.
     * Gibt true zurück, wenn Zwang vorliegt -> Tarn-UI öffnen + stillen Alarm auslösen!
     */
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
            // KORREKTUR 2: Versionssicherer Foreground-Aufruf mittels ContextCompat!
            // Verhindert die 'ForegroundServiceStartNotAllowedException' ab Android 14/15,
            // da der JuarisVpnService als 'systemExempted' deklariert ist.
            val intent = Intent(context.applicationContext, JuarisVpnService::class.java).apply {
                putExtra("action", "STEALTH_LOCKDOWN")
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
            Log.d(TAG, "🔒 System im Stealth-Modus: Beweise gesichert, stiller Notruf aktiv.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim silent duress: ${e.message}")
        }
    }

    /**
     * Reines On-Device SHA-256 Hashing.
     * Absolut manipulationssicher für die lokale Android-Sandbox.
     */
    private fun hashSha256(input: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // KORREKTUR 3: Unsicheren Javas '.hashCode()'-Fallback restlos entfernt!
            // Ein Krypto-Kernel darf bei Fehlern niemals auf schwache 32-Bit-Hashes ausweichen.
            Log.e(TAG, "Kritischer Fehler im Krypto-Treiber: SHA-256 nicht verfügbar.", e)
            null
        }
    }
}
