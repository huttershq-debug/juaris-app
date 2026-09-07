package com.juaris.app.ui // Passe das Package an dein Projekt an, falls es direkt in com.juaris.app liegt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Core-Instanz merken, damit sie bei Neuladen nicht neu erstellt wird
    val gestureCore = remember { AirGestureCore(context) }
    
    // Live-Daten aus der Core abgreifen
    val gestureState by gestureCore.gestureState.collectAsState()
    val lastAction by gestureCore.lastAction.collectAsState()

    // Gestenerkennung beim Betreten der Seite starten und beim Verlassen stoppen
    DisposableEffect(lifecycleOwner) {
        gestureCore.startGestureDetection(lifecycleOwner) { action ->
            // Hier kannst du bei Bedarf Code ausführen, wenn eine Geste erkannt wird
        }
        onDispose {
            gestureCore.stopGestureDetection()
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
                
                // Hier wird der Live-Feed aus der Core angezeigt!
                Text(
                    text = "Sensor-Feed: $gestureState",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Hier springt die Anzeige von NONE auf SWIPE_LEFT / SWIPE_RIGHT
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


