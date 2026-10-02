@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.juaris.app

import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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
            Toast.makeText(this, getString(R.string.toast_sms_active), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_sms_rejected), Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.toast_notification_granted), Toast.LENGTH_SHORT).show()
            checkAndBootProtectionServices()
        } else {
            Toast.makeText(this, getString(R.string.toast_notification_warning), Toast.LENGTH_LONG).show()
        }
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.toast_calendar_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_calendar_warning), Toast.LENGTH_SHORT).show()
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, getString(R.string.toast_vpn_granted), Toast.LENGTH_SHORT).show()
            startVpnServiceInternal()
        } else {
            Toast.makeText(this, getString(R.string.toast_vpn_rejected), Toast.LENGTH_LONG).show()
        }
    }

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

        checkRuntimeIntegrity()
        checkAndBootProtectionServices()
        registerEmergencyReceiver()

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
            var isLoggedIn by remember { mutableStateOf(false) }

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
                                    executeEmergencyProtocol(getString(R.string.emergency_reason_manual))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndBootProtectionServices()
    }

    private fun checkAndBootProtectionServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        checkAndRequestVpnPermission()
    }

    private fun checkAndRequestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            startVpnServiceInternal()
        }
    }

    private fun startVpnServiceInternal() {
        try {
            val intent = Intent(this, JuarisVpnService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkRuntimeIntegrity() {
        // 1. Lokaler Check auf unbefugte Debugger-Verbindungen (Abwehr von Reverse-Engineering)
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            executeEmergencyProtocol("Sicherheitsverletzung: Unbefugter Debugger angehängt!")
            return
        }

        // 2. Lokaler Root-Erkennungs-Kernel (Sucht nach su-Binaries auf Geräteebene)
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
        )
        for (path in rootPaths) {
            if (File(path).exists()) {
                executeEmergencyProtocol("Systemintegrität kompromittiert: Root-Rechte (su) lokal detektiert!")
                return
            }
        }

        // 3. Lokale Erkennung von Dynamic-Memory-Injektionen (z. B. Frida-Server-Prozesse im RAM)
        try {
            val process = Runtime.getRuntime().exec("ps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line != null && (line!!.contains("frida") || line!!.contains("magisk") || line!!.contains("xposed"))) {
                    executeEmergencyProtocol("Angriff abgewehrt: Aktive Runtime-Injektionswerkzeuge im Speicher gefunden!")
                    return
                }
            }
            reader.close()
        } catch (e: Exception) {
            // Falls das System den ps-Befehl einschränkt, läuft der Schutz isoliert weiter
        }
    }

    private fun registerEmergencyReceiver() {
        if (emergencyReceiver == null) {
            emergencyReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val reason = intent?.getStringExtra("reason") ?: "System-Notfall"
                    executeEmergencyProtocol(reason)
                }
            }
            val filter = IntentFilter("com.juaris.app.TRIGGER_EMERGENCY")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(emergencyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(emergencyReceiver, filter)
            }
        }
    }

    private fun executeEmergencyProtocol(reason: String) {
        EmergencyActivity.triggerEmergencyAlarm(this, reason)
    }

    private fun launchBiometricVaultAuthentication(onSuccess: () -> Unit) {
        val biometricManager = JuarisBiometricManager(this)
        biometricManager.authenticateUser(
            onSuccess = onSuccess,
            onError = { err -> Toast.makeText(this, err, Toast.LENGTH_LONG).show() }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        emergencyReceiver?.let {
            unregisterReceiver(it)
            emergencyReceiver = null
        }
    }

    // =================================================================
    // JETPACK COMPOSE UI-KOMPONENTEN
    // =================================================================
    @Composable
    fun WelcomeScreen() {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JUARIS", color = NeonGiftgruen, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Zero-Cloud Protection Kernel", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }

    @Composable
    fun LoginScreen(onLoginSuccess: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("Tresor gesperrt", color = NeonGiftgruen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launchBiometricVaultAuthentication(onLoginSuccess) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen, contentColor = Color.Black)
                ) {
                    Text("Biometrisch entsperren", fontWeight = FontWeight.Bold)
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
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("JUARIS CONSOLE", color = NeonGiftgruen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    SecurityLogsPage()
                }
            }
        }
    }
}
