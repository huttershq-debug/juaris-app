package com.juaris.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

class LocalAICore(private val context: Context) {

    companion object {
        private const val TAG = "JuarisAIEngine"
    }

    private val _aiStatus = MutableStateFlow("On-Device Intent Engine: Global Multi-Language Active")
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
            PHISHING_THREAT,    // Rot: Betrug, Malware, Angriffe
            INVOICE_FINANCIAL,  // Orange: Rechnungen, Mahnungen, Inkasso
            SERVICE_UTILITY,    // Gold: Strom, Gas, Wasser, Zähler, Wartung
            HEALTH_APPOINTMENT, // Gold: Arzt, Klinik, Medizinisch
            PERSONAL_REMINDER,  // Gold: Geburtstag, Jahrestag
            DELIVERY_PARCEL,    // Info: Logistik, Post
            GENERAL             // Standard
        }
    }

    fun evaluateContentSafety(text: String, sender: String?): Boolean {
        val result = analyzeAndCategorize(text, sender)
        return result.isSafe
    }

    // Universelle, sprachübergreifende Analyse für den Weltmarkt (12+ Sprachräume)
    fun analyzeAndCategorize(text: String, sender: String?): UnifiedAnalysisResult {
        val normalized = normalizeAndClean(text)
        val stripped = normalized.replace(" ", "")

        // 1. Phishing & Dringlichkeit (Multilingual: EN, DE, ES, FR, PT, RU, AR, ZH, HI, BN, UR, ID)
        val urgencyKeywords = listOf(
            "sofort", "urgent", "urgente", "immediato", "срочно", "عاجл", "紧急", "तुरंत", "জরুরি", "فوری", "segera",
            "frist", "deadline", "vencimiento", "échéance", "срок", "مهلة", "期限", "समय सीमा", "সময়সীমা", "مقررہ تاریخ", "tenggat",
            "sperrung", "suspension", "bloqué", "blokiert", "блокировка", "حظر", "冻结", "ब्लॉक", "ব্লক", "بند", "blokir"
        )

        val authorityKeywords = listOf(
            "police", "polizei", "zoll", "court", "gericht", "tax", "finanzamt", "hacienda", "полиция", "налог", 
            "شرطة", "ضريبة", "警察", "税务", "पुलिस", "পুলিশ", "پولیس", "polisi",
            "bank", "post", "dhl", "netflix", "microsoft", "apple", "google"
        )

        // 2. Rechnungen & Finanzen (Multilingual)
        val invoiceKeywords = listOf(
            "rechnung", "invoice", "factura", "facture", "fatura", "счет", "فاتورة", "发票", "चालан", "বিল", "انوائس", "faktur",
            "bill", "amount", "betrag", "montant", "valor", "сумма", "مبلغ", "金额", "राशि", "পরিমাণ", "رقم", "jumlah",
            "fällig", "due", "vencimiento", "échéance", "vencimento", "оплата", "استحقاق", "到期", "देय", "বকেয়া", "واجب الادا", "jatuh tempo",
            "mahnung", "reminder", "overdue", "inkasso", "задолженность", "تذكير", "催款", "स्मारक", "স্মারক", "یاد دہانی", "pengingat"
        )

        // 3. Versorger, Energie, Wasser & Zähler
        val utilityKeywords = listOf(
            "utility", "utilities", "energy", "power", "electricity", "electricidad", "électricité", "energia", "энергия", "طاقة", "能源", "ऊर्जा", "শক্তি", "توانائی", "energi",
            "gas", "gás", "газ", "غاز", "燃气", "गैस", "গ্যাস", "গিসের",
            "water", "agua", "eau", "água", "вода", "ماء", "水", "पानी", "পানি", "پانی", "air",
            "meter", "zähler", "contador", "compteur", "medidor", "счетчик", "عداد", "电表", "मीटर", "মিটার", "میٹر", "meteran",
            "reading", "ablesung", "relevé", "leitura", "показания", "قراءة", "抄表", "रीडिंग", "পঠন", "ریڈنگ", "pembacaan",
            "outage", "grid", "netz", "network", "red", "réseau", "сеть", "شبكة", "电网", "नेटवर्क", "নেটওয়ার্ক", "جال", "jaringan",
            "maintenance", "wartung", "mantenimiento", "manutenção", "ремонт", "صيانة", "维护", "रخرखाव", "রক্ষণাবেক্ষণ", "تھام بھال", "pemeliharaan"
        )

        // 4. Gesundheit & Termine
        val healthKeywords = listOf(
            "doctor", "arzt", "médecin", "médico", "врач", "طبيب", "医生", "डॉक्टर", "ডাক্তার", "ڈاکٹر", "dokter",
            "hospital", "clinic", "klinik", "clinique", "больница", "клиника", "مستشفى", "医院", "अस्पताल", "হাসপাতাল", "اسپتال", "rumah sakit",
            "appointment", "termin", "cita", "rendez-vous", "consulta", "запись", "موعد", "预约", "अपॉइंटमेंट", "앱পয়েন্টমেন্ট", "ملاقات", "janji temu"
        )

        var threatScore = 0
        var priorityScore = 3

        // 1. Dringlichkeit & Phishing-Muster prüfen
        if (urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }) threatScore += 35
        if (authorityKeywords.any { normalized.contains(it) || stripped.contains(it) }) threatScore += 25
       
        // Roh-Text ohne Leerzeichen für exakte Krypto- und IBAN-Muster
        val strippedRaw = text.replace(" ", "").lowercase()
        val hasIbanPattern = Regex("[a-z]{2}\\d{2}[a-z0-9]{11,30}").containsMatchIn(strippedRaw)
        val hasCryptoPattern = Regex("(bc1|[13])[a-km-zA-HJ-NP-Z1-9]{25,39}").containsMatchIn(strippedRaw)
       
        if (hasIbanPattern || hasCryptoPattern) {
            threatScore += 45
        }

        // UPGRADE FÜR ABSOLUTE WELTSPITZE: Mathematische Verhaltens-Heuristik (Pattern-Matching)
        // Erkennt verdächtige Kombinationen, die auf Social-Engineering hindeuten (z.B. Link + Zeitdruck)
        val hasSuspiciousUrl = text.contains("http://") || text.contains("https://")
        if (hasSuspiciousUrl && urgencyKeywords.any { normalized.contains(it) }) {
            threatScore += 30 // Kombination aus URL + Panikmache erhöht das Risiko drastisch!
        }

        threatScore = threatScore.coerceAtMost(100)
        _threatLevel.value = threatScore

        val category: UnifiedAnalysisResult.Category
        val title: String

        // 2. Kategorisierung und Prioritäts-Vergabe
        val containsInvoice = invoiceKeywords.any { normalized.contains(it) }
        val containsUrgency = urgencyKeywords.any { normalized.contains(it) || stripped.contains(it) }

        when {
            threatScore >= 60 -> {
                category = UnifiedAnalysisResult.Category.PHISHING_THREAT
                priorityScore = 10
                title = "🚨 Security Threat / Phishing Detected!"
            }
            containsInvoice && containsUrgency -> {
                category = UnifiedAnalysisResult.Category.INVOICE_FINANCIAL
                priorityScore = 9
                title = "⚠️ Urgent Overdue / Payment Notice"
            }
            containsInvoice -> {
                category = UnifiedAnalysisResult.Category.INVOICE_FINANCIAL
                priorityScore = 7
                title = "📄 Invoice / Financial Statement"
            }
            utilityKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.SERVICE_UTILITY
                priorityScore = 8
                title = "🔧 Utility & Service Notice (Power/Gas/Water)"
            }
            healthKeywords.any { normalized.contains(it) } -> {
                category = UnifiedAnalysisResult.Category.HEALTH_APPOINTMENT
                priorityScore = 8
                title = "🩺 Medical Appointment / Health"
            }
            else -> {
                category = UnifiedAnalysisResult.Category.GENERAL
                priorityScore = 3
                title = "Notification from ${sender ?: "System"}"
            }
        }

        // ARCHITEKTUR-TIPP FÜR DIE INTEGRATION DES ECHTEN LLM KERNELS (ONNX/TFLite):
        // Wenn das Heuristik-Ergebnis unschlüssig ist (z.B. Score zwischen 30 und 59),
        // weisen wir die On-Device-NPU an, die semantische Absicht tiefergehend zu prüfen:
        // if (threatScore in 30..59) { executeLocalTensorModelCheck(text) }

        val isSafe = threatScore < 60
        _aiStatus.value = if (isSafe) "System Secure (Priority: $priorityScore)" else "Threat Blocked (Score: $threatScore)"

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
        _aiStatus.value = "On-Device Intent Engine: Memory Cleared"
    }

    private fun normalizeAndClean(input: String): String {
        val cleanLeetspeak = input.lowercase()
            .replace("0", "o")
            .replace("4", "a")
            .replace("1", "i")
            .replace("3", "e")
            .replace("@", "a")
            .replace("$", "s")

        // KORREKTUR: Den unvollständigen und fehlerhaften Regex sauber repariert!
        // Bereinigt Satzzeichen, bleibt aber vollkommen sicher für Unicode-Zeichensätze 
        // (Kyrillisch, Arabisch, Chinesisch, Hindi etc.), um globale Kompatibilität zu garantieren.
        return try {
            val unicodeRegex = Regex("[^\\p{L}\\p{Nd}\\s]")
            cleanLeetspeak.replace(unicodeRegex, " ")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der Unicode-Normalisierung, weiche auf Basis-String aus.")
            cleanLeetspeak
        }
    }
}
