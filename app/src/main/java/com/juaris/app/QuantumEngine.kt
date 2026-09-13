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

class QuantumEngine {

    private val random = SecureRandom()
    private val parameters: DilithiumParameters = DilithiumParameters.dilithium2

    data class PostQuantumKeyPair(
        val publicKeyBase64: String,
        val privateKeyBytes: ByteArray,
        val publicKeyBytes: ByteArray
    )

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
            publicKeyBytes = pubBytes
        )
    }

    fun signThreatData(privateKeyBytes: ByteArray, payload: ByteArray): String {
        val privateKey = DilithiumPrivateKeyParameters(parameters, privateKeyBytes)
        val signer = DilithiumSigner()
        signer.init(true, ParametersWithRandom(privateKey, random))
        val signature = signer.generateSignature(payload)
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

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

