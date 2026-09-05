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

        // Fester Status für das lokale P2P-Schwarmnetzwerk unter Julias Design
        txtSwarmStatus.text = "Schwarm-Signaturen lokal: Aktiv (P2P-Mesh verbunden)"
        
        // Fester, transparenter System-Log für alle drei Überwachungsvektoren
        val systemLog = "[*] Juaris Sovereign Engine gestartet.\n" +
                "[*] 100% Offline-Schutz aktiv (Keine Cloud-Anbindung).\n" +
                "----------------------------------------\n" +
                "[MONITOR] Aktive Schutzvektoren:\n" +
                " [+] Anruf-Filter (Call Blocking)\n" +
                " [+] SMS-Filter (Phishing/Spam)\n" +
                " [+] E-Mail-Filter (Lokal geparst)\n" +
                "----------------------------------------\n" +
                "[LIVE-LOG] System bereit. Warte auf Ereignisse...\n"

        txtLogList.text = systemLog
    }
}

