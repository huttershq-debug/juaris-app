package com.juaris.security

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF8F9FA)
            ) {
                JuarisDashboard()
            }
        }
    }
}

@Composable
fun JuarisDashboard() {
    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("juaris_prefs", Context.MODE_PRIVATE)
    }

    // Zustände aus dem lokalen Speicher laden (Standard ist 'true')
    var smsFilterEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("sms_filter", true)) 
    }
    var callProtectionEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("call_protection", true)) 
    }
    var emailProtectionEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("email_protection", true)) 
    }

    // Berechnen, ob das System insgesamt aktiv ist
    val isSystemActive = smsFilterEnabled || callProtectionEnabled || emailProtectionEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Titel
        Text(
            text = "Juaris Security",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        // Status Karte
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemActive) Color(0xFFEFE7FC) else Color(0xFFFFEEEE)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isSystemActive) "Status: Aktiv & Geschützt" else "Status: Pausiert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isSystemActive) Color(0xFF4A148C) else Color(0xFFC62828)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSystemActive) "Lokale Schutz-Dienste laufen fehlerfrei" else "Alle Module sind aktuell deaktiviert",
                    fontSize = 13.sp,
                    color = Color(0xFF555555)
                )
            }
        }

        Text(
            text = "Schutzmodule steuern",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color(0xFF333333),
            modifier = Modifier.padding(top = 8.dp)
        )

        // Modul 1: SMS-Filter
        ModuleControlCard(
            title = "SMS-Filter",
            subtitle = "Priorität 999 (Eingehende Nachrichten)",
            checked = smsFilterEnabled,
            onCheckedChange = { newState ->
                smsFilterEnabled = newState
                sharedPrefs.edit().putBoolean("sms_filter", newState).apply()
            }
        )

        // Modul 2: Anruf-Schutz
        ModuleControlCard(
            title = "Anruf-Schutz",
            subtitle = "Erkennung von Scam & Spam-Anrufen",
            checked = callProtectionEnabled,
            onCheckedChange = { newState ->
                callProtectionEnabled = newState
                sharedPrefs.edit().putBoolean("call_protection", newState).apply()
            }
        )

        // Modul 3: E-Mail-Schutz
        ModuleControlCard(
            title = "E-Mail-Schutz",
            subtitle = "Phishing & Spam-Analyse im Hintergrund",
            checked = emailProtectionEnabled,
            onCheckedChange = { newState ->
                emailProtectionEnabled = newState
                sharedPrefs.edit().putBoolean("email_protection", newState).apply()
            }
        )
    }
}

@Composable
fun ModuleControlCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFE7FC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF6750A4)
                )
            )
        }
    }
}
