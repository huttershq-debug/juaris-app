package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshDao {

    // KORREKTUR 1: Logischen Fehler behoben! 'getAllActivePosts' lädt nun ECHT nur die 
    // Nachrichten, die entweder dauerhaft sind oder deren flüchtige Lebensdauer (TTL) 
    // im Verhältnis zur aktuellen Systemzeit noch nicht abgelaufen ist.
    @Query("""
        SELECT * FROM mesh_posts 
        WHERE isEphemeral = 0 OR (timestamp + (ttlHopCount * 60000)) > :currentTime 
        ORDER BY timestamp DESC
    """)
    fun getAllActivePosts(currentTime: Long): Flow<List<MeshPostEntity>>

    // Lädt das vollständige lokale Krypto-Archiv für Forensik-Logs
    @Query("SELECT * FROM mesh_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<MeshPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MeshPostEntity)

    // KORREKTUR 2: Die automatische dezentrale Müllabfuhr (Storage Pruning)!
    // Ermöglicht es dem Hintergrund-Worker der App, ephemere Daten rückstandslos zu vernichten.
    @Query("DELETE FROM mesh_posts WHERE isEphemeral = 1 AND (timestamp + (ttlHopCount * 60000)) <= :currentTime")
    suspend fun pruneExpiredEphemeralPosts(currentTime: Long)

    // Bereinigt die lokale Datenbank von extrem alten Posts (z.B. älter als 30 Tage),
    // um den Flash-Speicher des Handys im Dauereinsatz absolut schlank zu halten.
    @Query("DELETE FROM mesh_posts WHERE timestamp < :expirationThreshold")
    suspend fun pruneOldArchive(expirationThreshold: Long)

    // Der ultimative Panik-Löschbefehl: Wird im Fall eines "Stealth Lockdowns" getriggert,
    // um das dezentrale Mesh-Archiv sofort physikalisch zu nullen.
    @Query("DELETE FROM mesh_posts")
    suspend fun clearEntireMeshStorage()
}
