package com.juaris.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GlobalMeshEngine {
    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        meshManager: NearbyMeshManager?,
        onBlocked: (String) -> Unit,
        onSuccess: (Long) -> Unit
    ) {
        val db = JuarisDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val post = MeshPostEntity(
                senderNode = "LocalDevice",
                content = content,
                timestamp = System.currentTimeMillis(),
                isEphemeral = isEphemeral,
                mediaUri = null,
                mediaType = null,
                ttlHopCount = 3 // Standard-Hop-Count für das Mesh-Netzwerk
            )
            db.meshDao().insertPost(post)
            onSuccess(System.currentTimeMillis())
        }
    }
}
