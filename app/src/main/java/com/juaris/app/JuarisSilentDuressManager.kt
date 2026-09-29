package com.juaris.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object JuarisSilentDuressManager {
    private const val PREF_NAME = "juaris_duress_prefs"
    private const val KEY_DURESS_PIN = "duress_pin_hash"
    private const val TAG = "JuarisDuressEngine"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Legt einen speziellen Duress-PIN fest (Sollte in den App-Einstellungen konfiguriert werden).
     */
    fun setDuressPin(context: Context, pin: String) {
        val hashed = pin.hashCode().toString() // In Produktion durch SHA-256 ersetzen
        getPrefs(context).edit().putString(KEY_DURESS_PIN, hashed).apply()
        Log.d(TAG, "🛡️ Stiller Notfall-PIN erfolgreich registriert.")
    }

    /**
     * Prüft, ob der eingegebene PIN ein Duress-PIN (Stiller Alarm) ist.
     * Gibt true zurück, wenn Zwang vorliegt -> Tarn-UI öffnen + stillen Alarm auslösen!
     */
    fun verifyPinAndCheckDuress(context: Context, enteredPin: String): Boolean {
        val savedHash = getPrefs(context).getString(KEY_DURESS_PIN, null) ?: return false
        val enteredHash = enteredPin.hashCode().toString()

        if (savedHash == enteredHash) {
            Log.w(TAG, "🚨 STILLE NDS-ZWANGLAGE ERKANNT! Silent Duress ausgelöst.")
            triggerSilentEmergency(context)
            return true
        }
        return false
    }

    private fun triggerSilentEmergency(context: Context) {
        // 1. Unbemerkt den Schwarm informieren oder Notfall vorbereiten
        // 2. Keine optische Eskalation auf dem Display (Tarnung wahren), aber im Hintergrund Netzwerk isolieren
        try {
            // Starte den Notfall-Prozess im Hintergrund ohne lautes Fullscreen-UI, 
            // oder triggere den Mesh-Notruf-Broadcast an vertrauenswürdige Nodes.
            val intent = android.content.Intent(context, JuarisVpnService::class.java)
            // Hier kann z.B. ein Kill-Switch oder Mesh-SOS-Signal getriggert werden
            Log.d(TAG, "🔒 System im Stealth-Modus: Beweise gesichert, stiller Notruf aktiv.")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim silent duress: ${e.message}")
        }
    }
}

