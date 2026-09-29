package com.juaris.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object JuarisNotificationDispatcher {

    private const val CHANNEL_ID = "juaris_priority_channel"
    private const val NOTIFICATION_ID_HIGH = 9999

    fun sendPriorityAlert(context: Context, title: String, message: String, isCritical: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Erstelle den High-Importance Kanal für echte Pop-ups (Heads-Up)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (isCritical) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, "Juaris Priority Alerts", importance).apply {
                description = "Wichtige Sicherheits- und Versorger-Warnungen"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Konstruiere das Pop-up mit maximaler Priorität
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isCritical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isCritical) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        with(NotificationManagerCompat.from(context)) {
            // Löst das Pop-up sofort auf dem Gerät aus
            notify(NOTIFICATION_ID_HIGH, builder.build())
        }
    }
}

