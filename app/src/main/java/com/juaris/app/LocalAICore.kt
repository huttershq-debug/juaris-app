package com.juaris.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalAICore(private val context: Context) {

    private val _aiStatus = MutableStateFlow("On-Device Intent Engine: Aktiv")
    val aiStatus: StateFlow<String> = _aiStatus

    private val _threatLevel = MutableStateFlow(0)
    val threatLevel: StateFlow<Int> = _threatLevel

    data class UnifiedAnalysisResult(
        val isSafe: Boolean,
        val threatScore: Int,
        val priorityScore: Int,
        val category: Category,
        val title: String,
        val summary: String
    ) {
        enum class Category {
            PHISHING_THREAT,
            INVOICE,
            REMINDER,
            PAYMENT,
            CALENDAR,
            GENERAL
        }
    }

    fun analyzeAndCategorize(text: String, sender: String?): UnifiedAnalysisResult {
        val normalized = normalizeAndClean(text)
        val stripped = normalized.replace(" ", "")

        val urgencyKeywords = listOf("sofort", "heute noch", "frist", "drohung", "sperrung", "letzte warnung", "sofortiges handeln")
        val authorityKeywords = listOf("zoll", "polizei", "gericht", "finanzamt", "bank", "post", "dhl", "netflix", "microsoft")
        val actionKeywords = listOf("http://", "https://", "bit.ly", "tinyurl", "login", "bestaetigen", "verifizieren", "daten eingeben", "passwort")
        val invoiceKeywords = listOf("rechnung", "betrag", "fällig", "zahlungsziel", "überweisung")

        var threatScore = 0
        var priorityScore = 3

        if (urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }) threatScore += 35
        if (authorityKeywords.any { normalized.contains(it) || stripped.contains(it) }) threatScore += 25
        if (actionKeywords.any { normalized.contains(it) || stripped.contains(it) }) threatScore += 40

        val hasIbanPattern = Regex("[a-z]{2}\\d{2}[a-z0-9]{11,30}").containsMatchIn(stripped)
        val hasCryptoPattern = Regex("(bc1|[13])[a-km-zA-HJ-NP-Z1-9]{25,39}").containsMatchIn(stripped)
        
        if (hasIbanPattern || hasCryptoPattern) {
            threatScore += 45
        }

        val hasUrgency = urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }
        val hasAuthority = authorityKeywords.any { normalized.contains(it) || stripped.contains(it) }
        if (hasUrgency && hasAuthority) {
            threatScore += 25
        }

        threatScore = threatScore.coerceAtMost(100)
        _threatLevel.value = threatScore

        val category: UnifiedAnalysisResult.Category
        val title: String

        when {
            threatScore >= 60 -> {
                category = UnifiedAnalysisResult.Category.PHISHING_THREAT
                priorityScore = 10
                title = "🚨 Phishing / Betrug erkannt!"
            }
            normalized.contains("mahnung") || normalized.contains("inkasso") -> {
                category = UnifiedAnalysisResult.Category.REMINDER
                priorityScore = 9
                title = "⚠️ Dringende Mahnung / Frist"
            }
            invoiceKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.INVOICE
                priorityScore = 7
                title = "📄 Neue Rechnung eingetroffen"
            }
            else -> {
                category = UnifiedAnalysisResult.Category.GENERAL
                priorityScore = 3
                title = "Information von ${sender ?: "Unbekannt"}"
            }
        }

        val isSafe = threatScore < 60
        _aiStatus.value = if (isSafe) "System sicher (Prio-Score: $priorityScore)" else "Bedrohung geblockt (Score: $threatScore)"

        return UnifiedAnalysisResult(
            isSafe = isSafe,
            threatScore = threatScore,
            priorityScore = priorityScore,
            category = category,
            title = title,
            summary = text.take(120) + if (text.length > 120) "..." else ""
        )
    }

    private fun normalizeAndClean(input: String): String {
        return input.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("1", "i")
            .replace("3", "e")
            .replace("@", "a")
            .replace("$", "s")
            .replace(Regex("[^a-zäöüß0-9\\s]"), " ")
    }
}

