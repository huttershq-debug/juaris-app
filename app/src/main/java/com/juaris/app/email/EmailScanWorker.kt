package com.juaris.app.email

import android.content.Context
import com.juaris.app.QuantumEngine
import com.juaris.app.SecurityLogDao
import com.juaris.app.SecurityLogEntity
import java.util.Date

class EmailScanWorker(private val context: Context, private val logDao: SecurityLogDao) {

    // Scannend den E-Mail-Inhalt lokal und speichert Bedrohungen unbestechlich in der Room-DB
    suspend fun scanAndPersistEmail(sender: String, subject: String, bodySnippet: String): Boolean {
        // Erzeugt eine lokale kryptografische Signatur (Quanten-Hash)
        val threatSignature = QuantumEngine.generateThreatSignature("$sender:$subject")

         // HIER DIE NEUE UNHACKBARE FUZZY-LOGIK EINBAUEN:
        val phishingAnalyzer = LocalPhishingAnalyzer()
        val textToAnalyze = "$subject $bodySnippet"
        val analysisResult = phishingAnalyzer.analyzeText(textToAnalyze)
        
        val isPhishingThreat = analysisResult.isSuspicious

        if (isPhishingThreat) {
            // Sofortiger Eintrag in die lokale SQLite-Datenbank für den Logs-Tab
            val logEntry = SecurityLogEntity(
                timestamp = Date().time,
                module = "EMAIL_SCANNER",
                description = "Phishing-Verdacht erkannt!",
                status = "BLOCKED",
                details = "Absender: $sender | Betreff: $subject | Sig: ${threatSignature.take(16)}..."
            )
            logDao.insertLog(logEntry)
        }

        return isPhishingThreat
    }
}

