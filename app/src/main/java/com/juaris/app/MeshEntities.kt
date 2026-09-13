package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mesh_posts")
data class MeshPostEntity(
    @PrimaryKey
    val postId: String, // SHA-256 Signatur des Posts
    val senderNode: String, // Absender-ID (z.B. "Node-AT-2700")
    val content: String, // Inhalt oder Bild-Untertitel
    val mediaUri: String?, // NEU: Pfad für lokale Fotos oder Videos
    val mediaType: String, // NEU: "TEXT", "IMAGE" oder "VIDEO"
    val timestamp: Long,
    val isEphemeral: Boolean, // True = Snapchat-Modus (Selbstzerstörung)
    val ttlHopCount: Int // Verbleibende Hops im globalen Schwarm
)

@Dao
interface MeshDao {
    @Query("SELECT * FROM mesh_posts ORDER BY timestamp DESC")
    fun getAllMeshPosts(): Flow<List<MeshPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MeshPostEntity)

    @Query("DELETE FROM mesh_posts WHERE timestamp < :expirationTime OR ttlHopCount <= 0")
    suspend fun purgeExpiredPosts(expirationTime: Long)
}

