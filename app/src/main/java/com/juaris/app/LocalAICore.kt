package com.juaris.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalAICore(private val context: Context) {

    private val _aiStatus = MutableStateFlow("On-Device Intent Engine: Aktiv")
    val aiStatus: StateFlow<String> = _aiStatus

    private val _threatLevel = MutableStateFlow(0)
    val threatLevel: StateFlow<Int> = _threatLevel

    fun evaluateContentSafety(text: String, sender: String?): Boolean {
        val normalized = normalizeAndClean(text)
        
        // 1. Psychologischer Druck & Panik-Faktor (Urgency)
        val urgencyKeywords = listOf("sofort", "heute noch", "frist", "drohung", "sperrung", "letzte warnung", "sofortiges handeln")
        // 2. Autoritäts-Imitierung (Authority Mimicry)
        val authorityKeywords = listOf("zoll", "polizei", "gericht", "finanzamt", "bank", "post", "dhl", "netflix", "microsoft")
        // 3. Handlungsfallen (Action Traps - Links / Datenabfrage)
        val actionKeywords = listOf("http://", "https://", "bit.ly/", "tinyurl", "login", "bestaetigen", "verifizieren", "daten eingeben", "passwort")

        var score = 0

        // Gewichtete psychologische Vektor-Analyse
        if (urgencyKeywords.any { normalized.contains(it) }) score += 35
        if (authorityKeywords.any { normalized.contains(it) }) score += 25
        if (actionKeywords.any { normalized.contains(it) }) score += 40

        // Social Engineering Kombi-Boost (Druck + Autorität = Höchste Alarmstufe)
        if (urgencyKeywords.any { normalized.contains(it) } && authorityKeywords.any { normalized.contains(it) }) {
            score += 25
        }

        val finalScore = score.coerceAtMost(100)
        _threatLevel.value = finalScore

        if (finalScore >= 60) {
            _aiStatus.value = "Social Engineering / Phishing erkannt (Score: $finalScore)"
            return false // Bedrohung blockieren!
        }

        _aiStatus.value = "Kontext verifiziert (Score: $finalScore)"
        return true
    }

    private fun normalizeAndClean(input: String): String {
        return input.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("1", "i")
            .replace("3", "e")
            .replace("@", "a")
            .replace("$", "s")
            .replace(Regex("[^a-zäöüß0-9\\s]"), "")
    }
}


