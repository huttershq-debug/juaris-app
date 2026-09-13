package com.juaris.app

import android.content.Context
import java.util.UUID

object GlobalMeshEngine {
    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        onBlocked: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        // Hier wird die lokale Broadcast-ID generiert
        val packetId = UUID.randomUUID().toString()
        onSuccess(packetId)
    }
}

