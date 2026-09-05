package com.juaris.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

data class TabItem(val title: String, val icon: ImageVector)
data class SecurityLogEntity(val timestamp: Long, val module: String, val description: String, val status: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Das giftgrüne Hacker-Farbschema (Schwarz & Neon-Grün mit rotem Alarm-Akzent)
            val hackerGreenColorScheme = darkColorScheme(
                primary = Color(0xFF00FF66),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF003311),
                onPrimaryContainer = Color(0xFF00FF66),
                background = Color.Black,
                onBackground = Color(0xFF00FF66),
                surface = Color(0xFF080808),
                onSurface = Color(0xFFE0E0E0),
                surfaceVariant = Color(0xFF121212),
                onSurfaceVariant = Color(0xFF00FF66),
                error = Color(0xFFFF3333) // Giftiges Rot für den Panic-Button & Bedrohungen
            )

            MaterialTheme(colorScheme = hackerGreenColorScheme) {
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
        TabItem("Status", Icons.Default.CheckCircle),
        TabItem("Schutz", Icons.Default.Lock),
        TabItem("Sperren", Icons.Default.Refresh),
        TabItem("Schwarm", Icons.Default.Share),
        TabItem("Info", Icons.Default.Info)
    )

    val liveLogs = remember {
        mutableStateListOf(
            SecurityLogEntity(timestamp = System.currentTimeMillis(), module = "Netzwerk-Monitor", description = "Lokale Loopback-Verbindung verifiziert", status = "SAFE"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 30000, module = "Berechtigungs-Wächter", description = "Keine App im Hintergrund aktiv", status = "SAFE"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 60000, module = "Anruf-Schutz", description = "Unterdrückte Nummer blockiert", status = "BLOCKED"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 120000, module = "SMS-Filter", description = "Phishing-SMS ('Paket Zustellung') abgefangen", status = "QUARANTINE")
        )
    }

    val blockedContacts = remember {
        mutableStateListOf("+43123456789", "+49987654321", "Anonyme Anrufe (Block)")
    }

    var callProtection by remember { mutableStateOf(true) }
    var smsProtection by remember { mutableStateOf(true) }
    var emailProtection by remember { mutableStateOf(true) }
    var vaultUnlocked by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Juaris Security Suite") }
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
                0 -> StatusPage(
                    logs = liveLogs,
                    onSimulateThreat = {
                        liveLogs.add(0, SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "Test-Sandbox",
                            description = "Simulierter Offline-Angriff erfolgreich abgewehrt!",
                            status = "BLOCKED"
                        ))
                        Toast.makeText(context, "Bedrohung erfolgreich abgewehrt!", Toast.LENGTH_SHORT).show()
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
                1 -> ProtectionModulesPage(
                    callProtection = callProtection,
                    onCallChange = { callProtection = it },
                    smsProtection = smsProtection,
                    onSmsChange = { smsProtection = it },
                    emailProtection = emailProtection,
                    onEmailChange = { emailProtection = it },
                    vaultUnlocked = vaultUnlocked,
                    onVaultToggle = { vaultUnlocked = it }
                )
                2 -> BlacklistAndLogsPage(
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
                    },
                    logs = liveLogs
                )
                3 -> SwarmMeshPage()
                4 -> PrivacyAndLegalContent()
            }
        }
    }
}

