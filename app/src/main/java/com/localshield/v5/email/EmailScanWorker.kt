package com.localshield.v5.email

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.localshield.v5.ai.LocalPhishingAnalyzer
import android.util.Log

class EmailScanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Lokaler Hintergrund-Scan auf Phishing-Muster (komplett offline & serverless)
        val sampleMessage = "Ihr Konto wurde temporär gesperrt. Klicken Sie hier für die Verifizierung."
        val result = LocalPhishingAnalyzer.analyzeText(sampleMessage)

        if (result.isSuspicious) {
            Log.w("JuarisEmailScan", "Warnung: Verdächtiger Inhalt im Hintergrund erkannt! Score: ${result.riskScore}")
        } else {
            Log.i("JuarisEmailScan", "Hintergrund-Scan erfolgreich: Keine Bedrohung.")
        }

        return Result.success()
    }
}
