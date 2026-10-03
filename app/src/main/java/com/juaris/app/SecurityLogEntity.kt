package com.juaris.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "security_logs")
data class SecurityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val status: String, // "SAFE", "WARNING", "IMPORTANT", "BLOCKED"
    val module: String, // Welches Schutzmodul hat das Log erzeugt
    val description: String, // Kurzbeschreibung des Vorfalls
    val details: String // Detaillierte technische Analyse (Verschlüsselt auf dem Festspeicher)
) {
    
    // Anti-Leak-Sicherung: Verhindert das Auslesen sensibler Details im Android Logcat-Systemprotokoll
    override fun toString(): String {
        return "JuarisSecurityLog(id=$id, timestamp=$timestamp, status='$status', module='$module', description='$description')"
    }
}
