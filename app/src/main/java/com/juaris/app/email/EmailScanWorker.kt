package com.juaris.app.email

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.juaris.app.SecurityEngine
import com.juaris.app.EmailSecurityResult

class EmailScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val sender = inputData.getString("sender") ?: "unbekannt"
            val subject = inputData.getString("subject") ?: ""
            val body = inputData.getString("body") ?: ""

            // Lokaler Offline-Scan der E-Mail über die SecurityEngine
            val result = SecurityEngine.analyzeIncomingEmail(sender, subject, body)

            if (result == EmailSecurityResult.BLOCK) {
                Result.failure()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
