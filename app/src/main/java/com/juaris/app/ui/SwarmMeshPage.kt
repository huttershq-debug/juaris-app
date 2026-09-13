package com.juaris.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.juaris.app.JuarisDatabase
import com.juaris.app.GlobalMeshEngine

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    val db = remember { JuarisDatabase.getDatabase(context) }
    
    val meshPosts by db.meshDao().getAllMeshPosts().collectAsState(initial = emptyList())

    var postInputText by remember { mutableStateOf("") }
    var isEphemeralMode by remember { mutableStateOf(false) } // Snapchat Modus Schalter

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Globaler P2P-Schwarm (Mesh)", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("100% Offline • Keine Cloud • Weltweites Routing", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        // Sende-Box (WhatsApp/Instagram/Snapchat Hybrid)
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neuen Schwarm-Broadcast senden", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    
                    OutlinedTextField(
                        value = postInputText,
                        onValueChange = { postInputText = it },
                        label = { Text("Nachricht oder Payload", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGiftgruen,
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isEphemeralMode,
                                onCheckedChange = { isEphemeralMode = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonGiftgruen)
                            )
                            Text("Snapchat-Modus (Selbstzerstörung)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            if (postInputText.isNotBlank()) {
                                GlobalMeshEngine.broadcastToSwarm(context, postInputText, isEphemeralMode) { hash ->
                                    Toast.makeText(context, "In Schwarm eingespeist! Hash: ${hash.take(8)}...", Toast.LENGTH_SHORT).show()
                                    postInputText = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("An weltweiten Schwarm senden", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Aktiver Schwarm-Feed (${meshPosts.size})", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
        }

        // Live Feed der Beiträge im Mesh
        items(meshPosts) { post ->
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(post.senderNode, style = MaterialTheme.typography.bodySmall, color = NeonGiftgruen, fontWeight = FontWeight.Bold)
                        if (post.isEphemeral) {
                            Text("⚡ Ephemer (Snapchat)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF3333))
                        } else {
                            Text("🌐 Global Mesh", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(post.content, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hash-ID: ${post.postId.take(16)}... | Hops übrig: ${post.ttlHopCount}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Hutter's IT-Solutions • Release Candidate", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

