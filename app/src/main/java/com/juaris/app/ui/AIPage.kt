package com.juaris.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.SecurityLogEntity
import com.juaris.app.LocalAICore

@Composable
fun AIPage(aiCore: LocalAICore, logs: List<SecurityLogEntity>) {
    val context = LocalContext.current
    val status by aiCore.aiStatus.collectAsState()
    val threatLevel by aiCore.threatLevel.collectAsState()
    val insights by aiCore.aiInsights.collectAsState()

    val threatColor = if (threatLevel == "SICHER") Color(0xFF00FF66) else Color(0xFFFF3333)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("On-Device KI-Kern", style = MaterialTheme.typography.titleLarge)
            Text("Dezentrale, lokale Intelligenz ohne Cloud-Anbindung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sicherheits-Status", style = MaterialTheme.typography.titleMedium)
                        Text(text = threatLevel.orEmpty(), color = threatColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Divider(color = MaterialTheme.colorScheme.surface)
                    Text("System-Zustand: $status", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Echtzeit-Analyse (Lokaler Kontext)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = insights.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        aiCore.analyzeLocalEnvironment(logs, activeMeshNodes = 3, clipboardActive = true)
                        Toast.makeText(context, "Lokaler KI-Scan abgeschlossen", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("On-Device KI-Scan jetzt ausführen")
                }
                OutlinedButton(
                    onClick = {
                        aiCore.wipeAiMemory()
                        Toast.makeText(context, "KI-Kontext komplett gelöscht", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3333))
                ) {
                    Text("KI-Gedächtnis löschen (Privacy Wipe)")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter IT-Solutions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
