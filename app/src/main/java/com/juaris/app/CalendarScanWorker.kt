package com.juaris.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarScanWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "juaris_life_companion_alerts"
    }

    override fun doWork(): Result {
        return try {
            val context = applicationContext
            val db = JuarisDatabase.getDatabase(context)

            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_CALENDAR
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            // Zeitfenster für den heutigen Tag definieren (00:00 bis 23:59 Uhr)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startOfDay)
            ContentUris.appendId(builder, endOfDay)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.DESCRIPTION
            )

            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)

                while (it.moveToNext()) {
                    val title = if (titleIdx != -1) it.getString(titleIdx) ?: "" else ""
                    val description = if (descIdx != -1) it.getString(descIdx) ?: "" else ""
                    val combined = "$title $description".lowercase()

                    // 1. Meilensteine & Beziehungen (Hochzeitstag, Jahrestag, Geburtstag)
                    val isAnniversary = combined.contains("hochzeitstag") || combined.contains("jahrestag") || combined.contains("jubiläum")
                    val isBirthday = combined.contains("geburtstag") || combined.contains("birthday")
                    
                    // 2. Gesundheit & Termine
                    val isMedical = combined.contains("arzt") || combined.contains("zahnarzt") || combined.contains("termin") || combined.contains("klinik") || combined.contains("therapie")
                    
                    // 3. Finanzen, Rechnungen & Fristen
                    val isFinancial = combined.contains("rechnung") || combined.contains("mahnung") || combined.contains("inkasso") || combined.contains("frist") || combined.contains("steuer") || combined.contains("bescheid") || combined.contains("zahlung")

                    // 4. Reisen & Mobilität
                    val isTravel = combined.contains("flug") || combined.contains("reise") || combined.contains("hotel") || combined.contains("zug") || combined.contains("ticket")

                    val isImportant = isAnniversary || isBirthday || isMedical || isFinancial || isTravel

                    if (isImportant) {
                        val alertTitle = when {
                            isAnniversary -> "💍 Wichtiger Jahrestag / Hochzeitstag heute!"
                            isBirthday -> "🎂 Geburtstag heute!"
                            isMedical -> "🩺 Wichtiger Arzt- oder Gesundheitstermin"
                            isFinancial -> "🚨 Wichtige finanzielle Frist / Rechnung"
                            isTravel -> "✈️ Reise- oder Mobilitäts-Termin"
                            else -> "📅 Wichtiger Tages-Eintrag"
                        }

                        // In die lokale verschlüsselte Datenbank schreiben
                        CoroutineScope(Dispatchers.IO).launch {
                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    status = "IMPORTANT",
                                    module = "Life-Companion-Wächter",
                                    description = alertTitle,
                                    details = "Ereignis: $title | Notiz: $description"
                                )
                            )
                        }

                        // Sofortiges High-Priority Popup für den Nutzer
                        showLifePopup(context, alertTitle, title.ifEmpty { "Eintrag im Kalender gefunden" })
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun showLifePopup(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Juaris Life Companion & Fristen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unverzichtbare Alarme für Hochzeitstage, Geburtstage, Termine und Fristen"
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.hologram_avatar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}

