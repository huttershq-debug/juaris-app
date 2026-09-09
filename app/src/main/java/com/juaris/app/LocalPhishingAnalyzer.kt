package com.juaris.app

import android.content.Context

class LocalPhishingAnalyzer(private val context: Context? = null) {

    private val urgencyTriggers = listOf(
        "konto gesperrt", "sofort handeln", "verifizierung", "aktualisieren",
        "gewinn", "überweisung", "sicherheit", "warnung", "krypto"
    )

    fun analyze(content: String): Boolean {
        return analyzeText(content).isSuspicious
    }

    fun analyzeText(rawText: String): PhishingResult {
        val cleanedText = erodeAndNormalizeText(rawText)
       
        var score = 0
        val detectedTriggers = mutableListOf<String>()

        // 2. FUZZY-MATCHING: Prüft auf semantische Nähe zu den Mustern
        for (trigger in urgencyTriggers) {
            if (cleanedText.contains(trigger)) {
                score += 25
                detectedTriggers.add(trigger)
            } else {
                // Erwischt auch leichte Abwandlungen
                if (levenshteinDistance(cleanedText, trigger) < 3) {
                    score += 15
                    detectedTriggers.add(trigger)
                }
            }
        }

        // Sicherheitsfaktor für unverschlüsselte Links
        if (rawText.contains("http://", ignoreCase = true)) {
            score += 30
        }

        val isSuspicious = score >= 40
        val recommendation = if (isSuspicious) {
            "Achtung! Lokale KI erkennt psychologische Manipulationstaktiken. Keine Links öffnen!"
        } else {
            "Inhalt unbedenklich."
        }

        return PhishingResult(isSuspicious, score, recommendation)
    }

    // Filtert Leetspeak (0 -> o, 4 -> a, @ -> a) und jegliche Sonderzeichen-Tricks heraus
    private fun erodeAndNormalizeText(text: String): String {
        val normalized = text.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("@", "a")
            .replace("$", "s")
            .replace("9", "g")
           
        // Entfernt alle Leerzeichen und Trennzeichen, um "k-o-n-t-o" zu "konto" zu verschmelzen
        return normalized.replace(Regex("[^a-z]"), "")
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        val prev = IntArray(s2.length + 1) { it }
        val curr = IntArray(s2.length + 1)
        for (i in s1.indices) {
            curr[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[s2.length]
    }

    data class PhishingResult(
        val isSuspicious: Boolean,
        val score: Int,
        val recommendation: String
    )
}
