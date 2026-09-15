package com.juaris.app

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.UUID

object GlobalMeshEngine {

    private val quantumEngine = QuantumEngine()
    private val keyPair = quantumEngine.generatePostQuantumKeyPair()

    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        meshManager: NearbyMeshManager?,
        onBlocked: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        val aiCore = LocalAICore(context)
        // Lokale AGI-Integritätsprüfung vor dem Broadcast
        if (!aiCore.evaluateContentSafety(content, null)) {
            onBlocked("On-Device AGI hat den Inhalt als manipuliert oder schädlich eingestuft.")
            return
        }

        val packetId = UUID.randomUUID().toString()
        val payloadBytes = content.toByteArray(StandardCharsets.UTF_8)
        
        // Post-Quantum Signatur mittels CRYSTALS-Dilithium erzeugen
        val signature = quantumEngine.signThreatData(keyPair.privateKeyObj, payloadBytes)
        val signedPayloadString = "$packetId::$signature::$content"

        // Tatsächlicher Versand über den NearbyMeshManager an echte Geräte
        meshManager?.broadcastMessage(signedPayloadString)

        // In die lokale Room-Datenbank schreiben
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = JuarisDatabase.getDatabase(context)
            val post = MeshPostEntity(
                postId = packetId,
                senderNode = "Node-${android.os.Build.MODEL}",
                content = content,
                mediaUri = null,
                mediaType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isEphemeral = isEphemeral,
                ttlHopCount = 5
            )
            db.meshDao().insertPost(post)
        }

        onSuccess(packetId)
    }
}
