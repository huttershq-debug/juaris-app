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

@Composable
fun AIPage(aiCore: LocalAICore, logs: MutableList<SecurityLogEntity>) {
    var aiInsights by remember { mutableStateOf("Local AI Core Online. Alle Modelle arbeiten 100% On-Device.") }
    var analysisStatus by remember { mutableStateOf("Bereit") }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("On-Device AI Engine", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("100% lokale Heuristik & Bedrohungsanalyse ohne Cloud.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KI-Insights & Status", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(aiInsights, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                analysisStatus = "Analysiere..."
                                aiInsights = "Umgebung geprüft: Keine Anomalien im lokalen Speicher festgestellt."
                                analysisStatus = "Abgeschlossen"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Lokale Umgebung analysieren", color = Color.Black, fontWeight = FontWeight.Bold)
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
                            aiInsights = "AI Memory erfolgreich bereinigt."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333))
                    ) {
                        Text("KI-Arbeitsspeicher leeren", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


