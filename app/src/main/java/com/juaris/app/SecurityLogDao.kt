package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {
    @Query("SELECT * FROM security_logs ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC")
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    // 👈 WICHTIG: Diese Methode hat im Interface gefehlt und wird vom Deep-Scan benötigt!
    @Query("SELECT * FROM security_logs ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC")
    suspend fun getAllLogsSync(): List<SecurityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLogEntity)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
