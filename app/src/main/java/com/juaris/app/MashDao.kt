package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshDao {

    // Lädt nur Nachrichten, die dauerhaft sind oder deren flüchtige Lebensdauer (TTL) noch nicht abgelaufen ist
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

    // Die automatische dezentrale Müllabfuhr (Storage Pruning) für ephemere Daten
    @Query("DELETE FROM mesh_posts WHERE isEphemeral = 1 AND (timestamp + (ttlHopCount * 60000)) <= :currentTime")
    suspend fun pruneExpiredEphemeralPosts(currentTime: Long)

    // Bereinigt die lokale Datenbank von extrem alten Posts (z.B. älter als 30 Tage)
    @Query("DELETE FROM mesh_posts WHERE timestamp < :expirationThreshold")
    suspend fun pruneOldArchive(expirationThreshold: Long)

    // Der ultimative Panik-Löschbefehl: Triggert den "Stealth Lockdown" zum Nullen des Mesh-Archivs
    @Query("DELETE FROM mesh_posts")
    suspend fun clearEntireMeshStorage()
}
