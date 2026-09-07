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
fun AirGesturePage(
    airGestureCore: AirGestureCore,
    onNavigate: (Boolean) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Live-Daten aus der Core abgreifen
    val gestureState by airGestureCore.gestureState.collectAsState()
    val lastAction by airgestureCore.lastAction.collectAsState()

    // WICHTIG: Hier wird die Kamera-Erkennung aktiv gestartet, sobald die Seite geöffnet wird!
    DisposableEffect(lifecycleOwner) {
        airGestureCore.startGestureDetection(lifecycleOwner) { action ->
            // Wenn eine Geste erkannt wird, schalten wir die Tabs weiter (forward/backward)
            when (action) {
                AirGestureCore.GestureAction.SWIPE_RIGHT -> onNavigate(true) // Nächster Tab
                AirGestureCore.GestureAction.SWIPE_LEFT -> onNavigate(false) Vorheriger Tab
                else -> {}
            }
        }
        onDispose {
            airGestureCore.stopGestureDetection()
        }
    }

    // Ab hier folgt deine UI (Card, Texte, Sensor-Feed etc.)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... deine bestehenden UI-Elemente für den Sensor-Feed und "Letzte Aktion: $lastAction" ...
    }
}

