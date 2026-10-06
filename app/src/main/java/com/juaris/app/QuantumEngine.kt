package com.juaris.app

import android.util.Base64
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner
import java.security.SecureRandom
import java.util.Arrays

class QuantumEngine {

    private val random = SecureRandom()
    private val parameters: DilithiumParameters = DilithiumParameters.dilithium2

    class PostQuantumKeyPair(
        val publicKeyBase64: String,
        private var privateKeyBytes: ByteArray?,
        val publicKeyBytes: ByteArray
    ) {
        fun getPrivateKeyParameters(params: DilithiumParameters): DilithiumPrivateKeyParameters? {
            val bytes = privateKeyBytes ?: return null
            return DilithiumPrivateKeyParameters(params, bytes)
        }

        fun shredPrivateKey() {
            privateKeyBytes?.let {
                Arrays.fill(it, 0.toByte())
            }
            privateKeyBytes = null
        }
    }

    fun generatePostQuantumKeyPair(): PostQuantumKeyPair {
        val keyGen = DilithiumKeyPairGenerator().apply {
            init(DilithiumKeyGenerationParameters(random, parameters))
        }
        val keyPair = keyGen.generateKeyPair()
        val pub = keyPair.public as DilithiumPublicKeyParameters
        val priv = keyPair.private as DilithiumPrivateKeyParameters

        val pubEncoded = pub.encoded
        val privEncoded = priv.encoded

        return PostQuantumKeyPair(
            publicKeyBase64 = Base64.encodeToString(pubEncoded, Base64.NO_WRAP),
            privateKeyBytes = privEncoded,
            publicKeyBytes = pubEncoded
        )
    }

    fun recreatePrivateKeyFromBytes(bytes: ByteArray): DilithiumPrivateKeyParameters {
        return DilithiumPrivateKeyParameters(parameters, bytes)
    }

    fun recreatePublicKeyFromBase64(base64: String): DilithiumPublicKeyParameters {
        val decoded = Base64.decode(base64, Base64.NO_WRAP)
        return DilithiumPublicKeyParameters(parameters, decoded)
    }

    fun signThreatData(privateKeyObj: DilithiumPrivateKeyParameters, payload: ByteArray): String {
        val signer = DilithiumSigner()
        signer.init(true, ParametersWithRandom(privateKeyObj, random))
        val signatureBytes = signer.generateSignature(payload)
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    fun verifySwarmSignature(
        publicKeyObj: DilithiumPublicKeyParameters,
        payload: ByteArray,
        base64Signature: String
    ): Boolean {
        return try {
            val signature = Base64.decode(base64Signature, Base64.NO_WRAP)
            val signer = DilithiumSigner()
            signer.init(false, publicKeyObj)
            val result = signer.verifySignature(payload, signature)
            Arrays.fill(signature, 0.toByte())
            result
        } catch (e: Exception) {
            false
        }
    }
}
