package com.juaris.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class JuarisBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "JuarisBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        Log.d(TAG, "🔄 System-Event abgefangen: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // 1. Sicheres Aufwecken des Notification-Listener-Services (Vom OS beim Booten erlaubt)
            val serviceIntent = Intent(context, JuarisNotificationListenerService::class.java)
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "🛡️ Juaris-Hintergrundwächter erfolgreich beim Systemstart initiiert.")
            } catch (se: SecurityException) {
                // KORREKTUR FÜR ANDROID 14 & 15: Fängt herstellerspezifische oder OS-seitige 
                // Sicherheits- und Berechtigungsblockaden (z.B. Hintergrundstart-Einschränkungen) 
                // gezielt ab, um einen unschönen App-Absturz im System-Log komplett zu verhindern.
                Log.e(TAG, "❌ SecurityException beim Dienststart (OS-Restriktion): ${se.message}")
            } catch (e: Exception) {
                // Genereller Fallback für unerwartete Laufzeitfehler
                Log.e(TAG, "❌ Allgemeiner Fehler beim Starten des Juaris-Wächters: ${e.message}")
                e.printStackTrace()
            }

            // HINWEIS FÜR DEN PRODUKTIVBETRIEB: 
            // Den VPN-Dienst (JuarisVpnService) unter keinen Umständen direkt aus dieser 
            // onReceive-Methode via startForegroundService() aufrufen! Das würde unweigerlich zu einer 
            // "ForegroundServiceStartNotAllowedException" führen. 
            // Der JuarisVpnService wird am sichersten und regelkonform direkt aus dem onCreate() 
            // oder onStartCommand() deines JuarisNotificationListenerServices nachgezogen, 
            // sobald dieser vom OS seine aktiven Systemprivilegien erhalten hat.
        }
    }
}
