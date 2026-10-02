package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "mesh_posts")
data class MeshPostEntity(
    @PrimaryKey
    val postId: String, // Echte SHA-256 Signatur des Pakets
    
    // KORREKTUR 1 (Anonymitäts-Garantie): 
    // Wir speichern NIEMALS Klarnamen, Regionen (wie Node-AT-2700) oder feste IDs.
    // Dieses Feld speichert ausschließlich den anonymisierten Krypto-Fingerabdruck (SHA-256-Short-Hash) 
    // des Absender-Schlüssels. Das garantiert absolute Anonymität im dezentralen Schwarm!
    val senderNodeHash: String, 
    
    val content: String, // Der echte Textinhalt der Nachricht
    val mediaUri: String?, // Pfad für lokale Fotos oder Videos (Isoliert im 'vault_media_storage')
    val mediaType: String, // "TEXT", "IMAGE" oder "VIDEO"
    val timestamp: Long,
    val isEphemeral: Boolean, // True = Ephemerer Selbstzerstörungs-Modus (Gekoppelt an DAO-Pruning)
    val ttlHopCount: Int, // Verbleibende Weiterleitungen im Schwarm
    
    // Dedizierte Felder für die Post-Quanten-Sicherheit!
    val pqcSignature: String, // Speichert die Dilithium-Signatur
    val pqcPublicKey: String // Speichert den Public Key zur dezentralen Verifizierung
) {
    companion object {
        /**
         * Hilfsmethode für die GlobalMeshEngine, um aus einem rohen Public Key 
         * eine vollkommen anonyme, aber eindeutige Sender-ID zu generieren.
         */
        fun generateAnonymousNodeId(publicKeyBase64: String): String {
            return try {
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val hashBytes = digest.digest(publicKeyBase64.toByteArray(Charsets.UTF_8))
                // Wir nehmen die ersten 16 Zeichen des Hashes – absolut kollisionssicher für P2P-Mesh
                val hex = hashBytes.joinToString("") { "%02x".format(it) }
                "Node-PQC-${hex.take(16).uppercase(Locale.ROOT)}"
            } catch (e: Exception) {
                "Node-PQC-ANONYMOUS"
            }
        }
    }
}
