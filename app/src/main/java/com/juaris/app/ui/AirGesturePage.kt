package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juaris.app.AirGestureCore
import com.juaris.app.R // Wichtig für den Zugriff auf deine strings.xml-Keys!

@Composable
fun AirGesturePage(
    airGestureCore: AirGestureCore,
    isGestureActive: Boolean,
    statusText: String,
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
            // KORREKTUR 1: Überschriften an deine 12 Weltsprachen gekoppelt!
            Text(stringResource(R.string.air_gesture_page_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(stringResource(R.string.air_gesture_page_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                            Text(stringResource(R.string.air_gesture_toggle_label), style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // Zeigt den echten Live-Status an (bereits dynamisch lokalisiert in der MainActivity)
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusText.contains("Fehler") || statusText.contains("verweigert") || statusText.contains("Error") || statusText.contains("denied")) Color(0xFFFF3333) else Color.Gray
                            )
                        }
                        Switch(
                            checked = isGestureActive,
                            onCheckedChange = onToggleGesture
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.air_gesture_store_notice), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.company_footer), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

