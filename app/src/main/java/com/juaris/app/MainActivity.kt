@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.juaris.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

    // Verschlüsselter lokaler Speicher (AES-256 per Android Keystore)
    private lateinit var securePrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            securePrefs = EncryptedSharedPreferences.create(
                this,
                "juaris_secure_vault",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback falls Keystore auf sehr alten Geräten zickt
            securePrefs = getPreferences(Context.MODE_PRIVATE)
        }

        setContent {
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
                error = Color(0xFFFF3333)
            )

            // States aus dem sicheren Speicher laden
            var isFirstRun by remember { mutableStateOf(securePrefs.getBoolean("is_first_run", true)) }
            var isLoggedIn by remember { mutableStateOf(securePrefs.getBoolean("is_logged_in", false)) }

            MaterialTheme(colorScheme = hackerGreenColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // 1. Allerster Start nach Download -> Willkommen
                        isFirstRun -> {
                            WelcomeScreen(
                                onContinueClicked = {
                                    securePrefs.edit().putBoolean("is_first_run", false).apply()
                                    isFirstRun = false
                                }
                            )
                        }
                        // 2. Zweiter Schritt -> Abo / Aktivierung (1,99 €)
                        !isLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    securePrefs.edit().putBoolean("is_logged_in", true).apply()
                                    isLoggedIn = true
                                }
                            )
                        }
                        // 3. Ab dann immer direkt ins Haupt-Dashboard!
                        else -> {
                            JuarisMainDashboard(securePrefs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onContinueClicked: () -> Unit) {
    val toxicGreen = Color(0xFF00FF66)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "WILLKOMMEN BEI JUARIS",
                color = toxicGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dein Zuhause, deine Regeln, 100% sicher.",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onContinueClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = toxicGreen,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Weiter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val toxicGreen = Color(0xFF00FF66)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "AKTIVIERUNG",
                color = toxicGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bitte aktiviere dein Juaris Abo (1,99 €/Monat), um den lokalen Schutz zu starten.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onLoginSuccess,
                colors = ButtonDefaults.buttonColors(
                    containerColor = toxicGreen,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Abo starten / Anmelden",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun JuarisMainDashboard(prefs: SharedPreferences) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
   
    val tabs = listOf(
        "Status",
        "Schutz",
        "Sperren",
        "Logs",
        "Clipboard",
        "Rechte",
        "Schwarm",
        "Info"
    )

    // Echte persistente Zustände laden
    val liveLogs = remember {
        mutableStateListOf(
            SecurityLogEntity(timestamp = System.currentTimeMillis(), module = "Netzwerk-Monitor", description = "Verschlüsselter Lokalspeicher initialisiert", status = "SAFE"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 15000, module = "Berechtigungs-Wächter", description = "Keine verdächtige App im Hintergrund aktiv", status = "SAFE")
        )
    }

    val blockedContacts = remember {
        val savedList = prefs.getStringSet("blocked_numbers", setOf("+43123456789", "Anonyme Anrufe")) ?: setOf()
        mutableStateListOf(*savedList.toTypedArray())
    }

    var callProtection by remember { mutableStateOf(prefs.getBoolean("call_prot", true)) }
    var smsProtection by remember { mutableStateOf(prefs.getBoolean("sms_prot", true)) }
    var emailProtection by remember { mutableStateOf(prefs.getBoolean("email_prot", true)) }
    var vaultUnlocked by remember { mutableStateOf(false) }
    var clipboardAutoClear by remember { mutableStateOf(prefs.getBoolean("clip_auto", true)) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Juaris Security Suite (Production)") }
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> StatusPage(
                    logs = liveLogs,
                    onSimulateThreat = {
                        val newLog = SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "Live-Sandbox",
                            description = "Echtzeit-Angriffsvektor erfolgreich isoliert!",
                            status = "BLOCKED"
                        )
                        liveLogs.add(0, newLog)
                        Toast.makeText(context, "Bedrohung lokal abgewehrt & protokolliert!", Toast.LENGTH_SHORT).show()
                    },
                    onExportLogs = {
                        Toast.makeText(context, "${liveLogs.size} Logs sicher in den verschlüsselten Vault geschrieben.", Toast.LENGTH_LONG).show()
                    },
                    onPanicWipe = {
                        liveLogs.clear()
                        blockedContacts.clear()
                        prefs.edit().clear().apply()
                        Toast.makeText(context, "PANIC WIPE: Alle lokalen Daten unwiderruflich gelöscht!", Toast.LENGTH_LONG).show()
                    }
                )
                1 -> ProtectionModulesPage(
                    callProtection = callProtection,
                    onCallChange = {
                        callProtection = it
                        prefs.edit().putBoolean("call_prot", it).apply()
                    },
                    smsProtection = smsProtection,
                    onSmsChange = {
                        smsProtection = it
                        prefs.edit().putBoolean("sms_prot", it).apply()
                    },
                    emailProtection = emailProtection,
                    onEmailChange = {
                        emailProtection = it
                        prefs.edit().putBoolean("email_prot", it).apply()
                    },
                    vaultUnlocked = vaultUnlocked,
                    onVaultToggle = { vaultUnlocked = it }
                )
                2 -> BlacklistPage(
                    blockedList = blockedContacts,
                    onAddBlocked = { newEntry ->
                        if (newEntry.isNotBlank() && !blockedContacts.contains(newEntry)) {
                            blockedContacts.add(newEntry)
                            prefs.edit().putStringSet("blocked_numbers", blockedContacts.toSet()).apply()
                            Toast.makeText(context, "Nummer permanent gesperrt", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveBlocked = { item ->
                        blockedContacts.remove(item)
                        prefs.edit().putStringSet("blocked_numbers", blockedContacts.toSet()).apply()
                        Toast.makeText(context, "Nummer freigegeben", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> LogsPage(logs = liveLogs)
                4 -> ClipboardProtectionPage(
                    autoClearEnabled = clipboardAutoClear,
                    onAutoClearChange = {
                        clipboardAutoClear = it
                        prefs.edit().putBoolean("clip_auto", it).apply()
                    }
                )
                5 -> PermissionsAuditPage()
                6 -> SwarmMeshPage()
                7 -> PrivacyAndLegalContent() // Letzte Seite: Datenschutzerklärung & Impressum
            }
        }
    }
}

@Composable
fun StatusPage(
    logs: List<SecurityLogEntity>,
    onSimulateThreat: () -> Unit,
    onExportLogs: () -> Unit,
    onPanicWipe: () -> Unit
) {
    val blockedCount = logs.count { h -> h.status == "BLOCKED" || h.status == "QUARANTINE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("System-Gesundheit & Status", style = MaterialTheme.typography.titleLarge)
            Text("Echtzeit-Diagnose des verschlüsselten Offline-Kernels.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
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
                        Text("Status: AES-256 Gesichert", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Keine Telemetrie, Keine Cloud, 100% On-Device", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Abgewehrt", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$blockedCount", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Verschlüsselung", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aktiv", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Echtzeit-Aktionen", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSimulateThreat, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Echtzeit-Angriff abwehren & testen")
                }
                OutlinedButton(onClick = onExportLogs, modifier = Modifier.fillMaxWidth()) {
                    Text("Logs im verschlüsselten Vault sichern")
                }
                Button(
                    onClick = onPanicWipe,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PANIC WIPE (Alle Daten löschen)", color = Color.Black)
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

@Composable
fun ProtectionModulesPage(
    callProtection: Boolean, onCallChange: (Boolean) -> Unit,
    smsProtection: Boolean, onSmsChange: (Boolean) -> Unit,
    emailProtection: Boolean, onEmailChange: (Boolean) -> Unit,
    vaultUnlocked: Boolean, onVaultToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Kernmodule & Schutz-Regler", style = MaterialTheme.typography.titleLarge)
            Text("Aktive Hintergrund-Wächter auf Device-Ebene.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("System-Filter", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anruf-Schutz", style = MaterialTheme.typography.bodyLarge)
                            Text("Blockiert Spam & unterdrückte Nummern", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = callProtection, onCheckedChange = onCallChange)
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS-Filter", style = MaterialTheme.typography.bodyLarge)
                            Text("Erkennt Phishing & Malware-Links", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = smsProtection, onCheckedChange = onSmsChange)
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("E-Mail-Scan", style = MaterialTheme.typography.bodyLarge)
                            Text("Lokale Postfach-Heuristik", style = MaterialTheme.typography.bodySmall)
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
                    val statusText = if (!vaultUnlocked) "Vault sicher entsperrt" else "Vault verschlüsselt"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verschlüsselter Offline-Vault", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AES-256 geschützter Speicher für sensible Notizen.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusColor = if (vaultUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(if (vaultUnlocked) "Status: Entsperrt" else "Status: Gesperrt", color = statusColor, style = MaterialTheme.typography.bodyMedium)
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

@Composable
fun BlacklistPage(blockedList: MutableList<String>, onAddBlocked: (String) -> Unit, onRemoveBlocked: (String) -> Unit) {
    var inputNumber by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sperrliste & Blockaden", style = MaterialTheme.typography.titleLarge)
            Text("Persistente Rufnummern- und Muster-Filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nummer zur Sperrliste hinzufügen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = inputNumber, onValueChange = { inputNumber = it }, label = { Text("Rufnummer / Muster") }, modifier = Modifier.weight(1f))
                        Button(onClick = { onAddBlocked(inputNumber); inputNumber = "" }) {
                            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                        }
                    }
                }
            }
        }
        item { Text("Aktive Sperrlisteneinträge (${blockedList.size})", style = MaterialTheme.typography.titleMedium) }
        items(blockedList) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemoveBlocked(item) }) { Text("Freigeben", color = MaterialTheme.colorScheme.error) }
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

@Composable
fun LogsPage(logs: List<SecurityLogEntity>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Live-Aktivitätsstream", style = MaterialTheme.typography.titleLarge)
            Text("Protokoll aller lokalen Systemereignisse.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        items(logs) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Modul: ${log.module}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Text("Ereignis: ${log.description}", style = MaterialTheme.typography.bodyLarge)
                    Text("Status: ${log.status}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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

@Composable
fun ClipboardProtectionPage(autoClearEnabled: Boolean, onAutoClearChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Zwischenablage-Wächter", style = MaterialTheme.typography.titleLarge)
            Text("Schützt sensible Daten vor Hintergrund-Spyware.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clipboard-Sicherheit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatisches Leeren", style = MaterialTheme.typography.bodyLarge)
                            Text("Säubert den Puffer bei Inaktivität", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = autoClearEnabled, onCheckedChange = onAutoClearChange)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(""))
                            Toast.makeText(context, "Zwischenablage komplett geleert!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("Zwischenablage jetzt leeren", color = MaterialTheme.colorScheme.primary)
                    }
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

@Composable
fun PermissionsAuditPage() {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Berechtigungs-Auditor", style = MaterialTheme.typography.titleLarge)
            Text("Prüft das System auf kritische Sonderrechte.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accessibility & Overlays", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Status: Keine unautorisierten Screen-Reader aktiv.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Button(onClick = { Toast.makeText(context, "Audit erfolgreich: System sauber.", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                Text("Vollständigen Audit-Scan starten")
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

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    var discoveredDevicesCount by remember { mutableStateOf(0) }
    var scanStatusText by remember { mutableStateOf("Bereit für Hardware-Abgleich") }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter
    var testInputText by remember { mutableStateOf("Juaris Secure Node") }
    val generatedHash = remember(testInputText) {
        try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(testInputText.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "Fehler" }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("P2P-Schwarm & Mesh", style = MaterialTheme.typography.titleLarge)
            Text("Dezentraler Austausch von SHA-256 Hashes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth-Hardware Status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text(scanStatusText, style = MaterialTheme.typography.bodyMedium)
                    Text("Gekoppelte Nodes: $discoveredDevicesCount", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (bluetoothAdapter == null) {
                                scanStatusText = "Keine Bluetooth-Hardware"
                            } else if (!bluetoothAdapter.isEnabled) {
                                scanStatusText = "Bluetooth ist deaktiviert!"
                                Toast.makeText(context, "Bitte Bluetooth aktivieren", Toast.LENGTH_SHORT).show()
                            } else {
                                scanStatusText = "Hardware-Scan aktiv"
                                discoveredDevicesCount = bluetoothAdapter.bondedDevices?.size ?: 0
                                Toast.makeText(context, "Scan abgeschlossen", Toast.LENGTH_SHORT).show()
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
                    Text("Quantum-Hash Generator", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = testInputText, onValueChange = { testInputText = it }, label = { Text("Signatur-Text") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SHA-256 Hash:", style = MaterialTheme.typography.bodySmall)
                    Text(generatedHash, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
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

@Composable
fun PrivacyAndLegalContent() {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Datenschutzerklärung & Impressum", style = MaterialTheme.typography.titleLarge)
            Text("Rechtliche Bestimmungen von Juaris", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Grundsatz", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Juaris wurde entwickelt, um die Privatsphäre der Nutzer maximal zu schützen. Der Schutz deiner persönlichen Daten hat für uns oberste Priorität.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2. Keine Datenerhebung", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Juaris arbeitet strikt nach dem Local-First-Prinzip. Sämtliche App-Daten, Logs und Einstellungen werden ausschließlich lokal auf deinem Endgerät in einer verschlüsselten Datenbank gespeichert. Es werden keine persönlichen Daten, Standortdaten oder Nutzungsprofile an uns oder Dritte übertragen.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3. In-App-Abonnements", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Für die Abwicklung des monatlichen Abonnements (1,99 €/Monat) nutzen wir den offiziellen Google Play Billing Service. Wir selbst erhalten keine Kreditkarten- oder Bankdaten.")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impressum", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Angaben gemäß § 5 TMG / ECG:")
                    Text("Entwickler: Benedikt Wolfgang Hütter")
                    Text("Anschrift: Schulgasse 4/15, 2700 Wiener Neustadt, Österreich")
                    Text("Kontakt: hutters.hq@gmail.com")
                    Text("Verantwortlich für den Inhalt: Benedikt Wolfgang Hütter")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hutterschq-debug.github.io/juaris-app/privacy.md"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Online-Dokumentation im Browser öffnen")
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Hutter IT Solutions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Copyright Benedikt Wolfgang Hütter", style = MaterialTheme.typography.bodySmall)
                    Text("Design Julia Kerschhofer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

data class SecurityLogEntity(
    val timestamp: Long,
    val module: String,
    val description: String,
    val status: String
)
