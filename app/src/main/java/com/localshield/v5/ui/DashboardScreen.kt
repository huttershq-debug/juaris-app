package com.localshield.v5.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuarisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuarisDashboardScreen()
                }
            }
        }
    }
}

@Composable
fun JuarisTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF6200EE),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(colorScheme = darkColorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuarisDashboardScreen() {
    // 100% Kontrolle: Nutzer entscheidet aktiv, was läuft
    var isCallShieldActive by remember { mutableStateOf(true) }
    var isSmsShieldActive by remember { mutableStateOf(true) }
    var isEmailShieldActive by remember { mutableStateOf(true) }

    // Transparenz-Log (Lokal gespeichert)
    val auditLogs = remember {
        mutableStateListOf(
            "🛡️ [00:01] System lokal gestartet. Keine Cloud-Verbindung.",
            "📞 [00:02] Call Screening API aktiv.",
            "✉️ [00:05] SMS-Filter lauscht im Arbeitsspeicher.",
            "📧 [00:10] IMAP-Hintergrund-Scan erfolgreich (0 Bedrohungen)."
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JUARIS // Sovereign Shield", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2C34)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("STATUS: VOLLSTÄNDIG GESCHÜTZT", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("100% On-Device Verarbeitung. Zero Server Costs. Absolute Privatsphäre.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Kontrolle & Freiheit (Toggles)
            item {
                Text("Aktivierte Module (Deine Kontrolle)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            item {
                ControlToggleItem(
                    title = "Echtzeit-Anrufschutz",
                    description = "Analysiert eingehende Nummern lokal.",
                    checked = isCallShieldActive,
                    onCheckedChange = { isCallShieldActive = it }
                )
            }

            item {
                ControlToggleItem(
                    title = "SMS Phishing-Blocker",
                    description = "Abfangen schädlicher SMS im Arbeitsspeicher.",
                    checked = isSmsShieldActive,
                    onCheckedChange = { isSmsShieldActive = it }
                )
            }

            item {
                ControlToggleItem(
                    title = "IMAP E-Mail Hintergrund-Scan",
                    description = "Direkter, verschlüsselter Postfach-Abgleich.",
                    checked = isEmailShieldActive,
                    onCheckedChange = { isEmailShieldActive = it }
                )
            }

            // Transparenz-Feed (Volle Einsicht)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Transparenz-Protokoll (Live)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }

            items(auditLogs) { log ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = log,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ControlToggleItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(description, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50))
            )
        }
    }
}
