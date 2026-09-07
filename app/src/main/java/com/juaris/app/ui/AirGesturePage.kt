package com.juaris.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage(airGestureCore: AirGestureCore, onTabSwipe: (Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val gestureState by airGestureCore.gestureState.collectAsState()
    val lastAction by airGestureCore.lastAction.collectAsState()

    var isEnabled by remember { mutableStateOf(false) }

    // Kamera-Berechtigung anfordern
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isEnabled = true
            airGestureCore.startGestureDetection(lifecycleOwner) { action ->
                if (action == AirGestureCore.GestureAction.SWIPE_RIGHT) {
                    onTabSwipe(true) // Nächster Tab
                } else if (action == AirGestureCore.GestureAction.SWIPE_LEFT) {
                    onTabSwipe(false) // Vorheriger Tab
                }
            }
            Toast.makeText(context, "Gesten-Steuerung aktiviert", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Kamera-Berechtigung für Gesten erforderlich!", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Air-Swiping Gesten-Steuerung", style = MaterialTheme.typography.titleLarge)
            Text("Bediene Juaris berührungslos über die Frontkamera (100% lokal & offline).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
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
                        Text("Status der Kamera", style = MaterialTheme.typography.titleMedium)
                        Text(if (isEnabled) "AKTIV" else "INAKTIV", color = if (isEnabled) Color(0xFF00FF66) else Color(0xFFFF3333), fontWeight = FontWeight.Bold)
                    }
                    Divider(color = MaterialTheme.colorScheme.surface)
                    Text("Sensor-Feed: $gestureState", style = MaterialTheme.typography.bodyMedium)
                    Text("Letzte Aktion: $lastAction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (!isEnabled) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        isEnabled = false
                        airGestureCore.stop()
                        Toast.makeText(context, "Gesten-Steuerung gestoppt", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) Color(0xFFFF3333) else Color(0xFF00FF66),
                    contentColor = Color.Black
                )
            ) {
                Text(if (isEnabled) "Gesten-Steuerung ausschalten" else "Gesten-Steuerung einschalten", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter IT Solutions & Design Julia Kerschhofer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
