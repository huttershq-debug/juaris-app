package com.juaris.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.IBinder
import androidx.activity.ComponentActivity

class SmsDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}

class WapPushDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {}
}

class SmsRespondService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

class SmsComposeActivity : ComponentActivity()
