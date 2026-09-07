package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage(
    airGestureCore: AirGestureCore,
    onNavigate: (Boolean) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Live-Daten sicher aus der Core abgreifen
    val gestureState by airGestureCore.gestureState.collectAsState()
    val lastAction by airGestureCore.lastAction.collectAsState()

    // Kamera-Erkennung beim Betreten der Seite starten und beim Verlassen stoppen
    DisposableEffect(lifecycleOwner) {
        airGestureCore.startGestureDetection(lifecycleOwner) { action ->
            when (action) {
                AirGestureCore.GestureAction.SWIPE_RIGHT -> onNavigate(true) // Nächster Tab
                AirGestureCore.GestureAction.SWIPE_LEFT -> onNavigate(false) // Vorheriger Tab
                else -> {}
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
                Text(
                    text = "🟢 Kamera-Schutz aktiv (Permanent)",
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hinweis: Es werden zu keiner Zeit Bilder oder Videos gespeichert oder an Server gesendet. Die Analyse erfolgt ausschließlich im Arbeitsspeicher deines Geräts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

