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
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Hier können globale, flüchtige System-Broadcasts verarbeitet werden.
        // Der Schutz läuft ressourcenschonend in den Kern-Services.
        Log.v(TAG, "System-Signal empfangen: ${intent.action}")
    }
}
