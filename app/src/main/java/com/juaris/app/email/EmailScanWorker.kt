package com.juaris.app.email

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.juaris.app.LocalPhishingAnalyzer

class EmailScanWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
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
