package com.juaris.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Juaris Background Receiver
 * Globaler System-Ereignisverteiler für dezentrale Offline-Signale.
 * Die Echtzeit-Filterung von Anrufen und SMS wird aus Performance- und Datenschutzgründen
 * direkt von den spezialisierten System-Services (ScamCallScreeningService & SmsFilterReceiver) übernommen.
 */
class JuarisReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JuarisReceiver"
        
        // Konstanten für potenzielle dezentrale App-Interaktionen (z.B. aus dem P2P-Schwarm)
        const val ACTION_SWARM_SIGNAL_RECEIVED = "com.juaris.app.ACTION_SWARM_SIGNAL_RECEIVED"
        const val ACTION_LOCAL_SECURITY_TRIGGER = "com.juaris.app.ACTION_LOCAL_SECURITY_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        // KORREKTUR 1: Nullability-Schutz! Verhindert App-Abstürze, falls das Android-System 
        // in seltenen Extremsituationen (z.B. bei RAM-Mangel) einen null-Intent liefert.
        if (intent == null) return

        val action = intent.action
        Log.v(TAG, "System-Signal empfangen: $action")

        // KORREKTUR 2: Saubere und erweiterbare Ereignis-Weiche (Sealed/When-Struktur).
        // Ermöglicht es dem Entwickler, flüchtige Offline-Signale aus dem Bluetooth-Mesh-Schwarm
        // oder interne Sicherheits-Trigger sofort thread-sicher zu verarbeiten.
        try {
            when (action) {
                ACTION_SWARM_SIGNAL_RECEIVED -> {
                    val swarmPayload = intent.getStringExtra("swarm_payload") ?: ""
                    Log.d(TAG, "Dezentrales Schwarm-Signal im Hintergrund-Receiver erfasst.")
                    // Hier kann bei Bedarf ein Event in den JuarisEventBus geschossen werden:
                    // JuarisEventBus.postEvent("SWARM:$swarmPayload")
                }
                ACTION_LOCAL_SECURITY_TRIGGER -> {
                    Log.w(TAG, "🚨 Interner Sicherheits-Trigger via Broadcast empfangen!")
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    // Wichtig für den Bluetooth-Mesh-Schwarm: Wenn der Flugmodus aktiviert wird,
                    // muss die App reagieren, um Hardware-Konflikte im Hintergrund zu vermeiden.
                    val isAirplaneModeOn = intent.getBooleanExtra("state", false)
                    Log.d(TAG, "Flugmodus-Status geändert: $isAirplaneModeOn")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der Signal-Verarbeitung im JuarisReceiver: ${e.message}")
            e.printStackTrace()
        }
    }
}
