package com.juaris.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            try {
                // KORREKTUR 1: Perfekt synchronisiert mit den neuen Krypto-Feldern deines Datenmodells!
                val post = MeshPostEntity(
                    postId = UUID.randomUUID().toString(),
                    senderNode = if (isEphemeral) "[Ephemerer PQC-Node]" else "[Verifizierter PQC-Node]",
                    content = content, // Speichert das, was der Nutzer ECHT geschrieben hat!
                    timestamp = System.currentTimeMillis(),
                    isEphemeral = isEphemeral,
                    mediaUri = null, 
                    mediaType = "TEXT",
                    ttlHopCount = 3,
                    pqcSignature = signatureBase64, // Dedizierte Signatur-Sicherung
                    pqcPublicKey = publicKeyBase64 // Dedizierte Schlüssel-Sicherung
                )
               
                db.meshDao().insertPost(post)

                // P2P-Broadcast: Überträgt das verpackte Post-Quantum-Sicherheitspaket ins Bluetooth-Mesh
                val meshPacket = "JUARIS_PQC_SECURE:$signatureBase64:$publicKeyBase64:$content"
                meshManager?.broadcastMessage(meshPacket)

                // UI-Callback sicher zurück auf den Hauptthread melden
                withContext(Dispatchers.Main) {
                    onSuccess(System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fehler in der Swarm-Engine: ${e.message}")
            }
        } // KORREKTUR 2: Schließt die Coroutine-Schleife sauber ab!
    } // KORREKTUR 3: Schließt die broadcastToSwarm-Funktion regelkonform!
} // KORREKTUR 4: Schließt das GlobalMeshEngine-Objekt fehlerfrei!


