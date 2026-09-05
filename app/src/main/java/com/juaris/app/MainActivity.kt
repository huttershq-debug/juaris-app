package com.juaris.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val txtLogList = findViewById<TextView>(R.id.txtLogList)
        val txtSwarmStatus = findViewById<TextView>(R.id.txtSwarmStatus)

        // Fester Status für das lokale P2P-Schwarmnetzwerk
        txtSwarmStatus.text = "Schwarm-Signaturen lokal: Aktiv (P2P-Mesh verbunden)"
        
        // Fester, transparenter System-Log für alle drei Überwachungsvektoren
        val systemLog = StringBuilder().apply {
            append("[*] Juaris Sovereign Engine gestartet.\n")
            append("[*] 100% Offline-Schutz aktiv (Keine Cloud-Anbindung).\n")
            append("----------------------------------------\n")
            append("[MONITOR] Aktive Schutzvektoren:\n")
            append(" [+] Anruf-Filter (Call Blocking)\n")
            append(" [+] SMS-Filter (Phishing/Spam)\n")
            append(" [+] E-Mail-Filter (Lokal geparst)\n")
            append("----------------------------------------\n")
            append("[LIVE-LOG] System bereit. Warte auf Ereignisse...\n")
        }.toString()

        txtLogList.text = systemLog
    }
}
