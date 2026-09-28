package com.juaris.app.ui

import android.content.ContentUris
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juaris.app.JuarisDatabase
import com.juaris.app.LocalAICore
import com.juaris.app.SecurityLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private val TacticalWarningRed = Color(0xFFFF3333)
private val TacticalGray = Color(0xFF8B949E)

@Composable
fun AIPage(aiCore: LocalAICore, logs: MutableList<SecurityLogEntity>) {
    val context = LocalContext.current
    val aiStatus by aiCore.aiStatus.collectAsState()
    val threatLevel by aiCore.threatLevel.collectAsState()
    var aiInsights by remember { mutableStateOf("Local AI Core Online. Bereit für den vollständigen Gerätescan (Room-DB & Kalender).") }
    val coroutineScope = rememberCoroutineScope()
    val db = remember { JuarisDatabase.getDatabase(context) }

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
                                aiInsights = "⏳ Deep-Scan läuft: Durchsuche gesamte Room-Datenbank und den Gerätekalender..."

                                val resultReport = withContext(Dispatchers.IO) {
                                    val reportBuilder = StringBuilder()
                                    var totalFound = 0

                                    // 1. SCHRITT: Durchsuche alle Logs in der Room-Datenbank
                                    val allLogs = try {
                                        db.securityLogDao().getAllLogsSync()
                                    } catch (e: Exception) {
                                        emptyList()
                                    }

                                    val matchingLogs = allLogs.filter { log ->
                                        val content = "${log.module} ${log.description} ${log.details}".lowercase()
                                        content.contains("evn") || content.contains("gas") || content.contains("strom") ||
                                        content.contains("wasser") || content.contains("zähler") || content.contains("ausbau") ||
                                        content.contains("rechnung") || content.contains("mahnung") || content.contains("termin")
                                    }

                                    if (matchingLogs.isNotEmpty()) {
                                        reportBuilder.append("📁 **Room-DB Treffer (${matchingLogs.size}):**\n")
                                        matchingLogs.forEach { log ->
                                            reportBuilder.append("• [${log.status}] ${log.description}\n")
                                        }
                                        totalFound += matchingLogs.size
                                    }

                                    // 2. SCHRITT: Durchsuche den gesamten Gerätekalender (30 Tage zurück bis 90 Tage voraus)
                                    try {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.DAY_OF_YEAR, -30)
                                        val startTime = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_YEAR, 120) // total window 150 days
                                        val endTime = cal.timeInMillis

                                        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                                        ContentUris.appendId(builder, startTime)
                                        ContentUris.appendId(builder, endTime)

                                        val cursor = context.contentResolver.query(
                                            builder.build(),
                                            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.DESCRIPTION),
                                            null, null, "${CalendarContract.Instances.BEGIN} ASC"
                                        )

                                        val matchingEvents = mutableListOf<String>()
                                        cursor?.use {
                                            val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                                            val descIdx = it.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                                            while (it.moveToNext()) {
                                                val title = if (titleIdx != -1) it.getString(titleIdx) ?: "" else ""
                                                val desc = if (descIdx != -1) it.getString(descIdx) ?: "" else ""
                                                val combined = "$title $desc".lowercase()

                                                if (combined.contains("evn") || combined.contains("gas") || combined.contains("strom") ||
                                                    combined.contains("zähler") || combined.contains("ausbau") || combined.contains("ablesung") ||
                                                    combined.contains("termin") || combined.contains("rechnung") || combined.contains("mahnung")) {
                                                    matchingEvents.add(title.ifEmpty { "Unbenannter Termin" })
                                                }
                                            }
                                        }

                                        if (matchingEvents.isNotEmpty()) {
                                            reportBuilder.append("\n📅 **Kalender-Treffer (${matchingEvents.size}):**\n")
                                            matchingEvents.forEach { event ->
                                                reportBuilder.append("• $event\n")
                                            }
                                            totalFound += matchingEvents.size
                                        }
                                    } catch (e: Exception) {
                                        reportBuilder.append("\n⚠️ Kalender-Zugriff eingeschränkt oder nicht erlaubt.\n")
                                    }

                                    if (totalFound == 0) {
                                        "🔍 Deep-Scan abgeschlossen: Keine kritischen Einträge oder Versorger-Daten (EVN/Gas) auf dem Gerät gefunden."
                                    } else {
                                        "🚨 **Full-Device Scan erfolgreich!** $totalFound relevante Einträge im System verifiziert:\n\n$reportBuilder"
                                    }
                                }

                                aiInsights = resultReport
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
                            aiCore.clearMemory()
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

