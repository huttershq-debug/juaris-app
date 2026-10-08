package com.juaris.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Verwahrt die Datenbank-Passphrase: eine zufaellige 32-Byte-Passphrase wird mit einem
 * Android-Keystore-Schluessel (AES-GCM) verschluesselt abgelegt. Es gibt keinen Klartext-Fallback.
 */
internal object KeystoreVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "juaris_db_wrap_key_v1"
    private const val PREFS = "juaris_vault"
    private const val KEY_BLOB = "db_key_blob"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    /** Wirft eine Exception, wenn der Keystore nicht nutzbar ist; der Aufrufer behandelt das. */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapKey = getOrCreateWrapKey()
        val stored = prefs.getString(KEY_BLOB, null)
        if (stored != null) {
            return decrypt(wrapKey, Base64.decode(stored, Base64.NO_WRAP))
        }
        val passphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val blob = Base64.encodeToString(encrypt(wrapKey, passphrase), Base64.NO_WRAP)
        check(prefs.edit().putString(KEY_BLOB, blob).commit()) { "Schluessel konnte nicht gespeichert werden" }
        return passphrase
    }

    fun reset(context: Context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        } catch (e: Throwable) {
            // ignorieren
        }
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS)
        } catch (e: Throwable) {
            // ignorieren
        }
    }

    private fun getOrCreateWrapKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(key: SecretKey, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(data)
        return byteArrayOf(iv.size.toByte()) + iv + cipherText
    }

    private fun decrypt(key: SecretKey, blob: ByteArray): ByteArray {
        val ivLength = blob[0].toInt() and 0xFF
        require(ivLength in 12..16 && blob.size > ivLength + 1) { "Ungueltiger Schluessel-Blob" }
        val iv = blob.copyOfRange(1, 1 + ivLength)
        val cipherText = blob.copyOfRange(1 + ivLength, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherText)
    }
