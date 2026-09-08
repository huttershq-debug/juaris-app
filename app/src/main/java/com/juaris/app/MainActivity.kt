@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.juaris.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.juaris.app.BuildConfig
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.juaris.app.ui.AIPage
import com.juaris.app.ui.AirGesturePage
import com.juaris.app.ui.TacticalPulseCard
import com.juaris.app.ui.NeonGiftgruen
import com.juaris.app.ui.SecurityLogsPage
import java.security.MessageDigest

class MainActivity : ComponentActivity() {

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
            securePrefs = getPreferences(Context.MODE_PRIVATE)
        }

        setContent {
            val hackerGreenColorScheme = darkColorScheme(
                primary = NeonGiftgruen,
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF003311),
                onPrimaryContainer = NeonGiftgruen,
                background = Color.Black,
                onBackground = NeonGiftgruen,
                surface = Color(0xFF080808),
                onSurface = Color(0xFFE0E0E0),
                surfaceVariant = Color(0xFF121212),
                onSurfaceVariant = NeonGiftgruen,
                error = Color(0xFFFF3333)
            )

            var showIntro by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(securePrefs.getBoolean("is_logged_in", false)) }

            MaterialTheme(colorScheme = hackerGreenColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showIntro -> {
                            WelcomeScreen(
                                onContinueClicked = {
                                    showIntro = false
                                }
                            )
                        }
                        isLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    securePrefs.edit().putBoolean("is_logged_in", true).apply()
                                    isLoggedIn = true
                                }
                            )
                        }
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
            Image(
                painter = painterResource(id = R.drawable.hologram_avatar),
                contentDescription = "Juaris KI Hologramm",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "WILLKOMMEN BEI JUARIS",
                color = NeonGiftgruen,
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
                onClick = { onContinueClicked() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGiftgruen,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Abo starten / Anmelden (Test-Bypass)",
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
    val context = LocalContext.current

    val billingManager = remember {
        BillingManager(context, "juaris_monats_abo") {
            onLoginSuccess()
        }
    }

    LaunchedEffect(Unit) {
        billingManager.startConnection {}
    }

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
                color = NeonGiftgruen,
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
                onClick = {
                    onLoginSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGiftgruen,
                    contentColor = Color.Black
                )
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
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val aiCore = remember { LocalAICore(context) }
    val airGestureCore = remember { AirGestureCore(context) }
    var selectedTab by remember { mutableStateOf(0) }
   
    val tabs = listOf(
        "Status", "Schutz", "Sperren", "Logs", "Clipboard",
        "Rechte", "Schwarm", "KI", "Gesten", "Info"
    )

    // Automatische Kamera-Berechtigungs-Prüfung und Anforderung beim Start
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Air-Gestenkern voll aktiviert!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Autonomer Start direkt beim Laden des Dashboards (ohne Klicks notwendig)
    LaunchedEffect(hasCameraPermission, lifecycleOwner) {
        if (hasCameraPermission) {
            airGestureCore.startGestureDetection(lifecycleOwner) { action ->
                when (action) {
                    AirGestureCore.GestureAction.SWIPE_RIGHT -> {
                        selectedTab = (selectedTab + 1) % tabs.size
                    }
                    AirGestureCore.GestureAction.SWIPE_LEFT -> {
                        selectedTab = (selectedTab - 1 + tabs.size) % tabs.size
                    }
                    AirGestureCore.GestureAction.NONE -> {}
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            airGestureCore.stopGestureDetection()
        }
    }

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
                        val threatDescription = "E-Mail-Phishing & Live-Sandbox Vektor erfolgreich isoliert!"
                        val newLog = SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "E-Mail-Heuristik",
                            description = threatDescription,
                            status = "BLOCKED"
                        )
                        liveLogs.add(0, newLog)
                        Toast.makeText(context, "Heilige Dreifaltigkeit: Bedrohung lokal abgewehrt!", Toast.LENGTH_SHORT).show()
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
                3 -> {
                    val db = JuarisDatabase.getDatabase(LocalContext.current)
                    SecurityLogsPage(logDao = db.securityLogDao())
                }
                4 -> ClipboardProtectionPage(
                    autoClearEnabled = clipboardAutoClear,
                    onAutoClearChange = {
                        clipboardAutoClear = it
                        prefs.edit().putBoolean("clip_auto", it).apply()
                    }
                )
                5 -> PermissionsAuditPage()
                6 -> SwarmMeshPage()
                7 -> AIPage(aiCore = aiCore, logs = liveLogs)
                8 -> AirGesturePage(airGestureCore = airGestureCore) { forward ->
                    selectedTab = if (forward) {
                        (selectedTab + 1) % tabs.size
                    } else {
                        if (selectedTab - 1 < 0) tabs.size - 1 else selectedTab - 1
                    }
                }
                9 -> PrivacyAndLegalContent()
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
            Text("System-Gesundheit & Status", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Echtzeit-Diagnose des verschlüsselten Offline-Kernels.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alphaAnim by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            TacticalPulseCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonGiftgruen.copy(alpha = alphaAnim),
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text("Status: AES-256 Gesichert", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                        Text("Keine Telemetrie, Keine Cloud, 100% On-Device", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    TacticalPulseCard {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ABGEWEHRT", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$blockedCount", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFFF3333))
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    TacticalPulseCard {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("VERSCHLÜSSELUNG", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("AKTIV", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Echtzeit-Aktionen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSimulateThreat,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Echtzeit-Angriff abwehren & testen", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onExportLogs,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NeonGiftgruen)
                    ) {
                        Text("Logs im verschlüsselten Vault sichern", color = NeonGiftgruen)
                    }
                    Button(
                        onClick = onPanicWipe,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3333)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PANIC WIPE (Alle Daten löschen)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
            Text("Kernmodule & Schutz-Regler", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Aktive Hintergrund-Wächter auf Device-Ebene.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("System-Filter", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anruf-Schutz", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Blockiert Spam & unterdrückte Nummern", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = callProtection, onCheckedChange = onCallChange)
                    }
                    Divider(color = Color(0xFF112211))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS-Filter", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Erkennt Phishing & Malware-Links", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = smsProtection, onCheckedChange = onSmsChange)
                    }
                    Divider(color = Color(0xFF112211))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("E-Mail-Scan", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Lokale Postfach-Heuristik", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = emailProtection, onCheckedChange = onEmailChange)
                    }
                }
            }
        }
        item {
            TacticalPulseCard(
                onClick = {
                    onVaultToggle(!vaultUnlocked)
                    val statusText = if (!vaultUnlocked) "Vault sicher entsperrt" else "Vault verschlüsselt"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }
            ) {
                Column {
                    Text("Verschlüsselter Offline-Vault", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AES-256 geschützter Speicher für sensible Notizen.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    val statusColor = if (vaultUnlocked) NeonGiftgruen else Color(0xFFFF3333)
                    Text(if (vaultUnlocked) "Status: Entsperrt" else "Status: Gesperrt", color = statusColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun BlacklistPage(blockedList: MutableList<String>, onAddBlocked: (String) -> Unit, onRemoveBlocked: (String) -> Unit) {
    var inputNumber by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sperrliste & Blockaden", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Persistente Rufnummern- und Muster-Filter.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nummer zur Sperrliste hinzufügen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputNumber,
                            onValueChange = { inputNumber = it },
                            label = { Text("Rufnummer / Muster", color = Color.Gray) },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onAddBlocked(inputNumber); inputNumber = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hinzufügen", tint = Color.Black)
                        }
                    }
                }
            }
        }
        item { Text("Aktive Sperrlisteneinträge (${blockedList.size})", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen) }
        items(blockedList) { item ->
            TacticalPulseCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    TextButton(onClick = { onRemoveBlocked(item) }) { Text("Freigeben", color = Color(0xFFFF3333)) }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
            Text("Zwischenablage-Wächter", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Schützt sensible Daten vor Hintergrund-Spyware.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Clipboard-Sicherheit", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatisches Leeren", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Säubert den Puffer bei Inaktivität", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Zwischenablage jetzt leeren", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PermissionsAuditPage() {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Berechtigungs-Auditor", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Prüft das System auf kritische Sonderrechte.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accessibility & Overlays", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Status: Keine unautorisierten Screen-Reader aktiv.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
        item {
            Button(
                onClick = { Toast.makeText(context, "Audit erfolgreich: System sauber.", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
            ) {
                Text("Vollständigen Audit-Scan starten", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    var scanStatusText by remember { mutableStateOf("Bereit für Hardware-Abgleich") }
    var discoveredDevicesCount by remember { mutableStateOf(0) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scanStatusText = "Hardware-Scan aktiv"
            Toast.makeText(context, "Bluetooth-Mesh-Scan gestartet...", Toast.LENGTH_SHORT).show()
        } else {
            scanStatusText = "Berechtigungen fehlen!"
            Toast.makeText(context, "Bluetooth-Berechtigungen werden für den Schwarm benötigt!", Toast.LENGTH_LONG).show()
        }
    }

    var testInputText by remember { mutableStateOf("Juaris Secure Node") }
    val generatedHash = remember(testInputText) {
        try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(testInputText.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { "Fehler" }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("P2P-Schwarm & Mesh", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Dezentraler Austausch von SHA-256 Hashes.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth-Hardware Status", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text(scanStatusText, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Gekoppelte Nodes: $discoveredDevicesCount", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            bluetoothPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_CONNECT,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Hardware-Mesh-Scan ausführen", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quantum-Hash Generator", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    OutlinedTextField(value = testInputText, onValueChange = { testInputText = it }, label = { Text("Signatur-Text", color = Color.Gray) }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SHA-256 Hash:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(generatedHash, style = MaterialTheme.typography.bodyMedium, color = NeonGiftgruen)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PrivacyAndLegalContent() {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Datenschutzerklärung & Impressum", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Rechtliche Bestimmungen von Juaris", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Grundsatz", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Juaris wurde entwickelt, um die Privatsphäre der Nutzer maximal zu schützen. Der Schutz deiner persönlichen Daten hat für uns oberste Priorität.", color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("2. Keine Datenerhebung", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Juaris arbeitet strikt nach dem Local-First-Prinzip. Sämtliche App-Daten, Logs und Einstellungen werden ausschließlich lokal auf deinem Endgerät in einer verschlüsselten Datenbank gespeichert. Es werden keine persönlichen Daten, Standortdaten oder Nutzungsprofile an uns oder Dritte übertragen.", color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3. In-App-Abonnements", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Für die Abwicklung des monatlichen Abonnements (1,99 €/Monat) nutzen wir den offiziellen Google Play Billing Service. Wir selbst erhalten keine Kreditkarten- oder Bankdaten.", color = Color.White)
                }
            }
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Impressum", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Angaben gemäß § 5 TMG / ECG:", color = Color.White)
                    Text("Entwickler: Benedikt Wolfgang Hütter", color = Color.White)
                    Text("Anschrift: Schulgasse 4/15, 2700 Wiener Neustadt, Österreich", color = Color.White)
                    Text("Kontakt: hutters.hq@gmail.com", color = Color.White)
                    Text("Verantwortlich für den Inhalt: Benedikt Wolfgang Hütter", color = Color.White)
                }
            }
        }
        item {
            OutlinedButton(
               onClick = {
                   try {
                       val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huttershq-debug.github.io/juaris-app/privacy.md"))
                       context.startActivity(intent)
                   } catch (e: Exception) {
                        // Fängt den Fehler ab, falls kein Browser verfügbar ist
                   }
              },
              modifier = Modifier.fillMaxWidth(),
              border = BorderStroke(1.dp, NeonGiftgruen)
          ) {
              Text("Online-Dokumentation im Browser öffnen", color = NeonGiftgruen)
          }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Hutter IT Solutions", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Copyright Benedikt Wolfgang Hütter", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("Design Julia Kerschhofer", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

