package com.juaris.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object GlobalMeshEngine {

    // Erzeugt einen quantensicheren SHA-256 Hash für das globale P2P-Paket
    fun generatePacketHash(payload: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    // Broadcastet einen neuen Post in den globalen Schwarm (Instagram/Snapchat-Hybrid)
    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        onSuccess: (String) -> Unit
    ) {
        val db = JuarisDatabase.getDatabase(context)
        val packetId = generatePacketHash(content + System.currentTimeMillis())
        val nodeName = "Juaris-Node-" + android.os.Build.MODEL

        val post = MeshPostEntity(
            postId = packetId,
            senderNode = nodeName,
            content = content,
            timestamp = System.currentTimeMillis(),
            isEphemeral = isEphemeral,
            ttlHopCount = 10 // Max 10 Hops durch den weltweiten Schwarm
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.meshDao().insertPost(post)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onSuccess(packetId)
            }
        }
    }
}

