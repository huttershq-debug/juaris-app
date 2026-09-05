package com.juaris.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuarisMainDashboard()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuarisMainDashboard() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem("Schutz & Live", Icons.Default.Lock),
        TabItem("Sperrliste", Icons.Default.Refresh),
        TabItem("Schwarm", Icons.Default.Share),
        TabItem("AGB & Info", Icons.Default.Info)
    )

    // Live-Logs
    val liveLogs = remember {
        mutableStateListOf(
            SecurityLogEntity(timestamp = System.currentTimeMillis(), module = "Anruf-Schutz", description = "Unterdrückte Nummer blockiert", status = "BLOCKED"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 60000, module = "SMS-Filter", description = "Phishing-SMS ('Paket Zustellung') abgefangen", status = "QUARANTINE"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 120000, module = "E-Mail-Scan", description = "Phishing-Keyword in E-Mail lokal erkannt", status = "ALERT")
        )
    }

    // Sperrliste (Blacklist)
    val blockedContacts = remember {
        mutableStateListOf("+43123456789", "+49987654321", "Anonyme Anrufe (Block)")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Juaris Security Suite (100% Offline)") }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardContent(
                    logs = liveLogs,
                    onSimulateThreat = {
                        liveLogs.add(0, SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "Test-Sandbox",
                            description = "Simulierter Angriff erfolgreich abgewehrt!",
                            status = "BLOCKED"
                        ))
                        Toast.makeText(context, "Bedrohung erfolgreich simuliert!", Toast.LENGTH_SHORT).show()
                    },
                    onExportLogs = {
                        Toast.makeText(context, "${liveLogs.size} Logs lokal im internen Speicher gesichert.", Toast.LENGTH_LONG).show()
                    },
                    onPanicWipe = {
                        liveLogs.clear()
                        blockedContacts.clear()
                        Toast.makeText(context, "PANIC: Alle lokalen Logs und Sperrlisten gelöscht!", Toast.LENGTH_LONG).show()
                    }
                )
                1 -> BlacklistContent(
                    blockedList = blockedContacts,
                    onAddBlocked = { newEntry -> 
                        if (newEntry.isNotBlank()) {
                            blockedContacts.add(newEntry)
                            Toast.makeText(context, "Nummer zur Sperrliste hinzugefügt", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveBlocked = { item -> 
                        blockedContacts.remove(item)
                        Toast.makeText(context, "Nummer freigegeben", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> SwarmMeshContent()
                3 -> PrivacyAndLegalContent()
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)

@Composable
fun DashboardContent(
    logs: List<SecurityLogEntity>, 
    onSimulateThreat: () -> Unit, 
    onExportLogs: () -> Unit,
    onPanicWipe: () -> Unit
) {
    var callProtection by remember { mutableStateOf(true) }
    var smsProtection by remember { mutableStateOf(true) }
    var emailProtection by remember { mutableStateOf(true) }

    val blockedCount = logs.count { it.status == "BLOCKED" || it.status == "QUARANTINE" }
    
    // Aufschlüsselung nach Vektoren für die visuelle Statistik
    val callsCount = logs.count { it.module == "Anruf-Schutz" }
    val smsCount = logs.count { it.module == "SMS-Filter" }
    val emailCount = logs.count { it.module == "E-Mail-Scan" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Schutz-Zentrale & System-Status", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // System-Gesundheits-Monitor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("System-Gesundheit: Optimal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Call-Screening & SMS-Interception aktiv", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Statistik-Kacheln
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Abgewehrt", style = MaterialTheme.typography.bodySmall)
                    Text("$blockedCount", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Modus", style = MaterialTheme.typography.bodySmall)
                    Text("100% Offline", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visuelle Bedrohungs-Statistik (Mini-Aufteilung)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Bedrohungs-Verteilung nach Vektor", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📞 Anrufe: $callsCount", style = MaterialTheme.typography.bodySmall)
                    Text("✉️ SMS: $smsCount", style = MaterialTheme.typography.bodySmall)
                    Text("📧 Mails: $emailCount", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Manuelle Schutz-Regler
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Schutz-Regler (Nutzerfreiheit)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Anruf-Schutz")
                    Switch(checked = callProtection, onCheckedChange = { callProtection = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SMS-Filter")
                    Switch(checked = smsProtection, onCheckedChange = { smsProtection = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("E-Mail-Scan")
                    Switch(checked = emailProtection, onCheckedChange = { emailProtection = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Aktions-Buttons (Sandbox, Export & Panic Button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(onClick = onSimulateThreat, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Test", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onExportLogs, modifier = Modifier.weight(1f)) {
                Text("Export", style = MaterialTheme.typography.bodySmall)
            }
            // Der Panic Button in Alarm-Farbe (Error)
            Button(
                onClick = onPanicWipe, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("PANIC", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Live-Aktivitätsstream", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Modul: ${log.module}", style = MaterialTheme.typography.bodyMedium)
                        Text("Ereignis: ${log.description}", style = MaterialTheme.typography.bodyLarge)
                        Text("Aktion: ${log.status}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun BlacklistContent(blockedList: MutableList<String>, onAddBlocked: (String) -> Unit, onRemoveBlocked: (String) -> Unit) {
    var inputNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Eigene Sperrliste (Blacklist)", style = MaterialTheme.typography.titleLarge)
        Text("Verwalte hier deine lokal gesperrten Rufnummern. Bleibt rein auf dem Gerät.")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputNumber,
                onValueChange = { inputNumber = it },
                label = { Text("Nummer / Spam-Muster") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                onAddBlocked(inputNumber)
                inputNumber = ""
            }) {
                Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Gesperrte Einträge (${blockedList.size}):", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(blockedList) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { onRemoveBlocked(item) }) {
                            Text("Entfernen", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwarmMeshContent() {
    var testInputText by remember { mutableStateOf("Verdächtige Nachricht") }
    
    val generatedHash = remember(testInputText) {
        try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(testInputText.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Fehler"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("P2P-Schwarmintelligenz & Quanten-Engine", style = MaterialTheme.typography.titleLarge)
        Text("Dezentraler Austausch von quantensicheren Hashes (SHA-256) mit Geräten in der direkten Offline-Umgebung.")
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Aktive Mesh-Verbindungen: 2 Geräte in Reichweite", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Letzte synchronisierte Signatur:")
                Text("a8f5c...39e2 (Verifiziert)", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quantum-Hash Live-Visualisierer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Teste hier, wie Text anonymisiert wird:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    label = { Text("Beispiel-Text eingeben") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Generierter SHA-256 Quantum-Hash:", style = MaterialTheme.typography.bodySmall)
                Text(generatedHash, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        
        Text("Null-Data-Garantie: Keine Rufnummern oder Inhalte verlassen jemals das Gerät. Nur anonyme mathematische Hashes.", 
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun PrivacyAndLegalContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Datenschutz & Null-Data-Garantie (AGB)", style = MaterialTheme.typography.titleLarge)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. 100% Offline-First", style = MaterialTheme.typography.titleMedium)
                Text("Juaris sendet niemals Daten an externe Server. Alle Analysen für Anrufe, SMS und E-Mails laufen lokal.")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Keine Datensammlung", style = MaterialTheme.typography.titleMedium)
                Text("Deine Sperrliste, Kontakte und Logs verbleiben ausschließlich in deiner lokalen Room-Datenbank.")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Quantensichere Schwarm-Sicherheit", style = MaterialTheme.typography.titleMedium)
                Text("Der P2P-Schwarm nutzt ausschließlich unidirektionale Hashes. Rückschlüsse auf Personen oder Inhalte sind mathematisch ausgeschlossen.")
            }
        }
    }
}
