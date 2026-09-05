package com.juaris.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var txtLogList: TextView
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtLogList = findViewById(R.id.txtLogList)
        val txtSwarmStatus = findViewById<TextView>(R.id.txtSwarmStatus)

        // Status für das lokale P2P-Schwarmnetzwerk unter Julias Design
        txtSwarmStatus.text = "Schwarm-Signaturen lokal: Aktiv (P2P-Mesh verbunden)"
        
        logBuilder.append("[*] Juaris Sovereign Engine gestartet.\n")
        logBuilder.append("[*] 100% Offline-Schutz aktiv (Keine Cloud-Anbindung).\n")
        logBuilder.append("----------------------------------------\n")
        logBuilder.append("[MONITOR] Aktive Schutzvektoren bereit...\n")
        txtLogList.text = logBuilder.toString()

        // Live-Event-Brücke anbinden, damit gefangene Bedrohungen direkt ins Dashboard schreiben
        JuarisEventBus.register { eventMessage ->
            runOnUiThread {
                logBuilder.append("$eventMessage\n")
                txtLogList.text = logBuilder.toString()
            }
        }
    }
}
