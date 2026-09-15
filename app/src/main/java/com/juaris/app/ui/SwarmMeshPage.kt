package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.GlobalMeshEngine
import com.juaris.app.NearbyMeshManager
import com.juaris.app.JuarisDatabase

@Composable
fun SwarmMeshPage(meshManager: NearbyMeshManager? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { JuarisDatabase.getDatabase(context) }
    val postsFlow = remember { db.meshDao().getAllActivePosts() }
    val posts by postsFlow.collectAsState(initial = emptyList())

    var inputMessage by remember { mutableStateOf("") }
    var isEphemeral by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var scanStatusText by remember { mutableStateOf("Bereit") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("P2P-Schwarm & Live-Feed", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Dezentraler Offline-Austausch.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nachricht broadcasten", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        label = { Text("Nachricht...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isEphemeral, onCheckedChange = { isEphemeral = it })
                            Text("Ephemer", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    meshManager?.broadcastMessage(inputMessage)
                                    GlobalMeshEngine.broadcastToSwarm(
                                        context = context,
                                        content = inputMessage,
                                        isEphemeral = isEphemeral,
                                        meshManager = meshManager,
                                        onBlocked = { reason -> statusMessage = reason },
                                        onSuccess = { _ ->
                                            statusMessage = "Gesendet!"
                                            inputMessage = ""
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                        ) {
                            Text("Broadcast", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item { Text("Schwarm-Pakete (${posts.size})", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen) }
        items(posts) { post ->
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text(text = post.senderNode, color = NeonGiftgruen, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.content, color = Color.White)
                }
            }
        }
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth Mesh Hardware", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Text("Status: $scanStatusText", color = Color.White)
                    Button(
                        onClick = {
                            meshManager?.startMeshNode()
                            scanStatusText = "Mesh-Knoten aktiv"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Text("Mesh-Scan starten", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
