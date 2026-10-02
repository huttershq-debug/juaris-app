package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityLogDao {

    // KORREKTUR 1 (UI-Performance): Wir limitieren die Live-Konsole auf die neuesten 250 Einträge.
    // Das garantiert eine blitzschnelle, ruckelfreie UI-Anzeige im Jetpack Compose Dashboard –
    // selbst wenn im Hintergrund zehntausende Logs in der Tabelle existieren!
    @Query("""
        SELECT * FROM security_logs 
        ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC 
        LIMIT 250
    """)
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    // Für den Deep-Scan im Hintergrund bleibt der volle Zugriff auf das gesamte Archiv intakt
    @Query("SELECT * FROM security_logs ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC")
    suspend fun getAllLogsSync(): List<SecurityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLogEntity)

    // KORREKTUR 2 (Storage Pruning): Automatische Müllabfuhr für die System-Stabilität.
    // Löscht im Hintergrund-Worker (z. B. jede Nacht) alle Protokolle, die älter als der 
    // übergebene Schwellenwert (Threshold) sind. Das hält den Handyspeicher dauerhaft schlank.
    @Query("DELETE FROM security_logs WHERE timestamp < :expirationThreshold")
    suspend fun pruneOldLogs(expirationThreshold: Long)

    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}
