package com.juaris.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * JUARIS SECURITY LOG DAO
 * Bietet die kryptografisch gesicherten SQLite-Schnittstellen für Forensik-Protokolle.
 * Optimiert für Jetpack Compose Live-Zustände (Flow) und automatische Speicherbereinigung.
 */
@Dao
interface SecurityLogDao {

    // UI-Performance: Live-Konsole priorisiert 'BLOCKED'-Einträge und limitiert auf die neuesten 250
    @Query("""
        SELECT * FROM security_logs
        ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC
        LIMIT 250
    """)
    fun getAllLogs(): Flow<List<SecurityLogEntity>>

    // Live-Zähler für Alerts im Dashboard (z. B. für Badge-Anzeigen im UI)
    @Query("SELECT COUNT(*) FROM security_logs WHERE status IN ('BLOCKED', 'WARNING')")
    fun countAlerts(): Flow<Int>

    // Für den Deep-Scan im Hintergrund: Voller Zugriff auf das gesamte Archiv mit Priorisierung
    @Query("""
        SELECT * FROM security_logs 
        ORDER BY CASE status WHEN 'BLOCKED' THEN 1 ELSE 2 END ASC, timestamp DESC
    """)
    suspend fun getAllLogsSync(): List<SecurityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SecurityLogEntity)

    // Löscht im Hintergrund-Worker alle Protokolle, die älter als der Schwellenwert sind
    @Query("DELETE FROM security_logs WHERE timestamp < :expirationThreshold")
    suspend fun pruneOldLogs(expirationThreshold: Long)

    // Der ultimative Lockdown-Löschbefehl: Bereinigt die gesamte Tabelle im Fall einer Panik-Löschung
    @Query("DELETE FROM security_logs")
    suspend fun clearLogs()
}

