package com.juaris.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class EmergencyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sperrbildschirm umgehen & App in den Vordergrund zwingen (Vollbild-Alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // KORREKTUR 1: Standard-Grund dynamisch aus deinen 12 Weltsprachen laden!
        val defaultReason = getString(R.string.emergency_reason_default)
        val reason = intent.getStringExtra("reason") ?: defaultReason

        setContent {
            EmergencyAlarmScreen(
                reason = reason,
                onCancel = {
                    finish()
                },
                onTriggerCall = {
                    executeEmergencyCall()
                }
            )
        }
    }

    private fun executeEmergencyCall() {
        try {
            // SICHERER NOTRUF: Öffnet den Wählhebel mit der Notrufnummer 112
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(dialIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * Zwingt die EmergencyActivity sofort aus dem Hintergrund auf den Bildschirm –
         * absolut regelkonform und sicher für den Google Play Store Review.
         */
        fun triggerEmergencyAlarm(context: Context, reason: String) {
            val intent = Intent(context, EmergencyActivity::class.java).apply {
                putExtra("reason", reason)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 999, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val channelId = "juaris_emergency_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(R.string.emergency_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // KORREKTUR 2: System-Benachrichtigungen an deine strings.xml gekoppelt!
            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.emergency_notification_title))
                .setContentText(context.getString(R.string.emergency_notification_text))
                .setSmallIcon(R.drawable.app_icon)
                .setFullScreenIntent(pendingIntent, true) // ERZWINGT DAS ÖFFNEN IM VOLLBILD
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(999, notification)
        }
    }
}

@Composable
fun EmergencyAlarmScreen(
    reason: String,
    onCancel: () -> Unit,
    onTriggerCall: () -> Unit
) {
    var countdown by remember { mutableStateOf(6) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        onTriggerCall()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCC0000)), // Aggressives Notfall-Rot
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // KORREKTUR 3: Alle UI-Texte sprachenunabhängig für den Weltmarkt deklariert!
            Text(
                text = stringResource(R.string.emergency_screen_heading),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = reason,
                color = Color.Yellow,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "$countdown",
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.emergency_screen_countdown_desc),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onTriggerCall,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(text = stringResource(R.string.btn_emergency_call_now), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(text = stringResource(R.string.btn_emergency_cancel), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

