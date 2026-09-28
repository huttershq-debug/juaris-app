package com.juaris.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocalAICore(private val context: Context) {

    private val _aiStatus = MutableStateFlow("On-Device Intent Engine: Aktiv (Vollschutz)")
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
            PHISHING_THREAT, // Rot: Betrug, Phishing, Angriffe
            INVOICE_FINANCIAL, // Rot/Gelb: Rechnungen, Mahnungen, Inkasso
            SERVICE_UTILITY, // Gold: EVN, Gas, Strom, Wasser, Zähler, Handwerker
            HEALTH_APPOINTMENT, // Gold: Arzt, Klinik, Therapie
            PERSONAL_REMINDER, // Gold: Geburtstag, Hochzeitstag, Jahrestag
            DELIVERY_PARCEL, // Info: DHL, Post, Paketdienste
            GENERAL // Standard
        }
    }

    fun evaluateContentSafety(text: String, sender: String?): Boolean {
        val result = analyzeAndCategorize(text, sender)
        return result.isSafe
    }

    // Universal-Analyse für das gesamte Handy (Benachrichtigungen, E-Mails, Kalender, SMS)
    fun analyzeAndCategorize(text: String, sender: String?): UnifiedAnalysisResult {
        val normalized = normalizeAndClean(text)
        val stripped = normalized.replace(" ", "")

        // 1. Bedrohungs- & Phishing-Keywords (Rot)
        val urgencyKeywords = listOf("sofort", "heute noch", "frist", "drohung", "sperrung", "letzte warnung", "sofortiges handeln", "kontosperrung")
        val authorityKeywords = listOf("zoll", "polizei", "gericht", "finanzamt", "bank", "post", "dhl", "netflix", "microsoft")
        val actionKeywords = listOf("http://", "https://", "bit.ly", "tinyurl", "login", "bestaetigen", "verifizieren", "daten eingeben", "passwort")

        // 2. Finanz- & Rechnungs-Keywords (Rot/Orange)
        val invoiceKeywords = listOf("rechnung", "betrag", "fällig", "zahlungsziel", "überweisung", "mahnung", "inkasso", "lastschrift", "bescheid")

        // 3. Versorger-, Energie- & Handwerker-Keywords (Gold - inkl. EVN & Gas!)
        val utilityKeywords = listOf("evn", "gas", "strom", "wasser", "zähler", "ausbau", "ablesung", "wartung", "handwerker", "installateur", "service", "energie", "netz")

        // 4. Gesundheits- & Termin-Keywords (Gold)
        val healthKeywords = listOf("arzt", "zahnarzt", "termin", "klinik", "therapie", "krankenhaus", "befund", "praxis")

        // 5. Persönliche Ereignisse (Gold)
        val personalKeywords = listOf("geburtstag", "hochzeitstag", "jahrestag", "jubiläum")

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
                category = UnifiedAnalysisResult.Category.INVOICE_FINANCIAL
                priorityScore = 9
                title = "⚠️ Dringende Mahnung / Frist"
            }
            invoiceKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.INVOICE_FINANCIAL
                priorityScore = 7
                title = "📄 Neue Rechnung eingetroffen"
            }
            utilityKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.SERVICE_UTILITY
                priorityScore = 8
                title = "🔧 Versorger- & Zähler-Termin (EVN/Gas)"
            }
            healthKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.HEALTH_APPOINTMENT
                priorityScore = 8
                title = "🩺 Medizinischer Termin"
            }
            personalKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.PERSONAL_REMINDER
                priorityScore = 8
                title = "🎉 Wichtiger Jahrestag / Geburtstag"
            }
            else -> {
                category = UnifiedAnalysisResult.Category.GENERAL
                priorityScore = 3
                title = "Information von ${sender ?: "System"}"
            }
        }

        val isSafe = threatScore < 60
        _aiStatus.value = if (isSafe) "System sicher (Prio: $priorityScore)" else "Bedrohung geblockt (Score: $threatScore)"

        return UnifiedAnalysisResult(
            isSafe = isSafe,
            threatScore = threatScore,
            priorityScore = priorityScore,
            category = category,
            title = title,
            summary = text.take(120) + if (text.length > 120) "..." else ""
        )
    }

    fun clearMemory() {
        _threatLevel.value = 0
        _aiStatus.value = "On-Device Intent Engine: Arbeitsspeicher bereinigt"
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


