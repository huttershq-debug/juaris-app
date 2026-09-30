package com.juaris.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class JuarisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // 1. Sicheres Aufwecken des Notification-Listener-Services (Vom OS beim Booten erlaubt)
            val serviceIntent = Intent(context, JuarisNotificationListenerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // HINWEIS: Starte den VPN-Dienst nicht direkt hier via startForegroundService,
            // um die "ForegroundServiceStartNotAllowedException" ab Android 14 komplett zu umgehen.
            // Der JuarisVpnService wird am besten direkt aus dem onCreate/onStartCommand 
            // deines NotificationListenerServices aufgerufen, sobald dieser im System aktiv ist.
        }
    }
}

