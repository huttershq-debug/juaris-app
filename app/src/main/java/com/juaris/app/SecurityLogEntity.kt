package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val module: String,
    val description: String,
    val status: String,
    val details: String = "" // Das hat im Worker gefehlt und wird hier ergänzt
)

