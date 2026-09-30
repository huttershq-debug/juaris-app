package com.juaris.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class JuarisAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
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

            // 2. Direkt-Klick auf den Notfall-Button im Widget öffnet den Notruf-Screen
            val emergencyIntent = Intent(context, EmergencyActivity::class.java).apply {
                putExtra("reason", "Widget Notfall-Auslöser")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
           
            // KORREKTUR: Zwingend erforderlich ab Android 12+ (API 31)!
            // Nutze FLAG_IMMUTABLE, um die harte Sicherheitsprüfung von Google fehlerfrei zu passieren.
            val emergencyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val emergencyPendingIntent = PendingIntent.getActivity(
                context,
                1,
                emergencyIntent,
                emergencyFlags
            )
            views.setOnClickPendingIntent(R.id.widget_emergency_btn, emergencyPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

