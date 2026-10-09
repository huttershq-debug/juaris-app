Package com.juaris.app.ui

import android.content.ContentUris
import android.net.Uri
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
private val NeonGiftgruen = Color(0xFF00E676)

@Composable
fun AIPage(aiCore: LocalAICore, logs: MutableList<SecurityLogEntity>) {
    val context = LocalContext.current
    val aiStatus by aiCore.aiStatus.collectAsState()
    val threatLevel by aiCore.threatLevel.collectAsState()
    var aiInsights by remember { mutableStateOf("Local AI Core Online. Bereit für den vollständigen Gerätescan (Room-DB, Kalender & SMS).") }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("KI-Insights & Prioritäten", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(aiInsights, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                aiInsights = "⏳ Deep-Scan läuft: Durchsuche Room-DB, Kalender und SMS-Posteingang (weltweit multilingual)..."

                                val resultReport = withContext(Dispatchers.IO) {
                                    val reportBuilder = StringBuilder()
                                    var totalFound = 0

                                    val globalUtilityKeywords = listOf(
                                        "بجلی", "گیس", "پانی", "بل", "انوائس", "میٹر", "ریڈنگ", "بقایا", "مقررہ تاریخ", "توانائی",
                                        "बिजली", "गैस", "पानी", "बिल", "चालान", "मीटर", "रीडिंग", "देय", "ऊर्जा", "खपत",
                                        "listrik", "energi", "daya", "gas", "air", "meteran", "pembacaan", "tagihan", "faktur", "jatuh tempo", "pemeliharaan",
                                        "বিদ্যুৎ", "গ্যাস", "পানি", "জল", "বিল", "মিটার", "পঠন", "বকেয়া", "শক্তি", "রক্ষণাবেক্ষণ",
                                        "strom", "energie", "gas", "wasser", "zähler", "ablesung", "rechnung", "mahnung", "fällig", "netz", "wartung",
                                        "energia", "luz", "agua", "gas", "medidor", "factura", "cuenta", "vencimiento", "mantenimiento",
                                        "électricité", "gaz", "eau", "compteur", "relevé", "facture", "échéance", "entretien",
                                        "энергия", "электричество", "газ", "вода", "счетчик", "счет", "квитанция", "оплата",
                                        "كهرباء", "طاقة", "غاز", "ماء", "عداد", "فاتورة", "استحقاق",
                                        "utility", "utilities", "energy", "power", "electricity", "water", "meter", "reading", "bill", "invoice", "due", "overdue", "outage"
                                    )

                                    val allLogs = try {
                                        db.securityLogDao().getAllLogsSync()
                                    } catch (e: Exception) {
                                        emptyList()
                                    }

                                    val matchingLogs = allLogs.filter { log ->
                                        val content = "${log.module} ${log.description} ${log.details}".lowercase()
                                        globalUtilityKeywords.any { keyword -> content.contains(keyword) }
                                    }

                                    if (matchingLogs.isNotEmpty()) {
                                        reportBuilder.append("📁 **Room-DB Treffer (${matchingLogs.size}):**\n")
                                        matchingLogs.forEach { log ->
                                            reportBuilder.append("• [${log.status}] ${log.description}\n")
                                        }
                                        totalFound += matchingLogs.size
                                    }

                                    try {
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.DAY_OF_YEAR, -30)
                                        val startTime = cal.timeInMillis
                                        cal.add(Calendar.DAY_OF_YEAR, 150)
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

                                                if (globalUtilityKeywords.any { keyword -> combined.contains(keyword) }) {
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

                                    try {
                                        val smsUri = Uri.parse("content://sms/inbox")
                                        val cursor = context.contentResolver.query(
                                            smsUri,
                                            arrayOf("address", "body", "date"),
                                            null, null, "date DESC"
                                        )

                                        cursor?.use {
                                            val addressIdx = it.getColumnIndex("address")
                                            val bodyIdx = it.getColumnIndex("body")
                                            val matchingSms = mutableListOf<String>()

                                            while (it.moveToNext()) {
                                                val sender = if (addressIdx != -1) it.getString(addressIdx) ?: "Unbekannt" else "Unbekannt"
                                                val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                                                val combined = "$sender $body".lowercase()

                                                if (globalUtilityKeywords.any { keyword -> combined.contains(keyword) }) {
                                                    matchingSms.add("Absender: $sender | Text: ${body.take(60)}...")
                                                }
                                            }

                                            if (matchingSms.isNotEmpty()) {
                                                reportBuilder.append("\n💬 **SMS-Posteingang Treffer (${matchingSms.size}):**\n")
                                                matchingSms.forEach { sms ->
                                                    reportBuilder.append("• $sms\n")
                                                }
                                                totalFound += matchingSms.size
                                            }
                                        }
                                    } catch (e: Exception) {
                                        reportBuilder.append("\n⚠️ SMS-Zugriff nicht erlaubt oder Berechtigung fehlt.\n")
                                    }

                                    if (totalFound == 0) {
                                        "🔍 Deep-Scan abgeschlossen: Keine kritischen Versorger- oder Energiemuster (global multilingual) im System gefunden."
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
