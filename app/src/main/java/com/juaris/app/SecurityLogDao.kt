package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {

    @Query("SELECT * FROM security_logs ORDER BY timestamp DESC LIMIT 250")
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    @Query("SELECT COUNT(*) FROM security_logs WHERE status IN ('BLOCKED', 'WARNING')")
    fun countAlerts(): Flow<Int>

    @Insert
    suspend fun insertLog(log: SecurityLogEntity)

    @Query("DELETE FROM security_logs WHERE timestamp < :threshold")
    suspend fun pruneOldLogs(threshold: Long)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
