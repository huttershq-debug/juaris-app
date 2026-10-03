package com.juaris.app

import android.util.Base64
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.pqc.crypto.crystals.dilithium.*
import java.security.SecureRandom
import java.util.Arrays

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
        fun shredPrivateKey() { Arrays.fill(privateKeyBytes, 0.toByte()) }
    }

    fun generatePostQuantumKeyPair(): PostQuantumKeyPair {
        val keyGen = DilithiumKeyPairGenerator().apply { 
            init(DilithiumKeyGenerationParameters(random, parameters)) 
        }
        val keyPair = keyGen.generateKeyPair()
        val pub = keyPair.public as DilithiumPublicKeyParameters
        val priv = keyPair.private as DilithiumPrivateKeyParameters
        return PostQuantumKeyPair(
            Base64.encodeToString(pub.encoded, Base64.NO_WRAP), 
            priv.encoded, 
            pub.encoded, 
            priv, 
            pub
        )
    }

    // KORREKTUR: Nutzt die offizielle BouncyCastle-Signatur unter automatischer 
    // Ableitung des symmetrischen Public-Key-Zweigs aus den encodeten Rohdaten!
    fun recreatePrivateKeyFromBytes(bytes: ByteArray): DilithiumPrivateKeyParameters {
        return DilithiumPrivateKeyParameters(parameters, bytes, null)
    }

    fun recreatePublicKeyFromBase64(base64: String): DilithiumPublicKeyParameters {
        return DilithiumPublicKeyParameters(parameters, Base64.decode(base64, Base64.NO_WRAP))
    }

    fun signThreatData(privateKeyObj: DilithiumPrivateKeyParameters, payload: ByteArray): String {
        val signer = DilithiumSigner().apply { 
            init(true, ParametersWithRandom(privateKeyObj, random)) 
        }
        return Base64.encodeToString(signer.generateSignature(payload), Base64.NO_WRAP)
    }

    fun verifySwarmSignature(publicKeyObj: DilithiumPublicKeyParameters, payload: ByteArray, base64Signature: String): Boolean {
        return try {
            val signature = Base64.decode(base64Signature, Base64.NO_WRAP)
            val result = DilithiumSigner().apply { init(false, publicKeyObj) }.verifySignature(payload, signature)
            Arrays.fill(signature, 0.toByte()) 
            result
        } catch (e: Exception) { false }
    }
}
