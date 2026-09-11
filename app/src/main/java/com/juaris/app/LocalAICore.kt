package com.juaris.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.LocalAICore
import com.juaris.app.SecurityLogEntity

@Composable
fun AIPage(aiCore: LocalAICore, logs: List<SecurityLogEntity>) {
    val aiStatus by aiCore.aiStatus.collectAsState()
    val threatLevel by aiCore.threatLevel.collectAsState()
    val aiInsights by aiCore.aiInsights.collectAsState()
    val decisions by aiCore.autonomousDecisions.collectAsState()

    // AGI führt bei jedem Öffnen der Seite einen Deep Scan durch
    LaunchedEffect(Unit) {
        aiCore.executeDeepCognitiveScan(logs, activeMeshNodes = 4, clipboardActive = true, permissionsAudited = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Juaris One-Device AGI", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Autonome On-Device Intelligenz & Heuristik-Kern.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AGI System-Status", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(aiStatus, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    val threatColor = if (threatLevel == "SICHER") NeonGiftgruen else Color(0xFFFF3333)
                    Text("Bedrohungs-Level: $threatLevel", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = threatColor)
                }
            }
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kognitive Einblicke", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(aiInsights.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB0BEC5))
                }
            }
        }
        if (decisions.isNotEmpty()) {
            item {
                Text("Autonome AGI-Entscheidungen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
            }
            items(decisions) { decision ->
                TacticalPulseCard {
                    Text("• $decision", color = Color.White, fontSize = 13.sp)
                }
            }
        }
        item {
            Button(
                onClick = { aiCore.triggerAutonomousCountermeasure() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
            ) {
                Text("Autonomen AGI-Scan erzwingen", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        item {
            OutlinedButton(
                onClick = { aiCore.wipeAiMemory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("AGI-Gedächtnis vernichten (Privacy Wipe)", color = Color(0xFFFF3333))
            }
        }
    }
}

