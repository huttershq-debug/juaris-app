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

@Composable
fun AirGesturePage(airGestureCore: AirGestureCore, onTabSwitch: (Boolean) -> Unit) {
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
