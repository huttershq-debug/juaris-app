package com.juaris.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object JuarisNotificationDispatcher {

    // KORREKTUR 1: Strikt getrennte Kanäle für Infos und Alarme.
    // Verhindert, dass das Android-System die Wichtigkeitsstufen dauerhaft falsch einfriert!
    private const val CHANNEL_ID_CRITICAL = "juaris_critical_alerts_channel"
    private const val CHANNEL_ID_DEFAULT = "juaris_info_alerts_channel"

    fun sendPriorityAlert(context: Context, title: String, message: String, isCritical: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // KORREKTUR 2: Laufzeit-Berechtigungsprüfung für Android 13+ statt Unterdrückung via SuppressLint
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return // Keine Rechte vorhanden, Abbruch um Systemkonflikte zu vermeiden
            }
        }

        // Ziel-Kanal und Priorität anhand der Dringlichkeit bestimmen
        val activeChannelId = if (isCritical) CHANNEL_ID_CRITICAL else CHANNEL_ID_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isCritical) {
                // High-Importance Kanal für lautstarke Heads-Up Popups
                if (notificationManager.getNotificationChannel(CHANNEL_ID_CRITICAL) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID_CRITICAL, 
                        "Juaris Kritische Alarme", 
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Echtzeit-Popups bei akuten Sicherheitsbedrohungen"
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            } else {
                // Default-Importance Kanal für lautlose oder dezente Info-Meldungen
                if (notificationManager.getNotificationChannel(CHANNEL_ID_DEFAULT) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID_DEFAULT, 
                        "Juaris Sicherheits-Protokolle", 
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Hintergrund-Informationen und Status-Updates"
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Dynamischer Request-Code basierend auf dem Titel-Hash, um Intent-Kollisionen zu vermeiden
        val uniqueRequestCode = title.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            context, uniqueRequestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, activeChannelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isCritical) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        // KORREKTUR 3: Dynamische ID-Generierung mittels Hash-Wert der Nachricht.
        // Garantiert, dass mehrere Bedrohungen gleichzeitig als separate Kacheln sichtbar bleiben!
        val dynamicNotificationId = (title + message).hashCode()

        try {
            NotificationManagerCompat.from(context).notify(dynamicNotificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
