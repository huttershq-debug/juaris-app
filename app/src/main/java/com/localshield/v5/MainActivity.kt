package com.localshield.v5

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.localshield.v5.email.EmailScanWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this)
        textView.text = "🛡️ LocalShield läuft erfolgreich!"
        textView.textSize = 20f
        setContentView(textView)

        // --- Lokalen E-Mail-Hintergrund-Scan starten ---
        startEmailBackgroundScan()
    }

    private fun startEmailBackgroundScan() {
        // Plant einen periodischen Scan ein (z. B. alle 30 Minuten auf dem S23)
        val emailWorkRequest = PeriodicWorkRequestBuilder<EmailScanWorker>(
            30, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueue(emailWorkRequest)
    }
}
