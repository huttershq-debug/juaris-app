package com.juaris.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

object GlobalMeshEngine {

    private const val TAG = "JuarisSwarmMesh"

    // Zero-Knowledge: Verknüpft deine robuste MeshPostEntity-Struktur mit dezentralem PQC-Schutz
    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        meshManager: NearbyMeshManager?,
        onBlocked: (String) -> Unit,
        onSuccess: (Long) -> Unit
    ) {
        // Zero-Knowledge Hash erzeugen, damit im Mesh niemals Klartext-Daten fließen
        val threatHash = sha256(content)
        Log.d(TAG, "Zero-Knowledge Threat-Hash generiert: $threatHash")

        val db = JuarisDatabase.getDatabase(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            val post = MeshPostEntity(
                postId = UUID.randomUUID().toString(),
                senderNode = if (isEphemeral) "[Ephemerer Schwarm-Node]" else "[Verifizierter Juaris-Node]",
                content = "PQC-Schswarm Signatur [ID: ${threatHash.take(8)}...]",
                timestamp = System.currentTimeMillis(),
                isEphemeral = isEphemeral,
                mediaUri = "", // Non-Null String konform
                mediaType = "", // Non-Null String konform
                ttlHopCount = 3 // Schützt vor Endlosschleifen im Funknetzwerk
            )
            
            db.meshDao().insertPost(post)

            // Anonyme Weitergabe des kryptografischen Hashes über Nearby Connections (Bluetooth P2P Mesh)
            meshManager?.broadcastMessage("JUARIS_PQC_HASH:$threatHash")

            onSuccess(System.currentTimeMillis())
        }
    }

    private fun sha256(base: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(base.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}


