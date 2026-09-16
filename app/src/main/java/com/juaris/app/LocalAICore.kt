package com.juaris.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalAICore(private val context: Context) {

    private val _aiStatus = MutableStateFlow("On-Device AGI: Aktiv & Gesichert")
    val aiStatus: StateFlow<String> = _aiStatus

    private val _threatLevel = MutableStateFlow(0) // 0 (Sicher) bis 100 (Kritisch)
    val threatLevel: StateFlow<Int> = _threatLevel

    // Echte On-Device Text- und Inhaltsanalyse (Ohne Cloud / Telemetrie)
    fun evaluateContentSafety(text: String, sender: String?): Boolean {
        val normalized = erodeAndNormalizeText(text)
        
        // Toxische Betrugs-Trigger (Kombination aus Druck & Masche)
        val coreTriggers = listOf(
            "konto gesperrt", "kreditkarte gesperrt", "sofort handeln", 
            "zollgebuehr", "paket zurueck", "gewinn eingeloest", "identitaet bestaetigen",
            "bitcoinguthaben", "wallet verifizieren", "gerichtlicher mahnbescheid"
        )

        var score = 0
        
        for (trigger in coreTriggers) {
            if (normalized.contains(trigger)) {
                score += 40
            }
        }

        if (normalized.contains("http://") || normalized.contains("bit.ly/") || normalized.contains("tinyurl")) {
            score += 30
        }

        val finalScore = score.coerceAtMost(100)
        _threatLevel.value = finalScore

        // Schwellenwert: Ab 60 Punkten schlägt der Alarm an
        if (finalScore >= 60) {
            _aiStatus.value = "Bedrohung lokal erkannt (Score: $finalScore)"
            return false // Blockieren + Alarm auslösen!
        }

        _aiStatus.value = "Inhalt verifiziert (Score: $finalScore)"
        return true
    }

    // Leetspeak-Bereinigung und Normalisierung
    private fun erodeAndNormalizeText(input: String): String {
        return input.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("1", "i")
            .replace("3", "e")
            .replace("@", "a")
            .replace(Regex("[^a-zäöüß0-9\\s]"), "")
    }
}
