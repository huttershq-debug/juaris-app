package com.juaris.app.email

import android.content.Context
import com.juaris.app.QuantumEngine
import com.juaris.app.SecurityLogDao
import com.juaris.app.SecurityLogEntity
import java.util.Date

class EmailScanWorker(private val context: Context, private val logDao: SecurityLogDao) {

    // Scannt den E-Mail-Inhalt lokal und speichert Bedrohungen unbestechlich in der Room-DB
    suspend fun scanAndPersistEmail(sender: String, subject: String, bodySnippet: String): Boolean {
        // Erzeugt eine lokale kryptografische Signatur (Quanten-Hash)
        val threatSignature = QuantumEngine.generateThreatSignature("$sender:$subject")

        // Erweiterte Offline-Heuristik (Prüfung auf Betrugs- und Phishing-Muster)
        val isPhishingThreat = subject.contains("Invoice", ignoreCase = true) ||
                               subject.contains("Bank", ignoreCase = true) ||
                               subject.contains("Rechnung", ignoreCase = true) ||
                               subject.contains("Konto", ignoreCase = true) ||
                               subject.contains("Überweisung", ignoreCase = true) ||
                               subject.contains("Security Alert", ignoreCase = true) ||
                               subject.contains("Sicherheitswarnung", ignoreCase = true) ||
                               bodySnippet.contains("http://", ignoreCase = true) // Unsicherer Link in Mail

        if (isPhishingThreat) {
            // Sofortiger Eintrag in die lokale SQLite-Datenbank für den Logs-Tab
            val logEntry = SecurityLogEntity(
                timestamp = Date().time,
                threatType = "HIGH_RISK_EMAIL_PHISHING",
                details = "Absender: $sender | Betreff: $subject | Signatur: ${threatSignature.take(16)}..."
            )
            logDao.insertLog(logEntry)
        }

        return isPhishingThreat
    }
}
