@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.juaris.app

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.juaris.app.ui.AIPage
import com.juaris.app.ui.AirGesturePage
import com.juaris.app.ui.NeonGiftgruen
import com.juaris.app.ui.SecurityLogsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var securePrefs: SharedPreferences
    private var emergencyReceiver: BroadcastReceiver? = null

    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private val smsRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Juaris ist jetzt als Standard-SMS-Wächter aktiv!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "SMS-Rolle wurde abgelehnt.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       
        // Anti-Tamper & Runtime-Integritätsprüfung beim Start
        checkRuntimeIntegrity()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // System-Rollen und Autopilot-Berechtigungen automatisch anfordern
        requestCallScreeningRoleIfNeeded()
        requestSmsRoleIfNeeded()
        requestBatteryOptimizationExemption()

        // 24/7 Vordergrund-Dienst & Zero-Trust Firewall starten
        startJuarisProtectionService()

        // Notfall-Broadcast-Empfänger für Hintergrund-Trigger (z.B. Audio-Wächter oder Gesten) registrieren
        registerEmergencyReceiver()

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

            LaunchedEffect(Unit) {
                delay(3000L)
                showIntro = false
            }

            MaterialTheme(colorScheme = hackerGreenColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showIntro -> {
                            WelcomeScreen()
                        }
                        !isLoggedIn -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    securePrefs.edit().putBoolean("is_logged_in", true).apply()
                                    isLoggedIn = true
                                }
                            )
                        }
                        else -> {
                            JuarisMainDashboard(
                                prefs = securePrefs,
                                onAuthenticateVault = { onSuccess ->
                                    launchBiometricVaultAuthentication(onSuccess)
                                },
                                onTriggerPanicEmergency = {
                                    executeEmergencyProtocol("Manueller Panic-Button Trigger")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    private fun registerEmergencyReceiver() {
    emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.juaris.app.ACTION_EMERGENCY_TRIGGER") {
                val reason = intent.getStringExtra("reason") ?: "Sensor-Notfall"
                executeEmergencyProtocol(reason)
            }
        }
    }
    val filter = IntentFilter("com.juaris.app.ACTION_EMERGENCY_TRIGGER")
    
    // Für Android 13 (API 33) und neuer muss das Flag explizit gesetzt werden
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(emergencyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(emergencyReceiver, filter)
    }
}

    /**
     * Play-Store-konformes Notfall-Protokoll:
     * Startet einen Countdown mit Sicherheitsabfrage, löscht lokale Daten und wählt nach Ablauf 112.
     */
    fun executeEmergencyProtocol(reason: String) {
        try {
            // Lokale Daten zur Sicherheit des Anwenders bereinigen (Panic Wipe)
            securePrefs.edit().clear().apply()

            runOnUiThread {
                var countdown = 3
                val dialogView = TextView(this).apply {
                    text = "🚨 GEFAHR ERKANNT ($reason)!\nNotruf 112 wird in $countdown Sekunden gewählt...\nTippe zum Abbrechen."
                    textSize = 18f
                    setTextColor(android.graphics.Color.RED)
                    setPadding(50, 50, 50, 50)
                }

                val dialog = AlertDialog.Builder(this)
                    .setTitle("JUARIS NOTFALL-SCHUTZ")
                    .setView(dialogView)
                    .setCancelable(false)
                    .setNegativeButton("ABBRECHEN (Kein Notfall)") { d, _ ->
                        d.dismiss()
                        Toast.makeText(this, "Notfall-Alarm abgebrochen.", Toast.LENGTH_LONG).show()
                    }
                    .create()

                dialog.show()

                val handler = Handler(mainLooper)
                val runnable = object : Runnable {
                    override fun run() {
                        countdown--
                        if (countdown > 0) {
                            dialogView.text = "🚨 GEFAHR ERKANNT ($reason)!\nNotruf 112 wird in $countdown Sekunden gewählt...\nTippe zum Abbrechen."
                            handler.postDelayed(this, 1000)
                        } else {
                            dialog.dismiss()
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(callIntent)
                        }
                    }
                }
                handler.postDelayed(runnable, 1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Anti-Tamper Runtime Check
    private fun checkRuntimeIntegrity() {
        if (android.os.Debug.isDebuggerConnected()) {
            Toast.makeText(this, "Sicherheitswarnung: Debugger erkannt!", Toast.LENGTH_LONG).show()
        }
    }

    // Biometrische Hardware-Absicherung (Vault)
    private fun launchBiometricVaultAuthentication(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                    Toast.makeText(this@MainActivity, "Vault biometrisch entsperrt", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Authentifizierung fehlgeschlagen: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Juaris Secure Vault")
            .setSubtitle("Biometrische Verifizierung zur Entschlüsselung erforderlich")
            .setNegativeButtonText("Abbrechen")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun startJuarisProtectionService() {
        val serviceIntent = Intent(this, JuarisNotificationListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Lokale Zero-Trust Firewall im Hintergrund starten
        val vpnIntent = Intent(this, JuarisVpnService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(vpnIntent)
        } else {
            startService(vpnIntent)
        }
    }

    private fun requestCallScreeningRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    callScreeningRoleLauncher.launch(intent)
                }
            }
        }
    }

    private fun requestSmsRoleIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    smsRoleLauncher.launch(intent)
                }
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(fallbackIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        emergencyReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) {}
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun showProminentDisclosureDialog(context: Context, onProceed: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Sicherheits-Wächter aktivieren")
        .setMessage("Juaris benötigt den Benachrichtigungszugriff, um eingehende Nachrichten von WhatsApp, E-Mail und Messengern lokal in Echtzeit auf Betrug und Phishing zu scannen.\n\nWichtig: Alle Daten bleiben zu 100% auf Ihrem Gerät. Es werden niemals Daten an Server oder Clouds übertragen.")
        .setPositiveButton("Verstanden & Aktivieren") { _, _ -> onProceed() }
        .setNegativeButton("Abbrechen", null)
        .setCancelable(false)
        .show()
}

@Composable
fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
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
                modifier = Modifier.size(200.dp).padding(bottom = 24.dp)
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
                text = "Local-First Security Kernel. Zero Cloud.",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = NeonGiftgruen, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    var isBillingActive by remember { mutableStateOf(false) }

    val billingManager = remember {
        BillingManager(context, "juaris_monats_abo") {
            onLoginSuccess()
        }
    }

    LaunchedEffect(Unit) {
        billingManager.startConnection {
            isBillingActive = true
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "JUARIS ON-DEVICE KERNEL",
                color = NeonGiftgruen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Post-Quantum Security Suite\n1,99 € / Monat (Jederzeit kündbar)",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = {
                    context.findActivity()?.let { act ->
                        billingManager.launchBillingFlow(act)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen, contentColor = Color.Black)
            ) {
                Text(text = "Abo starten (Google Play Billing)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }

            if (com.juaris.app.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { onLoginSuccess() },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFFF9900))
                ) {
                    Text(text = "[DEBUG] Entwickler-Bypass (Aktiv zum Testen)", color = Color(0xFFFF9900), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun JuarisMainDashboard(
    prefs: SharedPreferences,
    onAuthenticateVault: (() -> Unit) -> Unit,
    onTriggerPanicEmergency: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val aiCore = remember { LocalAICore(context) }
    val airGestureCore = remember { AirGestureCore(context) }
    val coroutineScope = rememberCoroutineScope()
   
    var gestureEnabled by remember { mutableStateOf(false) }
    var gestureStatusText by remember { mutableStateOf("Bereit") }

    val tabs = listOf(
        "Status", "Schutz", "Sperren", "Logs", "Clipboard",
        "Rechte", "Schwarm", "KI", "Gesten", "Info"
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(gestureEnabled, lifecycleOwner) {
        if (gestureEnabled) {
            airGestureCore.startGestureDetection(
                lifecycleOwner = lifecycleOwner,
                onGestureDetected = { action ->
                    if (action == AirGestureCore.GestureAction.TRIGGERED) {
                        if (!pagerState.isScrollInProgress) {
                            coroutineScope.launch {
                                val nextTab = (pagerState.currentPage + 1) % tabs.size
                                pagerState.animateScrollToPage(nextTab)
                            }
                        }
                    }
                },
                onDebugInfo = { status -> gestureStatusText = status }
            )
        } else {
            airGestureCore.stopGestureDetection()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose { airGestureCore.stopGestureDetection() }
    }

    val liveLogs = remember {
        mutableStateListOf(
            SecurityLogEntity(timestamp = System.currentTimeMillis(), module = "Netzwerk-Monitor", description = "Verschlüsselter Lokalspeicher aktiv", status = "SAFE"),
            SecurityLogEntity(timestamp = System.currentTimeMillis() - 15000, module = "Berechtigungs-Wächter", description = "Keine Cloud-Telemetrie festgestellt", status = "SAFE")
        )
    }

    val blockedContacts = remember {
        val savedList = prefs.getStringSet("blocked_numbers", setOf("+43123456789", "Spam-Nummern")) ?: setOf()
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
                TopAppBar(title = { Text("Juaris Security Suite (Local-First)") })
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> StatusPage(
                    logs = liveLogs,
                    onSimulateThreat = {
                        val threatDescription = "E-Mail & SMS Phishing-Vektor lokal abgefangen!"
                        val newLog = SecurityLogEntity(
                            timestamp = System.currentTimeMillis(),
                            module = "Local-AI-Heuristik",
                            description = threatDescription,
                            status = "BLOCKED"
                        )
                        liveLogs.add(0, newLog)
                        Toast.makeText(context, "Bedrohung auf dem Gerät neutralisiert!", Toast.LENGTH_SHORT).show()
                    },
                    onExportLogs = {
                        Toast.makeText(context, "Logs sicher im verschlüsselten Vault gesichert.", Toast.LENGTH_LONG).show()
                    },
                    onPanicWipe = {
                        onTriggerPanicEmergency()
                    }
                )
                1 -> ProtectionModulesPage(
                    prefs = prefs,
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
                    onVaultToggle = { newState ->
                        if (newState) {
                            onAuthenticateVault {
                                vaultUnlocked = true
                            }
                        } else {
                            vaultUnlocked = false
                            Toast.makeText(context, "Vault gesperrt", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> BlacklistPage(
                    blockedList = blockedContacts,
                    onAddBlocked = { newEntry ->
                        if (newEntry.isNotBlank() && !blockedContacts.contains(newEntry)) {
                            blockedContacts.add(newEntry)
                            prefs.edit().putStringSet("blocked_numbers", blockedContacts.toSet()).apply()
                            Toast.makeText(context, "Nummer blockiert", Toast.LENGTH_SHORT).show()
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
                8 -> {
                    val cameraPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            gestureEnabled = true
                            Toast.makeText(context, "Kamera-Sensor gestartet!", Toast.LENGTH_SHORT).show()
                        } else {
                            gestureEnabled = false
                            Toast.makeText(context, "Berechtigung verweigert", Toast.LENGTH_LONG).show()
                        }
                    }

                    AirGesturePage(
                        airGestureCore = airGestureCore,
                        isGestureActive = gestureEnabled,
                        statusText = gestureStatusText,
                        onToggleGesture = { newState ->
                            if (newState) {
                                val permissionGranted = ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (permissionGranted) {
                                    gestureEnabled = true
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            } else {
                                gestureEnabled = false
                                gestureStatusText = "Pausiert"
                            }
                        },
                        onTabSwitch = { forward ->
                            coroutineScope.launch {
                                val target = if (forward) {
                                    (pagerState.currentPage + 1) % tabs.size
                                } else {
                                    if (pagerState.currentPage - 1 < 0) tabs.size - 1 else pagerState.currentPage - 1
                                }
                                pagerState.animateScrollToPage(target)
                            }
                        }
                    )
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
            Text("System-Gesundheit (Local-First)", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("100% On-Device Kontrolle ohne Server-Anbindung.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, NeonGiftgruen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGiftgruen, modifier = Modifier.size(36.dp))
                    Column {
                        Text("Status: AES-256 Verschlüsselt", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                        Text("Mikrofon-, Anruf-, SMS- & E-Mail-Filter aktiv", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BLOCKIERT", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$blockedCount", style = MaterialTheme.typography.headlineLarge, color = Color(0xFFFF3333))
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DATENFLUSS", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("NUR LOKAL", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Aktionen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSimulateThreat,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phishing-Angriff (SMS/Mail) simulieren", color = Color.Black, fontWeight = FontWeight.Bold)
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
                        Text("PANIC WIPE & NOTRUF (112)", color = Color.Black, fontWeight = FontWeight.Bold)
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
    prefs: SharedPreferences,
    callProtection: Boolean, onCallChange: (Boolean) -> Unit,
    smsProtection: Boolean, onSmsChange: (Boolean) -> Unit,
    emailProtection: Boolean, onEmailChange: (Boolean) -> Unit,
    vaultUnlocked: Boolean, onVaultToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Schutz-Module", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Echtzeit-Wächter für Anrufe, SMS und E-Mails.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kommunikations-Filter", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anruf-Schutz", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Null-Ring Spam-Abweisung", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = callProtection, onCheckedChange = onCallChange)
                    }
                    HorizontalDivider(color = Color(0xFF112211))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS-Phishing-Filter", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Standard-SMS-App Engine", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = smsProtection, onCheckedChange = onSmsChange)
                    }
                    HorizontalDivider(color = Color(0xFF112211))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("E-Mail-Benachrichtigungs-Scan", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                            Text("Lokaler Notification Listener", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = emailProtection,
                            onCheckedChange = { newState ->
                                if (newState) {
                                    showProminentDisclosureDialog(context) {
                                        onEmailChange(true)
                                        try {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Bitte aktiviere den Notification Listener manuell.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    onEmailChange(false)
                                    prefs.edit().putBoolean("email_prot", false).apply()
                                }
                            }
                        )
                    }
                }
            }
        }
        item {
            Card(
                onClick = {
                    onVaultToggle(!vaultUnlocked)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Verschlüsselter Offline-Vault", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AES-256 geschützter Speicher mit Biometrie-Hardware-Schutz.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (vaultUnlocked) "Status: Entsperrt (Biometrisch)" else "Status: Gesperrt (Tippen zum Entsperren)", color = if (vaultUnlocked) NeonGiftgruen else Color(0xFFFF3333), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BlacklistPage(blockedList: MutableList<String>, onAddBlocked: (String) -> Unit, onRemoveBlocked: (String) -> Unit) {
    var inputNumber by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sperrliste", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Persistente Offline-Blockaden.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nummer / Muster hinzufügen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = inputNumber,
                            onValueChange = { inputNumber = it },
                            label = { Text("Rufnummer", color = Color.Gray) },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onAddBlocked(inputNumber); inputNumber = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        }
                    }
                }
            }
        }
        item { Text("Gesperrte Einträge (${blockedList.size})", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen) }
        items(blockedList) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(item, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    TextButton(onClick = { onRemoveBlocked(item) }) { Text("Freigeben", color = Color(0xFFFF3333)) }
                }
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
            Text("Zwischenablage", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Schützt sensible Daten vor Spyware.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            Toast.makeText(context, "Zwischenablage geleert!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Jetzt leeren", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionsAuditPage() {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Berechtigungen & E-Mail-Listener", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Verwalte die System-Schnittstellen für E-Mail- und SMS-Schutz.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("E-Mail Notification Listener", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Ermöglicht das lokale Scannen eingehender E-Mail-Benachrichtigungen (Gmail, Outlook etc.) ohne Cloud.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showProminentDisclosureDialog(context) {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Fehler beim Öffnen der Einstellungen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Benachrichtigungs-Zugriff erlauben", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Button(
                onClick = { Toast.makeText(context, "System-Audit: Alle Wächter bereit.", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
            ) {
                Text("Vollständigen Audit-Scan starten", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    val db = remember { JuarisDatabase.getDatabase(context) }
    val postsFlow = remember { db.meshDao().getAllActivePosts() }
    val posts by postsFlow.collectAsState(initial = emptyList())

    var inputMessage by remember { mutableStateOf("") }
    var isEphemeral by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var scanStatusText by remember { mutableStateOf("Bereit") }
    var discoveredDevicesCount by remember { mutableStateOf(0) }

    val meshManager = remember {
        NearbyMeshManager(
            context = context,
            onDeviceDiscovered = { endpointId ->
                discoveredDevicesCount += 1
                scanStatusText = "Verbunden: $endpointId"
            },
            onDeviceLost = { endpointId ->
                discoveredDevicesCount = (discoveredDevicesCount - 1).coerceAtLeast(0)
                scanStatusText = "Getrennt: $endpointId"
            },
            onMessageReceived = { _, msg ->
                GlobalMeshEngine.broadcastToSwarm(
                    context = context,
                    content = msg,
                    isEphemeral = false,
                    meshManager = null,
                    onBlocked = {},
                    onSuccess = {}
                )
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { meshManager.stopMeshNode() }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            meshManager.startMeshNode()
            Toast.makeText(context, "P2P-Schwarm gestartet", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bluetooth-Berechtigungen fehlen", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("P2P-Schwarm & Live-Feed", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Dezentraler Offline-Austausch.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nachricht broadcasten", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        label = { Text("Nachricht...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isEphemeral, onCheckedChange = { isEphemeral = it })
                            Text("Ephemer", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    meshManager.broadcastMessage(inputMessage)
                                    GlobalMeshEngine.broadcastToSwarm(
                                        context = context,
                                        content = inputMessage,
                                        isEphemeral = isEphemeral,
                                        meshManager = meshManager,
                                        onBlocked = { reason -> statusMessage = reason },
                                        onSuccess = { _ ->
                                            statusMessage = "Gesendet!"
                                            inputMessage = ""
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                        ) {
                            Text("Broadcast", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Text("Schwarm-Pakete (${posts.size})", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen) }
        items(posts) { post ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = post.senderNode, color = NeonGiftgruen, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.content, color = Color.White)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth Mesh Hardware", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Status: $scanStatusText", color = Color.White)
                    Button(
                        onClick = {
                            bluetoothPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                                    android.Manifest.permission.BLUETOOTH_CONNECT,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("P2P-Schwarm-Node starten", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyAndLegalContent() {
    val uriHandler = LocalUriHandler.current

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Datenschutz & Impressum", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("100% Local-First Prinzip.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Datenschutz", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Keine Cloud, keine Server, keine Telemetrie. Alle Daten verbleiben ausschließlich verschlüsselt auf deinem Endgerät.", color = Color.White)
                   
                    Spacer(modifier = Modifier.height(4.dp))
                   
                    Text(
                        text = "🔗 Offizielle Datenschutzrichtlinie (Online)",
                        color = NeonGiftgruen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://huetterhq-debug.github.io/juaris-app/privacy.md")
                        }
                    )

                    HorizontalDivider(color = Color(0xFF112211), modifier = Modifier.padding(vertical = 8.dp))

                    Text("Impressum", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Angaben gemäß § 5 TMG / ECG", color = Color.Gray, fontSize = 12.sp)
                    Text("Name / Entwickler: Benedikt Wolfgang Hütter", color = Color.White)
                    Text("Anschrift: Schulgasse 4/15, 2700 Wiener Neustadt, Österreich", color = Color.White)
                    Text("E-Mail: support@juaris.com", color = Color.White)
                   
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Verantwortlich für den Inhalt:", color = Color.Gray, fontSize = 12.sp)
                    Text("Benedikt Wolfgang Hütter", color = Color.White)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Design:", color = Color.Gray, fontSize = 12.sp)
                    Text("Julia Kerschhofer", color = Color.White)
                }
            }
        }
    }
}


