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
        val stripped = normalized.replace(" ", "") // Fängt auch "B a n k" oder "K-o-n-t-o" ab

        // 1. Psychologischer Druck & Panik-Faktor (Urgency)
        val urgencyKeywords = listOf("sofort", "heute noch", "frist", "drohung", "sperrung", "letzte warnung", "sofortiges handeln")
        // 2. Autoritäts-Imitierung (Authority Mimicry)
        val authorityKeywords = listOf("zoll", "polizei", "gericht", "finanzamt", "bank", "post", "dhl", "netflix", "microsoft")
        // 3. Handlungsfallen & Links (Action Traps)
        val actionKeywords = listOf("http://", "https://", "bit.ly", "tinyurl", "login", "bestaetigen", "verifizieren", "daten eingeben", "passwort")

        var score = 0

        // Gewichtete psychologische Vektor-Analyse (Prüfung auf normalisiert & zeichenbefreit)
        if (urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }) score += 35
        if (authorityKeywords.any { normalized.contains(it) || stripped.contains(it) }) score += 25
        if (actionKeywords.any { normalized.contains(it) || stripped.contains(it) }) score += 40

        // 4. NEU: Finanzielles Diebesgut-Pattern (IBAN oder Krypto-Addressen im Text)
        val hasIbanPattern = Regex("[a-z]{2}\\d{2}[a-z0-9]{11,30}").containsMatchIn(stripped)
        val hasCryptoPattern = Regex("(bc1|[13])[a-km-zA-HJ-NP-Z1-9]{25,39}").containsMatchIn(stripped) // Bitcoin/Crypto Wallet Check
        if (hasIbanPattern || hasCryptoPattern) {
            score += 45 // Extrem hoher Indikator für Finanzbetrug!
        }

        // Social Engineering Kombi-Boost (Druck + Autorität = Höchste Alarmstufe)
        val hasUrgency = urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }
        val hasAuthority = authorityKeywords.any { normalized.contains(it) || stripped.contains(it) }
        if (hasUrgency && hasAuthority) {
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
            .replace(Regex("[^a-zäöüß0-9\\s]"), " ") // Ersetzt Sonderzeichen durch Leerzeichen statt sie zu löschen (verhindert Wortverschmelzung)
    }
}

