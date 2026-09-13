package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshDao {

    @Query("SELECT * FROM mesh_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<MeshPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MeshPostEntity)
}

