package com.juaris.app

import android.content.Context
import android.util.Log
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityEngine {

    private const val TAG = "JuarisLocalEngine"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128

    /**
     * Verschlüsselt sensible lokale Daten oder Dateien direkt auf dem Smartphone.
     * Es verlässt niemals das Gerät.
     */
    fun encryptData(inputData: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherText = cipher.doFinal(inputData)
        
        // IV an den Anfang der verschlüsselten Daten hängen
        return iv + cipherText
    }

    /**
     * Entschlüsselt lokale Daten sicher on-the-fly.
     */
    fun decryptData(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = encryptedData.copyOfRange(0, 12)
        val cipherText = encryptedData.copyOfRange(12, encryptedData.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherText)
    }

    /**
     * Bereitet lokale Verzeichnisse für die sichere Ablage von Medien,
     * Dokumenten und Analysedaten vor.
     */
    fun initializeLocalStorage(context: Context): Boolean {
        try {
            val secureDir = File(context.filesDir, "juaris_secure_vault")
            if (!secureDir.exists()) {
                secureDir.mkdirs()
            }
            Log.d(TAG, "📁 Lokaler High-Security-Speicher initialisiert: ${secureDir.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Initialisieren des Speichers: ${e.message}")
            return false
        }
    }
}

