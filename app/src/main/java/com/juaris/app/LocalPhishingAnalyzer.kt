package com.juaris.app

import android.content.Context

class LocalPhishingAnalyzer(private val context: Context? = null) {

    private val urgencyTriggers = listOf(
        "konto gesperrt", "sofort handeln", "verifizierung", "aktualisieren",
        "gewinn", "ueberweisung", "sicherheit", "warnung", "krypto", "konto"
    )

    fun analyze(content: String): Boolean {
        return analyzeText(content).isSuspicious
    }

    fun analyzeText(rawText: String): PhishingResult {
        // 1. Gesamte Erosion für den direkten "contains"-Check (erwischt k-o-n-t-o)
        val fullCleanedText = erodeAndNormalizeText(rawText, removeSpaces = true)
       
        var score = 0
        val detectedTriggers = mutableListOf<String>()

        // Direkter globaler Muster-Check
        for (trigger in urgencyTriggers) {
            val cleanTrigger = trigger.replace(" ", "")
            if (fullCleanedText.contains(cleanTrigger)) {
                score += 25
                detectedTriggers.add(trigger)
            }
        }

        // 2. KORREKTUR: Wort-für-Wort-Vergleich für die Levenshtein-Fuzzy-Logik (erwischt Tippfehler wie krypTo)
        // Hier behalten wir die Leerzeichen, um die Nachricht in einzelne Wörter zu splitten
        val wordsText = erodeAndNormalizeText(rawText, removeSpaces = false)
        val distinctWords = wordsText.split(" ").filter { it.length >= 4 }

        for (trigger in urgencyTriggers) {
            val cleanTrigger = trigger.replace(" ", "")
            // Wenn der globale Check nicht schon angeschlagen hat, prüfen wir die einzelnen Wörter per Fuzzy-Logik
            if (!detectedTriggers.contains(trigger)) {
                for (word in distinctWords) {
                    if (levenshteinDistance(word, cleanTrigger) < 2) { // Max. 1 Buchstabe Unterschied bei Wörtern
                        score += 15
                        detectedTriggers.add(trigger)
                        break // Ein Treffer pro Trigger reicht
                    }
                }
            }
        }

        // Sicherheitsfaktor für unverschlüsselte, gefährliche Links
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

    /**
     * Filtert Leetspeak (0 -> o, 4 -> a, @ -> a) und jegliche Sonderzeichen-Tricks heraus.
     * Flexibel steuerbar, ob Leerzeichen entfernt werden sollen oder nicht.
     */
    private fun erodeAndNormalizeText(text: String, removeSpaces: Boolean): String {
        val normalized = text.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("@", "a")
            .replace("$", "s")
            .replace("9", "g")
            .replace("ü", "ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
           
        return if (removeSpaces) {
            normalized.replace(Regex("[^a-z]"), "")
        } else {
            // Behält einzelne Leerzeichen zwischen den Wörtern für den Split, entfernt aber doppelte Leerzeichen und Sonderzeichen
            normalized.replace(Regex("[^a-z\\s]"), "").replace(Regex("\\s+"), " ")
        }
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


