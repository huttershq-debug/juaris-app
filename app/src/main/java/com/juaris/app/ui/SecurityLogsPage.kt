package com.juaris.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage() {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val airGestureCore = remember { AirGestureCore(context) }
    val gestureState by airGestureCore.gestureState.collectAsState()
    val lastAction by airGestureCore.lastAction.collectAsState()

    // Startet die Gestensteuerung vollautomatisch und permanent im Hintergrund
    LaunchedEffect(Unit) {
        if (lifecycleOwner != null) {
            airGestureCore.startGestureDetection(lifecycleOwner) { action ->
                when (action) {
                    AirGestureCore.GestureAction.SWIPE_LEFT -> {}
                    AirGestureCore.GestureAction.SWIPE_RIGHT -> {}
                    AirGestureCore.GestureAction.NONE -> {}
                }
            }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = "Kamera-Schutz aktiv (Permanent)",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "Sensor-Feed: $gestureState",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Letzte Aktion: $lastAction",
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hinweis: Es werden zu keiner Zeit Bilder oder Videos gespeichert oder an Server gesendet. Die Analyse erfolgt ausschließlich im Arbeitsspeicher deines Geräts – genau wie eine native Smartphone-Funktion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

