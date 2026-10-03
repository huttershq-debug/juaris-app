package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {

    // UI-Performance: Live-Konsole auf die neuesten 250 Einträge limitiert
    @Query("""
        SELECT * FROM security_logs 
        ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC 
        LIMIT 250
    """)
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    // Für den Deep-Scan im Hintergrund: Voller Zugriff auf das gesamte Archiv
    @Query("SELECT * FROM security_logs ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC")
    suspend fun getAllLogsSync(): List<SecurityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLogEntity)

    // Storage Pruning: Automatische Müllabfuhr für alte Logs im Hintergrund-Worker
    @Query("SELECT * FROM security_logs WHERE timestamp < :expirationThreshold") // wait, let's keep the user's exact query
