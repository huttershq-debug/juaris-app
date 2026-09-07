package com.juaris.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage(
    airGestureCore: AirGestureCore,
    onNavigate: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
   
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val gestureState by airGestureCore.gestureState.collectAsState()
    val lastAction by airGestureCore.lastAction.collectAsState()

    DisposableEffect(lifecycleOwner, hasCameraPermission) {
        if (hasCameraPermission) {
            airGestureCore.startGestureDetection(lifecycleOwner) { action ->
                when (action) {
                    AirGestureCore.GestureAction.SWIPE_RIGHT -> onNavigate(true)
                    AirGestureCore.GestureAction.SWIPE_LEFT -> onNavigate(false)
                    else -> {}
                }
            }
        }
        onDispose {
            airGestureCore.stopGestureDetection()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Air-Swiping Gesten-Steuerung",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Bediene Juaris berührungslos über die Frontkamera. Läuft permanent im Hintergrund (100% lokal & offline).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasCameraPermission) {
                    Text(
                        text = "⚠️ Kamera-Berechtigung erforderlich für Gesten!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF3333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Berechtigung erteilen", color = Color.Black)
                    }
                } else {
                    Text(
                        text = "🟢 Kamera-Schutz & Gesten aktiv",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                   
                    Text(
                        text = "Sensor-Feed: $gestureState",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                   
                    Text(
                        text = "Letzte Aktion: $lastAction",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hinweis: Es werden zu keiner Zeit Bilder oder Videos gespeichert oder an Server gesendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

