package com.juaris.app

import android.util.Base64
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner
import java.security.SecureRandom

class QuantumEngine {

    private val random = SecureRandom()
    // NIST Standard ML-DSA-44 (Dilithium2) Post-Quantum Parameter
    private val parameters = DilithiumParameters.dilithium2

    data class PostQuantumKeyPair(
        val publicKeyBase64: String,
        val privateKeyBytes: ByteArray,
        val publicKeyBytes: ByteArray
    )

    /**
     * Erzeugt ein echtes post-quantes Schlüsselpaar (NIST ML-DSA / Dilithium)
     */
    fun generatePostQuantumKeyPair(): PostQuantumKeyPair {
        val keyGen = DilithiumKeyPairGenerator()
        keyGen.init(KeyGenerationParameters(random, 128))
        val keyPair: AsymmetricCipherKeyPair = keyGen.generateKeyPair()

        val pubParams = keyPair.public as DilithiumPublicKeyParameters
        val privParams = keyPair.private as DilithiumPrivateKeyParameters

        val pubBytes = pubParams.encoded
        val privBytes = privParams.encoded

        return PostQuantumKeyPair(
            publicKeyBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP),
            privateKeyBytes = privBytes,
            publicKeyBytes = pubBytes
        )
    }

    /**
     * Signiert Bedrohungs- oder Schwarm-Daten mit echtem Post-Quantum ML-DSA
     */
    fun signThreatData(privateKeyBytes: ByteArray, payload: ByteArray): String {
        val privateKey = DilithiumPrivateKeyParameters(parameters, privateKeyBytes)
        val signer = DilithiumSigner()
        signer.init(true, ParametersWithRandom(privateKey, random))
        val signature = signer.generateSignature(payload)
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    /**
     * Verifiziert eine Post-Quantum Signatur eines Schwarm-Nodes
     */
    fun verifySwarmSignature(publicKeyBytes: ByteArray, payload: ByteArray, base64Signature: String): Boolean {
        return try {
            val publicKey = DilithiumPublicKeyParameters(parameters, publicKeyBytes)
            val signature = Base64.decode(base64Signature, Base64.NO_WRAP)
            val verifier = DilithiumSigner()
            verifier.init(false, publicKey)
            verifier.verifySignature(payload, signature)
        } catch (e: Exception) {
            false
        }
    }
}

