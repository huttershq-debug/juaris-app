package com.juaris.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class JuarisAppWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_TRIGGER_EMERGENCY = "com.juaris.app.ACTION_TRIGGER_EMERGENCY"

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.juaris_widget_layout)

            // 1. Klick auf das Widget öffnet die Haupt-App (Absolut store-konform)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
           
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // CRITICAL FIX: Umgehen der Android 12+ Hintergrund-Start-Restriktionen (Trampoline Restrictions).
            // Wir senden einen Broadcast an uns selbst, statt die Activity direkt aufzurufen!
            val broadcastIntent = Intent(context, JuarisAppWidgetProvider::class.java).apply {
                action = ACTION_TRIGGER_EMERGENCY
            }
           
            val emergencyPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                broadcastIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_emergency_btn, emergencyPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // Fängt den Broadcast des Widgets ab und startet die Activity sicher aus dem Kontext des Receivers
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TRIGGER_EMERGENCY) {
            // Wir nutzen die offizielle statische Methode der EmergencyActivity,
            // da diese bereits mit NotificationCompat.Builder und setFullScreenIntent
            // perfekt dafür ausgelegt ist, die Systembarrieren im Notfall zu durchbrechen!
            EmergencyActivity.triggerEmergencyAlarm(context, "Widget Notfall-Auslöser")
        }
    }
}
