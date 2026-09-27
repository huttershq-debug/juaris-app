package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_posts")
data class MeshPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val content: String
)
