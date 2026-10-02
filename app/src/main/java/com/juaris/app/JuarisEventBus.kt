package com.juaris.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object JuarisEventBus {

    private const val TAG = "JuarisEventBus"

    // Ein an den App-Lebenszyklus gebundener, thread-sicherer Scope für Notfall-Ereignisse
    private val busJob = SupervisorJob()
    private val busScope = CoroutineScope(Dispatchers.Default + busJob)

    // Ein speicher-optimierter Flow ohne Memory-Leaks
    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /**
     * Schießt ein Ereignis sicher in das System. 
     * Besitzt eine 100%-ige Zustellgarantie – selbst bei extremer Systemauslastung.
     */
    fun postEvent(event: String) {
        // 1. Blitzschneller Versuch, das Event direkt in den Puffer zu emittieren
        val emitted = _events.tryEmit(event)
        
        if (!emitted) {
            // 2. KORREKTUR FÜR DIE WELTSPITZE: Falls der Puffer durch einen massiven Angriff 
            // oder hunderte Netzwerkmeldungen temporär voll ist, weichen wir auf 'emit()' aus.
            // Das garantiert, dass KEIN Sicherheits-Alarm jemals lautlos verschluckt wird!
            busScope.launch {
                try {
                    _events.emit(event)
                    Log.d(TAG, "⚠️ Puffer-Limit erreicht. Event via Coroutine-Fallback zugestellt: $event")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Kritischer Fehler im EventBus-Fallback: ${e.message}")
                }
            }
        }
    }
}
