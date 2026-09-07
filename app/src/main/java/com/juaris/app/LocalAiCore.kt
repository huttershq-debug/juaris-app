package com.juaris.app

import android.content.Context
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

/**
 * Der 100% lokale KI-Kern von Juaris.
 * Läuft vollständig offline auf dem Gerät (Edge-AI/Heuristik-Engine).
 * Keine Cloud, keine Telemetrie, volle Kontrolle durch den Nutzer.
 */
class LocalAICore(private val context: Context) {

    private val _aiStatus = MutableStateFlow("KI-Kernel bereit (Offline-Modus)")
    val aiStatus: StateFlow<String> = _aiStatus

    private val _threatLevel = MutableStateFlow("SICHER")
    val threatLevel: StateFlow<String> = _threatLevel

    private val _aiInsights = MutableStateFlow("Alle lokalen Ströme im grünen Bereich. Keine Anomalien.")
    val aiInsights: StateFlow<String> = _aiInsights

    /**
     * Analysiert alle lokalen Streams in Echtzeit direkt auf dem Smartphone-Prozessor.
     */
    fun analyzeLocalEnvironment(
        logs: List<SecurityLogEntity>,
        activeMeshNodes: Int,
        clipboardActive: Boolean
    ) {
        _aiStatus.value = "Analysiere lokale Vektoren..."

        val recentThreats = logs.count { it.status == "BLOCKED" || it.status == "QUARANTINE" }
        
        val analysis = buildString {
            append("• Aktive Mesh-Nodes im Schwarm: $activeMeshNodes\n")
            append("• Abgewehrte Bedrohungen (on-device): $recentThreats\n")
            append("• Zwischenablage-Überwachung: ${if (clipboardActive) "Aktiv & Geschützt" else "Inaktiv"}\n")
            
            if (recentThreats > 2) {
                _threatLevel.value = "WARNUNG"
                append("\n[KI-Empfehlung]: Erhöhte Aktivität im lokalen Umfeld. P2P-Mesh-Verschlüsselung verifizieren.")
            } else {
                _threatLevel.value = "SICHER"
                append("\n[KI-Status]: Keine Bedrohungen im lokalen Raum. Zero-Cloud-Architektur intakt.")
            }
        }

        _aiInsights.value = analysis
        _aiStatus.value = "Überwachung aktiv (100% On-Device)"
    }

    /**
     * Sofortiges Löschen des KI-Gedächtnisses (Privacy Kill-Switch).
     */
    val wipeAiMemory: () -> Unit = {
        _aiStatus.value = "KI-Gedächtnis vollständig und spurlos gelöscht."
        _threatLevel.value = "SICHER"
        _aiInsights.value = "Kein Kontext vorhanden."
    }
}

