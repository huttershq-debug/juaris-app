package com.juaris.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class JuarisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val serviceIntent = Intent(context, JuarisNotificationListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            val vpnIntent = Intent(context, JuarisVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(vpnIntent)
            } else {
                context.startService(vpnIntent)
            }
        }
    }
}
