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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class CalendarScanWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val CHANNEL_ID = "juaris_life_companion_alerts"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val context = applicationContext
                val db = JuarisDatabase.getDatabase(context)

                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CALENDAR
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return@withContext Result.success()
                }

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
                        val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "" else ""
                        val description = if (descIdx >= 0) it.getString(descIdx) ?: "" else ""
                        val combined = "$title $description".lowercase()

                        // 🚫 SPAM- & WERBEFILTER (100% On-Device)
                        val spamKeywords = listOf(
                            "gewinn", "gutschein", "casino", "krypto", "bitcoin", "gratis",
                            "rabatt", "deal", "werbung", "newsletter", "angebot", "cashback",
                            "lotterie", "million", "gewonnen", "kostenlos", "sale", "voucher"
                        )
                        if (spamKeywords.any { spamWord -> combined.contains(spamWord) }) {
                            continue
                        }

                        val isAnniversary = combined.contains("hochzeitstag") || combined.contains("jahrestag") || combined.contains("jubiläum")
                        val isBirthday = combined.contains("geburtstag") || combined.contains("birthday")
                        val isMedical = combined.contains("arzt") || combined.contains("zahnarzt") || combined.contains("termin") || combined.contains("klinik") || combined.contains("therapie")
                        val isFinancial = combined.contains("rechnung") || combined.contains("mahnung") || combined.contains("inkasso") || combined.contains("frist") || combined.contains("steuer") || combined.contains("bescheid") || combined.contains("zahlung")
                        val isTravel = combined.contains("flug") || combined.contains("reise") || combined.contains("hotel") || combined.contains("zug") || combined.contains("ticket")
                        val isServiceOrHome = combined.contains("gas") || combined.contains("strom") || combined.contains("wasser") ||
                                              combined.contains("zähler") || combined.contains("ausbau") || combined.contains("ablesung") ||
                                              combined.contains("wartung") || combined.contains("handwerker") || combined.contains("installateur") ||
                                              combined.contains("service") || combined.contains("reparatur")

                        val isImportant = isAnniversary || isBirthday || isMedical || isFinancial || isTravel || isServiceOrHome

                        if (isImportant) {
                            // KORREKTUR 1: Lädt alle Titel und Zuweisungen dynamisch aus deinen 12 Sprachdateien (strings.xml)!
                            val (alertTitle, dbStatus) = when {
                                isFinancial -> Pair(context.getString(R.string.alert_financial_title), "WARNING")
                                isServiceOrHome -> Pair(context.getString(R.string.alert_service_title), "IMPORTANT")
                                isAnniversary -> Pair(context.getString(R.string.alert_anniversary_title), "IMPORTANT")
                                isBirthday -> Pair(context.getString(R.string.alert_birthday_title), "IMPORTANT")
                                isMedical -> Pair(context.getString(R.string.alert_medical_title), "IMPORTANT")
                                isTravel -> Pair(context.getString(R.string.alert_travel_title), "IMPORTANT")
                                else -> Pair(context.getString(R.string.alert_general_title), "INFO")
                            }

                            db.securityLogDao().insertLog(
                                SecurityLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    status = dbStatus,
                                    module = "Life-Companion-Wächter",
                                    description = alertTitle,
                                    details = "Ereignis: $title | Notiz: $description"
                                )
                            )

                            showLifePopup(context, alertTitle, title.ifEmpty { context.getString(R.string.logs_empty_message) })
                        }
                    }
                }

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
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
                description = "Unverzichtbare Alarme für Termine und Fristen"
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
            .setSmallIcon(R.drawable.app_icon) // KORREKTUR 2: Nutzt das transparente Vektor-Icon zur absoluten System-Stabilität!
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

