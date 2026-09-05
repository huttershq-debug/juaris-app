package com.juaris.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuarisDashboard()
                }
            }
        }
    }
}

@Composable
fun JuarisDashboard() {
    var isSmsActive by remember { mutableStateOf(true) }
    var isCallActive by remember { mutableStateOf(true) }
    var isEmailActive by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Juaris Security",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSmsActive || isCallActive || isEmailActive) 
                    Color(0xFFE8DEF8) else Color(0xFFE0E0E0)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Status: " + if (isSmsActive || isCallActive || isEmailActive) "Aktiv & Geschützt" else "Pausiert",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hintergrund-Dienste laufen lokal",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Schutzmodule steuern",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        ControlCard(
            title = "SMS-Filter",
            subtitle = "Priorität 999 (Eingehende Nachrichten)",
            isActive = isSmsActive,
            onToggle = { isSmsActive = it }
        )

        ControlCard(
            title = "Anruf-Schutz",
            subtitle = "Erkennung von Scam & Spam-Anrufen",
            isActive = isCallActive,
            onToggle = { isCallActive = it }
        )

        ControlCard(
            title = "E-Mail-Schutz",
            subtitle = "Phishing & Spam-Analyse im Hintergrund",
            isActive = isEmailActive,
            onToggle = { isEmailActive = it }
        )
    }
}

@Composable
fun ControlCard(title: String, subtitle: String, isActive: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = isActive,
                onCheckedChange = onToggle
            )
        }
    }
}

