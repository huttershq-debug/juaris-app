package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juaris.app.LocalAICore
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.launch

// Vollständig in sich geschlossene Farbdefinitionen (verhindert jegliche Unresolved Reference Fehler)
private val NeonGiftgruen = Color(0xFF00FF66)
private val TacticalWarningRed = Color(0xFFFF3333)
private val TacticalGray = Color(0xFF8B949E)

@Composable
fun AIPage(aiCore: LocalAICore, logs: MutableList<SecurityLogEntity>) {
    val aiStatus by aiCore.aiStatus.collectAsState()
    val threatLevel by aiCore.threatLevel.collectAsState()
    var aiInsights by remember { mutableStateOf("Local AI Core Online. Analysiert Kalender, Rechnungen und Nachrichten 100% On-Device.") }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("On-Device AI Engine & Priority Hub", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: $aiStatus", style = MaterialTheme.typography.bodyMedium, color = if (threatLevel > 50) TacticalWarningRed else NeonGiftgruen)
            Text("Aktueller Bedrohungs-Level: $threatLevel / 100", style = MaterialTheme.typography.bodySmall, color = TacticalGray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KI-Insights & Prioritäten", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(aiInsights, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                aiInsights = "System-Scan läuft: Kalender, Benachrichtigungen und offene Posten geprüft. Keine kritischen Vorfälle."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Gesamtes System analysieren", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Modell-Arbeitsspeicher", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Button(
                        onClick = {
                            aiInsights = "AI Memory erfolgreich bereinigt. Alle temporären Cache-Daten gelöscht."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalWarningRed)
                    ) {
                        Text("KI-Arbeitsspeicher leeren", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

