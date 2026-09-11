package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.juaris.app.AirGestureCore

@Composable
fun AirGesturePage(
    airGestureCore: AirGestureCore,
    isGestureActive: Boolean,
    statusText: String, // <--- 1. Parameter für den Live-Status hinzugefügt
    onToggleGesture: (Boolean) -> Unit,
    onTabSwitch: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Air-Swiping Gesten-Steuerung", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Bediene Juaris mit vertikalen Wischgesten vor der Frontkamera (100% lokal & offline).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gesten-Steuerung aktivieren", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                            Spacer(modifier = Modifier.height(2.dp))
                            // 2. Hier wird nun der echte Live-Status aus der Pipeline angezeigt:
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusText.contains("Fehler") || statusText.contains("verweigert")) Color(0xFFFF3333) else Color.Gray
                            )
                        }
                        Switch(
                            checked = isGestureActive,
                            onCheckedChange = onToggleGesture
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hinweis für den Store-Betrieb: Standardmäßig deaktiviert, um unbeabsichtigte Bildschirmsprünge zu verhindern.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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


