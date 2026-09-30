package com.juaris.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object GlobalMeshEngine {

    private const val TAG = "JuarisSwarmMesh"
    
    // Instanziiert deine echte Post-Quantum-Engine
    private val quantumEngine = QuantumEngine()
    
    // Generiert beim Start des Engines ein sicheres Dilithium-Schlüsselpaar für diesen Node
    private val nodeKeyPair = quantumEngine.generatePostQuantumKeyPair()

    // Signiert und verteilt Bedrohungsdaten post-quantum-resistent im P2P-Mesh
    fun broadcastToSwarm(
        context: Context,
        content: String,
        isEphemeral: Boolean,
        meshManager: NearbyMeshManager?,
        onBlocked: (String) -> Unit,
        onSuccess: (Long) -> Unit
    ) {
        Log.d(TAG, "Generiere post-quantum-resistente Dilithium-Signatur für den Schwarm...")

        val payloadBytes = content.toByteArray(Charsets.UTF_8)
        
        // Echte Dilithium-Signatur mit deiner QuantumEngine erzeugen
        val signatureBase64 = quantumEngine.signThreatData(nodeKeyPair.privateKeyObj, payloadBytes)
        val publicKeyBase64 = nodeKeyPair.publicKeyBase64

        Log.d(TAG, "Dilithium-Signatur erfolgreich erstellt: ${signatureBase64.take(12)}...")

         val db = JuarisDatabase.getDatabase(context)
       
        CoroutineScope(Dispatchers.IO).launch {
            // Speicherung des ECHTEN Inhalts in deiner Room-Datenbankstruktur
            val post = MeshPostEntity(
                postId = UUID.randomUUID().toString(),
                senderNode = if (isEphemeral) "[Ephemerer PQC-Node]" else "[Verifizierter PQC-Node]",
                content = content, // Speichert das, was der Nutzer ECHT geschrieben hat!
                timestamp = System.currentTimeMillis(),
                isEphemeral = isEphemeral,
                mediaUri = "PQC_SIG:$signatureBase64", // Nutze freie Felder für die Signatur-Sicherung
                mediaType = "PQC_KEY:$publicKeyBase64",
                ttlHopCount = 3
            )
           
            db.meshDao().insertPost(post)

            // P2P-Broadcast: Überträgt NUR das verpackte Post-Quantum-Sicherheitspaket!
            val meshPacket = "JUARIS_PQC_SECURE:$signatureBase64:$publicKeyBase64:$content"
            meshManager?.broadcastMessage(meshPacket)

            // UI-Callback sicher auf dem Hauptthread ausführen
            withContext(Dispatchers.Main) {
                onSuccess(System.currentTimeMillis())
            }
        }
    }

