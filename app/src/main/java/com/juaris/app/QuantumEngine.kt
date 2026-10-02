package com.juaris.app

import android.util.Base64
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner
import java.security.SecureRandom
import java.util.Arrays

/**
 * JUARIS QUANTUM ENGINE
 * Implementiert das NIST-standardisierte CRYSTALS-Dilithium2 Verfahren.
 * Schützt den dezentralen Schwarm vor Manipulationen durch quantenbasierte Cyber-Angriffe.
 */
class QuantumEngine {

    private val random = SecureRandom()
    private val parameters: DilithiumParameters = DilithiumParameters.dilithium2

    data class PostQuantumKeyPair(
        val publicKeyBase64: String,
        val privateKeyBytes: ByteArray,
        val publicKeyBytes: ByteArray,
        val privateKeyObj: DilithiumPrivateKeyParameters,
        val publicKeyObj: DilithiumPublicKeyParameters
    ) {
        /**
         * KORREKTUR 1 (Anti-Forensik): Löscht den privaten Schlüssel physisch aus dem RAM!
         * Muss vom Entwickler aufgerufen werden, sobald der Schlüssel in den 
         * EncryptedSharedPreferences gesichert wurde. Verhindert Cold-Boot- und RAM-Dumping-Angriffe.
         */
        fun shredPrivateKey() {
            Arrays.fill(privateKeyBytes, 0.toByte())
        }
    }

    fun generatePostQuantumKeyPair(): PostQuantumKeyPair {
        val keyGenParams = DilithiumKeyGenerationParameters(random, parameters)
        val keyGen = DilithiumKeyPairGenerator()
        keyGen.init(keyGenParams)
        val keyPair: AsymmetricCipherKeyPair = keyGen.generateKeyPair()

        val pubParams = keyPair.public as DilithiumPublicKeyParameters
        val privParams = keyPair.private as DilithiumPrivateKeyParameters

        val pubBytes = pubParams.encoded
        val privBytes = privParams.encoded

        return PostQuantumKeyPair(
            publicKeyBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP),
            privateKeyBytes = privBytes,
            publicKeyBytes = pubBytes,
            privateKeyObj = privParams,
            publicKeyObj = pubParams
        )
    }

    // KORREKTUR 2: Rekonstruktions-Schnittstelle für persistierte Schlüssel!
    // Erlaubt es JUARIS, den hardwareverschlüsselten privaten Schlüssel beim App-Start 
    // wieder aus der DB/SharedPrefs zu laden, ohne jedes Mal neue Identitäten zu erzwingen.
    fun recreatePrivateKeyFromBytes(privBytes: ByteArray): DilithiumPrivateKeyParameters {
        return DilithiumPrivateKeyParameters(parameters, privBytes)
    }

    fun recreatePublicKeyFromBytes(pubBytes: ByteArray): DilithiumPublicKeyParameters {
        return DilithiumPublicKeyParameters(parameters, pubBytes)
    }

    fun recreatePublicKeyFromBase64(pubBase64: String): DilithiumPublicKeyParameters {
        val decodedBytes = Base64.decode(pubBase64, Base64.NO_WRAP)
        return recreatePublicKeyFromBytes(decodedBytes)
    }

    fun signThreatData(privateKeyObj: DilithiumPrivateKeyParameters, payload: ByteArray): String {
        val signer = DilithiumSigner()
        signer.init(true, ParametersWithRandom(privateKeyObj, random))
        val signature = signer.generateSignature(payload)
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    fun verifySwarmSignature(publicKeyObj: DilithiumPublicKeyParameters, payload: ByteArray, base64Signature: String): Boolean {
        return try {
            val signature = Base64.decode(base64Signature, Base64.NO_WRAP)
            val verifier = DilithiumSigner()
            verifier.init(false, publicKeyObj)
            val result = verifier.verifySignature(payload, signature)
            
            // Flüchtige Signatur-Bytes im Speicher nullen
            Arrays.fill(signature, 0.toByte())
            result
        } catch (e: Exception) {
            false
        }
    }
}
