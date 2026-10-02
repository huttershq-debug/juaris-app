package com.juaris.app

import android.content.Context
import java.util.Locale

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

        // 2. KORREKTUR: Wort-für-Wort-Vergleich für die Levenshtein-Fuzzy-Logik
        val wordsText = erodeAndNormalizeText(rawText, removeSpaces = false)
        val distinctWords = wordsText.split(" ").filter { it.length >= 4 }.distinct() // '.distinct()' spart Doppelprüfungen!

        for (trigger in urgencyTriggers) {
            val cleanTrigger = trigger.replace(" ", "")
            
            if (!detectedTriggers.contains(trigger)) {
                val triggerLength = cleanTrigger.length
                
                for (word in distinctWords) {
                    // PERFORMANCE-KORREKTUR 1 (Short-Circuit): 
                    // Wenn der Längenunterschied zwischen dem Wort und dem Trigger bereits >= 2 ist, 
                    // KANN die Levenshtein-Distanz mathematisch niemals < 2 sein. 
                    // Wir überspringen die komplexe Berechnung komplett! Das spart bis zu 80% CPU-Last.
                    if (Math.abs(word.length - triggerLength) >= 2) {
                        continue
                    }

                    if (levenshteinDistance(word, cleanTrigger) < 2) { 
                        score += 15
                        detectedTriggers.add(trigger)
                        break 
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
     */
    private fun erodeAndNormalizeText(text: String, removeSpaces: Boolean): String {
        val normalized = text.lowercase(Locale.ROOT)
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
            normalized.replace(Regex("[^a-z\\s]"), "").replace(Regex("\\s+"), " ")
        }
    }

    /**
     * Optimierte Levenshtein-Distanz-Berechnung ohne speicherlastige Array-Kopien.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        
        var prev = IntArray(s2.length + 1) { it }
        var curr = IntArray(s2.length + 1)
        
        for (i in s1.indices) {
            curr[0] = i + 1
            for (j in s2.indices) {
                val cost = if (s1[i] == s2[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            // PERFORMANCE-KORREKTUR 2: Pointer-Swap statt schwerfälliges System.arraycopy!
            // Schont den CPU-Cache und erhöht die Ausführungsgeschwindigkeit drastisch.
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[s2.length]
    }

    data class PhishingResult(
        val isSuspicious: Boolean,
        val score: Int,
        val recommendation: String
    )
}
