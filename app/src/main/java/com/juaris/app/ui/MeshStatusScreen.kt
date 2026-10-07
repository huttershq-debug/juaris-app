package com.juaris.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.MeshPostEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshStatusScreen(
    isMeshActive: Boolean,
    connectedNodesCount: Int,
    discoveredNodes: List<String>,
    messages: List<MeshPostEntity>,
    onToggleMesh: (Boolean) -> Unit,
    onBroadcastEmergency: (String) -> Unit
) {
    var emergencyReasonInput by remember { mutableStateOf("") }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Juaris P2P-Mesh-Schwarm", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBroadcastDialog = true },
                containerColor = Color(0xFF900C3F),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Notfall Senden")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sektion 1: Statuskarte & Steuerung
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMeshActive) Color(0xFF1B4D3E) else Color(0xFF4A4A4A)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isMeshActive) Color.Green else Color.Red)
                            )
                            Text(
                                text = if (isMeshActive) "Mesh-Knoten Aktiv" else "Mesh-Knoten Inaktiv",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Switch(
                            checked = isMeshActive,
                            onCheckedChange = { onToggleMesh(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White)
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.2f))

                    Text(
                        text = "Verbundene Knoten im Schwarm: $connectedNodesCount",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }

            // Sektion 2: Live-Entdeckte Nodes
            Text(
                text = "Aktive Nachbar-Knoten (${discoveredNodes.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (discoveredNodes.isEmpty()) {
                Text(
                    text = "Suche nach anderen Geräten in der Umgebung...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredNodes) { nodeId ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = nodeId, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sektion 3: Mesh-Nachrichten & Krypto-Archiv
            Text(
                text = "Dezentrales Nachrichten-Archiv",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Nachrichten im Mesh-Speicher vorhanden.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = post.senderNodeHash,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val formattedDate = remember(post.timestamp) {
                                        SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault()).format(Date(post.timestamp))
                                    }
                                    Text(
                                        text = formattedDate,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = post.content,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Notfall-Broadcast Dialog
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("🚨 Notfall über Mesh senden") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Geben Sie einen Notfall-Grund ein, der an alle verbundenen Nachbarn im Mesh-Schwarm übermittelt wird:")
                    OutlinedTextField(
                        value = emergencyReasonInput,
                        onValueChange = { emergencyReasonInput = it },
                        placeholder = { Text("z.B. Hilfe benötigt / Bedrohung") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emergencyReasonInput.isNotBlank()) {
                            onBroadcastEmergency(emergencyReasonInput)
                            emergencyReasonInput = ""
                            showBroadcastDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF900C3F))
                ) {
                    Text("Sofort Senden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