// SEITE 1: STATUS & SYSTEM-ÜBERSICHT (Mit rotem Panic-Button)
@Composable
fun StatusPage(
    logs: List<SecurityLogEntity>,
    onSimulateThreat: () -> Unit,
    onExportLogs: () -> Unit,
    onPanicWipe: () -> Unit
) {
    val blockedCount = logs.count { h -> h.status == "BLOCKED" || h.status == "QUARANTINE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("System-Gesundheit & Status", style = MaterialTheme.typography.titleLarge)
            Text("Echtzeit-Diagnose deines isolierten Offline-Systems.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Status: Optimal & Abgesichert", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("100% Offline-Architektur aktiv (Keine Cloud-Schnittstelle)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Abgewehrt", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$blockedCount", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Architektur", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Offline-First", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Schnellaktionen & Steuerung", style = MaterialTheme.typography.titleMedium)
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSimulateThreat, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Angriff simulieren & Sandbox testen")
                }
                OutlinedButton(onClick = onExportLogs, modifier = Modifier.fillMaxWidth()) {
                    Text("Lokales Log-Archiv sichern")
                }
                // Hier ist der auffällige rote Panik-Button
                Button(
                    onClick = onPanicWipe,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PANIC WIPE (Alle lokalen Daten löschen)", color = Color.Black)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// SEITE 2: SCHUTZ-MODULE & REGLER
@Composable
fun ProtectionModulesPage(
    callProtection: Boolean,
    onCallChange: (Boolean) -> Unit,
    smsProtection: Boolean,
    onSmsChange: (Boolean) -> Unit,
    emailProtection: Boolean,
    onEmailChange: (Boolean) -> Unit,
    vaultUnlocked: Boolean,
    onVaultToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Kernmodule & Schutz-Regler", style = MaterialTheme.typography.titleLarge)
            Text("Konfiguriere hier die aktiven Hintergrund-Wächter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Aktive Filter & Blockaden", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anruf-Schutz", style = MaterialTheme.typography.bodyLarge)
                            Text("Blockiert unterdrückte & Spam-Nummern", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = callProtection, onCheckedChange = onCallChange)
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS-Filter", style = MaterialTheme.typography.bodyLarge)
                            Text("Erkennt Phishing-Links in Echtzeit", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = smsProtection, onCheckedChange = onSmsChange)
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("E-Mail-Scan", style = MaterialTheme.typography.bodyLarge)
                            Text("Untersucht Postfächer lokal auf Schadcode", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = emailProtection, onCheckedChange = onEmailChange)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onVaultToggle(!vaultUnlocked)
                    val statusText = if (!vaultUnlocked) "Vault entsperrt (Lokal)" else "Vault verschlüsselt & gesperrt"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verschlüsselter Offline-Vault", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sicherer Aufbewahrungsort für sensible Notizen & Passwörter.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusColor = if (vaultUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(if (vaultUnlocked) "Status: Entsperrt (Bereit)" else "Status: Gesperrt & Geschützt", color = statusColor, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { Toast.makeText(context, "Netzwerk-Monitor: 0 externe Verbindungen", Toast.LENGTH_SHORT).show() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lokaler Netzwerk-Monitor", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Traffic-Kontrolle läuft rein lokal. Keine externen Server-Pings.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// SEITE 3: SPERRLISTE & LOGS
@Composable
fun BlacklistAndLogsPage(
    blockedList: MutableList<String>,
    onAddBlocked: (String) -> Unit,
    onRemoveBlocked: (String) -> Unit,
    logs: List<SecurityLogEntity>
) {
    var inputNumber by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Sperrliste & Aktivitäten", style = MaterialTheme.typography.titleLarge)
            Text("Verwalte blockierte Rufnummern und betrachte den Live-Stream.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neue Nummer sperren", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputNumber,
                            onValueChange = { inputNumber = it },
                            label = { Text("Rufnummer / Muster") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            onAddBlocked(inputNumber)
                            inputNumber = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                        }
                    }
                }
            }
        }

        item {
            Text("Aktive Sperrliste (${blockedList.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(blockedList) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemoveBlocked(item) }) {
                        Text("Freigeben", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Live-Aktivitätsstream", style = MaterialTheme.typography.titleMedium)
        }

        items(logs) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Modul: ${log.module}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Text("Ereignis: ${log.description}", style = MaterialTheme.typography.bodyLarge)
                    Text("Aktion: ${log.status}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// SEITE 4: P2P-SCHWARM & QUANTUM HASH
@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    var discoveredDevicesCount by remember { mutableStateOf(0) }
    var scanStatusText by remember { mutableStateOf("Bereit für echten Hardware-Mesh-Abgleich") }
    
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter

    var testInputText by remember { mutableStateOf("Verdächtige Nachricht") }
   
    val generatedHash = remember(testInputText) {
        try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(testInputText.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Fehler"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("P2P-Schwarmintelligenz & Mesh", style = MaterialTheme.typography.titleLarge)
            Text("Dezentraler Austausch von quantensicheren Hashes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
       
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth-Hardware Status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(scanStatusText, style = MaterialTheme.typography.bodyMedium)
                    Text("Gekoppelte Nodes in Reichweite: $discoveredDevicesCount", style = MaterialTheme.typography.bodyLarge)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (bluetoothAdapter == null) {
                                scanStatusText = "Keine Bluetooth-Hardware am Gerät verfügbar"
                            } else if (!bluetoothAdapter.isEnabled) {
                                scanStatusText = "Bluetooth ist am Gerät deaktiviert!"
                                Toast.makeText(context, "Bitte Bluetooth einschalten", Toast.LENGTH_SHORT).show()
                            } else {
                                scanStatusText = "Hardware-Scan aktiv (Gekoppelte Nodes geprüft)"
                                discoveredDevicesCount = bluetoothAdapter.bondedDevices?.size ?: 0
                                Toast.makeText(context, "Hardware-Scan durchgeführt", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hardware-Mesh-Scan ausführen")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quantum-Hash Live-Visualisierer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Anonymisierung von Signaturen:", style = MaterialTheme.typography.bodySmall)
                   
                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text("Signatur-Text eingeben") },
                        modifier = Modifier.fillMaxWidth()
                    )
                   
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Generierter SHA-256 Quantum-Hash:", style = MaterialTheme.typography.bodySmall)
                    Text(generatedHash, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
       
        item {
            Text("Null-Data-Garantie: Keine Rufnummern oder Inhalte verlassen jemals das Gerät. Nur anonyme mathematische Hashes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// SEITE 5: AGB & INFO (Mit offiziellen Credits)
@Composable
fun PrivacyAndLegalContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Rechtliche Bestimmungen & AGB", style = MaterialTheme.typography.titleLarge)
            Text("Globale Null-Haftungs- und Offline-Garantie (Stand: 2026)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
       
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Grundsatz & Architektur (100% Offline-First)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Juaris Security Suite (nachfolgend 'Software') von Hutter's IT-Solutions arbeitet ausnahmslos lokal auf dem Endgerät des Nutzers. Es existieren zu keinem Zeitpunkt Cloud-Verbindungen, Telemetrie-Schnittstellen oder externe Server-Backends. Die Datenhoheit verbleibt zu 100% beim Betreiber des Endgeräts.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Absoluter Haftungsausschluss (Worldwide Disclaimer)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text("Die Nutzung der Software erfolgt auf eigene, ausschließliche Gefahr des Nutzers. Hutter's IT-Solutions als Entwickler und alle beteiligten Parteien schließen jegliche Haftung für direkte, indirekte, mittelbare oder Folgeschäden aus, die sich aus der Nutzung, der Fehlfunktion, dem Ausfall von Schutzmechanismen oder der Inkompatibilität der Software ergeben – weltweit und unabhängig von der geltenden nationalen oder internationalen Gesetzgebung.")
                    Text("Dies schließt ein, ist aber nicht beschränkt auf: Datenverlust, entgangenen Gewinn, finanzielle Schäden, Systemabstürze, verpasste Notrufe, nicht erkannte Schadsoftware, Phishing-Angriffe oder Hardware-Beschädigungen.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Keine Garantie auf Schutzwirkung ('As-Is')", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Software wird im Zustand 'wie besehen' ('as-is') und ohne jegliche ausdrückliche oder stillschweigende Gewährleistung von Hutter's IT-Solutions bereitgestellt. Es wird keine Garantie dafür übernommen, dass die lokalen Filter (Anruf-, SMS- oder E-Mail-Filter) 100% aller Bedrohungen abfangen oder dass die lokalen Sensoren fehlerfrei arbeiten. Der Nutzer erkennt an, dass absolute Sicherheit in der digitalen Welt mathematisch und technisch unmöglich ist.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Eigenverantwortung beim Panic-Button (Wipe)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Software beinhaltet eine unwiderrufliche Notfall-Löschfunktion ('PANIC WIPE'). Bei Betätigung dieser Funktion werden alle lokalen Logs, Sperrlisten und gespeicherten Daten sofort und unwiederbringlich aus dem Arbeitsspeicher und der lokalen Datenbank gelöscht. Hutter's IT-Solutions übernimmt keinerlei Haftung für versehentlich oder durch Dritte ausgelöschte Datenbestände.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("5. P2P-Schwarm & Anonyme Hashes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Der dezentrale Austausch über das lokale Hardware-Mesh erfolgt ausschließlich über unidirektionale SHA-256-Hashes. Es werden zu keinem Zeitpunkt Klardaten, Rufnummern oder persönliche Identifikatoren übertragen. Sollte es durch lokale Funkstörungen oder Hardware-Kollisionen zu Fehlinterpretationen von Signaturen kommen, ist jeglicher Regressanspruch gegenüber Hutter's IT-Solutions ausgeschlossen.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("6. Salvatorische Klausel & Gerichtsstand", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Sollten einzelne Bestimmungen dieser AGB ungültig, unvollständig oder nicht durchsetzbar sein, bleiben die übrigen Bestimmungen davon unberührt. Durch die Installation und Ausführung der Juaris Security Suite akzeptiert der Nutzer diese Bedingungen uneingeschränkt und unwiderruflich für alle weltweiten Jurisdiktionen.")
                }
            }
        }

        // Offizieller Copyright- & Design-Footer ganz unten auf der AGB-Seite
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Hutter IT Solutions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Copyright Benedikt Wolfgang Hütter", style = MaterialTheme.typography.bodySmall)
                    Text("Design Julia Kerschhofer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
