package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_posts")
data class MeshPostEntity(
    @PrimaryKey
    val postId: String, // Echte SHA-256 Signatur des Pakets
    val senderNode: String, // Absender-ID (z.B. "Node-AT-2700")
    val content: String, // Der echte Textinhalt der Nachricht
    val mediaUri: String?, // Pfad für lokale Fotos oder Videos (Nullbar für reine Textnachrichten)
    val mediaType: String, // "TEXT", "IMAGE" oder "VIDEO"
    val timestamp: Long,
    val isEphemeral: Boolean, // True = Ephemerer Selbstzerstörungs-Modus
    val ttlHopCount: Int, // Verbleibende Weiterleitungen im Schwarm
    
    // NEU & KRYPTOGRAFISCH SAUBER: Dedizierte Felder für die Post-Quanten-Sicherheit!
    val pqcSignature: String, // Speichert die Dilithium-Signatur
    val pqcPublicKey: String // Speichert den Public Key zur dezentralen Verifizierung
)

