package com.juaris.app

object LocalPhishingAnalyzer {

    // Typische psychologische Trigger für Phishing & Betrug (Druck, Angst, künstliche Dringlichkeit)
    private val urgencyTriggers = listOf(
        "sofort", "innerhalb von 24 stunden", "konto gesperrt", "klicken sie hier", 
        "gewinn", "erfolgreich", "verifizierung", "aktualisieren sie", "gerichtsvollzieher", "überprüfen sie"
    )

    data class AnalysisResult(
        val isSuspicious: Boolean,
        val riskScore: Int, // 0 bis 100
        val detectedTriggers: List<String>,
        val advice: String
    )

    fun analyzeText(inputMessage: String): AnalysisResult {
        val lowerCaseText = inputMessage.lowercase()
        val foundTriggers = mutableListOf<String>()
        
        for (trigger in urgencyTriggers) {
            if (lowerCaseText.contains(trigger)) {
                foundTriggers.add(trigger)
            }
        }

        val score = (foundTriggers.size * 35).coerceAtMost(100)
        val isSus = score >= 40

        val advice = if (isSus) {
            "Achtung! Diese Nachricht enthält typische psychologische Manipulationstasten. Keine Links anklicken!"
        } else {
            "Keine unmittelbaren Bedrohungsmuster erkannt."
        }

        return AnalysisResult(
            isSuspicious = isSus,
            riskScore = score,
            detectedTriggers = foundTriggers,
            advice = advice
        )
    }
}
