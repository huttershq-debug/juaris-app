package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juaris.app.AirGestureCore
import kotlinx.coroutines.delay

@Composable
fun AirGesturePage(airGestureCore: AirGestureCore, onTabSwitch: (Boolean) -> Unit) {
    var lastDetectedAction by remember { mutableStateOf("Warte auf Geste...") }
    var gestureCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(200L)
            when (airGestureCore.currentActionState) {
                AirGestureCore.GestureAction.SWIPE_UP -> {
                    lastDetectedAction = "SWIPE_UP (Nach oben gewischt)"
                    gestureCount++
                    onTabSwitch(false)
                }
                AirGestureCore.GestureAction.SWIPE_DOWN -> {
                    lastDetectedAction = "SWIPE_DOWN (Nach unten gewischt)"
                    gestureCount++
                    onTabSwitch(true)
                }
                AirGestureCore.GestureAction.NONE -> {
                    // Warten auf Eingabe
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Air-Swiping Gesten-Steuerung", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Bediene Juaris gemütlich mit vertikalen Wischgesten vor der Frontkamera (100% lokal & offline).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = MaterialTheme.shapes.small,
                            color = NeonGiftgruen
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vertikale Gesten aktiv", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sensor-Feed: Kamera aktiv - Bereit für Gesten", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Erkannte Gesten: $gestureCount", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Letzte Aktion: $lastDetectedAction", style = MaterialTheme.typography.bodyMedium, color = NeonGiftgruen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hinweis: Wische ganz entspannt nach oben oder unten vor der Frontkamera, um die Tabs kontrolliert zu wechseln.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

