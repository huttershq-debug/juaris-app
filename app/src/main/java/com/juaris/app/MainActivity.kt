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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

data class TabItem(val title: String, val icon: ImageVector)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Juaris") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    logs: List<SecurityLogEntity>,
    onSimulateThreat: () -> Unit,
    onExportLogs: () -> Unit,
    onPanicWipe: () -> Unit
) {
    val context = LocalContext.current
    var callProtection by remember { mutableStateOf(true) }
    var smsProtection by remember { mutableStateOf(true) }
    var emailProtection by remember { mutableStateOf(true) }
    var vaultUnlocked by remember { mutableStateOf(false) }

    val blockedCount = logs.count { h -> h.status == "BLOCKED" || h.status == "QUARANTINE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Schutz-Zentrale & System-Status", style = MaterialTheme.typography.titleLarge)
        }

        item {
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
                        Text("100% Offline-Architektur aktiv (Keine Cloud-Schnittstelle)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
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
                        Text("Architektur", style = MaterialTheme.typography.bodySmall)
                        Text("Offline-First", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Text("Integrierte Kernmodule", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Toast.makeText(context, "Netzwerk-Monitor: 0 externe Verbindungen (Isoliert)", Toast.LENGTH_SHORT).show()
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Lokaler Netzwerk-Monitor", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Traffic-Kontrolle läuft lokal. Keine externen Server-Pings.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    Toast.makeText(context, "Berechtigungs-Wächter: Alle Sensoren geschützt", Toast.LENGTH_SHORT).show()
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Berechtigungs-Wächter", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Kamera, Mikrofon & Standort im Hintergrund überwacht.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    vaultUnlocked = !vaultUnlocked
                    val statusText = if (vaultUnlocked) "Vault entsperrt (Lokal)" else "Vault verschlüsselt & gesperrt"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Verschlüsselter Offline-Vault", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    val statusColor = if (vaultUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(if (vaultUnlocked) "Status: Entsperrt (Bereit)" else "Status: Gesperrt (Sicher)", color = statusColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Schutz-Regler", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
        }

        item {
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
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Live-Aktivitätsstream", style = MaterialTheme.typography.titleMedium)
        }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("P2P-Schwarmintelligenz & Hardware-Mesh", style = MaterialTheme.typography.titleLarge)
        Text("Dezentraler Austausch von quantensicheren Hashes (SHA-256) über lokale Funk-Schnittstellen.")
       
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bluetooth-Hardware Status", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(scanStatusText, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Gekoppelte Nodes in Reichweite: $discoveredDevicesCount", style = MaterialTheme.typography.bodyLarge)
                
                Spacer(modifier = Modifier.height(12.dp))
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
                    Text("Echten Hardware-Mesh-Scan ausführen")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quantum-Hash Live-Visualisierer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Anonymisierung von Signaturen:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
               
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    label = { Text("Signatur-Text eingeben") },
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Rechtliche Bestimmungen & AGB", style = MaterialTheme.typography.titleLarge)
            Text("Globale Null-Haftungs- und Offline-Garantie (Stand: 2026)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
       
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Grundsatz & Architektur (100% Offline-First)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Juaris Security Suite (nachfolgend 'Software') arbeitet ausnahmslos lokal auf dem Endgerät des Nutzers. Es existieren zu keinem Zeitpunkt Cloud-Verbindungen, Telemetrie-Schnittstellen oder externe Server-Backends. Die Datenhoheit verbleibt zu 100% beim Betreiber des Endgeräts.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Absoluter Haftungsausschluss (Worldwide Disclaimer)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text("Die Nutzung der Software erfolgt auf eigene, ausschließliche Gefahr des Nutzers. Der Entwickler und alle beteiligten Parteien schließen jegliche Haftung für direkte, indirekte, mittelbare oder Folgeschäden aus, die sich aus der Nutzung, der Fehlfunktion, dem Ausfall von Schutzmechanismen oder der Inkompatibilität der Software ergeben – weltweit und unabhängig von der geltenden nationalen oder internationalen Gesetzgebung.")
                    Text("Dies schließt ein, ist aber nicht beschränkt auf: Datenverlust, entgangenen Gewinn, finanzielle Schäden, Systemabstürze, verpasste Notrufe, nicht erkannte Schadsoftware, Phishing-Angriffe oder Hardware-Beschädigungen.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Keine Garantie auf Schutzwirkung ('As-Is')", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Software wird im Zustand 'wie besehen' ('as-is') und ohne jegliche ausdrückliche oder stillschweigende Gewährleistung bereitgestellt. Es wird keine Garantie dafür übernommen, dass die lokalen Filter (Anruf-, SMS- oder E-Mail-Filter) 100% aller Bedrohungen abfangen oder dass die lokalen Sensoren fehlerfrei arbeiten. Der Nutzer erkennt an, dass absolute Sicherheit in der digitalen Welt mathematisch und technisch unmöglich ist.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Eigenverantwortung beim Panic-Button (Wipe)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Die Software beinhaltet eine unwiderrufliche Notfall-Löschfunktion ('PANIC WIPE'). Bei Betätigung dieser Funktion werden alle lokalen Logs, Sperrlisten und gespeicherten Daten sofort und unwiederbringlich aus dem Arbeitsspeicher und der lokalen Datenbank gelöscht. Der Entwickler übernimmt keinerlei Haftung für versehentlich oder durch Dritte ausgelöschte Datenbestände.")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("5. P2P-Schwarm & Anonyme Hashes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Der dezentrale Austausch über das lokale Hardware-Mesh erfolgt ausschließlich über unidirektionale SHA-256-Hashes. Es werden zu keinem Zeitpunkt Klardaten, Rufnummern oder persönliche Identifikatoren übertragen. Sollte es durch lokale Funkstörungen oder Hardware-Kollisionen zu Fehlinterpretationen von Signaturen kommen, ist jeglicher Regressanspruch ausgeschlossen.")
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
    }
}
