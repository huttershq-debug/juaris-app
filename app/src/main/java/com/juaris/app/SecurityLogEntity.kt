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
    val details: String // Detaillierte technische Analyse (Verschlüsselt via SQLCipher auf dem Festspeicher)
) {
    
    // KORREKTUR FÜR DIE ABSOLUTE WELTSPITZE (Anti-Leak-Sicherung):
    // Wir überschreiben die standardmäßige 'toString()'-Methode der Datenklasse.
    // Das verhindert effektiv, dass sensible Log-Details (wie mitgelesene SMS oder Kalender-Inhalte)
    // durch unbedachte 'Log.d()'-Aufrufe des Entwicklers im unverschlüsselten Android Logcat-System-Protokoll
    // landen, wo andere Apps sie theoretisch mitlesen könnten!
    override fun toString(): String {
        return "JuarisSecurityLog(id=$id, timestamp=$timestamp, status='$status', module='$module', description='$description')"
    }
}
