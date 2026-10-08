package com.juaris.app

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.GeneralSecurityException

object SecureStorageRecovery {

    private const val TAG = "JuarisKeyStoreRecovery"
    private const val SECURE_PREFS_NAME = "juaris_secure_prefs"

    fun initializeOrRecoverStorage(context: Context) {
        try {
            loadEncryptedStorage(context)
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "⚠️ Kritischer KeyStore-Fehler erkannt (Device-Corrupt / Key Invalid): ${e.message}")
            performEmergencyReset(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unbekannter Fehler beim Initialisieren des Secure Storages: ${e.message}")
        }
    }

    private fun loadEncryptedStorage(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun performEmergencyReset(context: Context) {
        try {
            Log.w(TAG, "🔄 Starte Notfall-Bereinigung des beschädigten Keystore-Kontexts...")

            val prefsFile = File(context.filesDir.parent, "shared_prefs/$SECURE_PREFS_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
            }

            Log.d(TAG, "✅ Notfall-Reset erfolgreich abgeschlossen. App läuft im sauberen Urzustand weiter.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler während des Notfall-Resets: ${e.message}")
        }
    }
}

