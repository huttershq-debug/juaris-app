package com.juaris.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ContextCompat
import androidx.compose.ui.unit.dp
import com.juaris.app.GlobalMeshEngine
import com.juaris.app.JuarisDatabase
import com.juaris.app.MeshPostEntity
import kotlinx.coroutines.launch

@Composable
fun SwarmMeshPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Datenbank & DAO laden
    val db = remember { JuarisDatabase.getDatabase(context) }
    val postsFlow = remember { db.meshDao().getAllPosts() }
    val posts by postsFlow.collectAsState(initial = emptyList())

    var inputMessage by remember { mutableStateOf("") }
    var isEphemeral by remember { mutableStateOf(false) }
    var statusMessage by remember { val state = mutableStateOf(""); state }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F0C)) // Juaris Dark Matrix Style
            .padding(16.dp)
    ) {
        Text(
            text = "P2P-Schwarm & Live-Feed",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF00FF66)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // --- INPUT BEREICH (Instagram / Snapchat Style) ---
        OutlinedTextField(
            value = inputMessage,
            onValueChange = { inputMessage = it },
            label = { Text("Nachricht in den Schwarm broadcasten...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00FF66),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Snapchat-Modus (Ephemer / Selbstzerstörung)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = isEphemeral,
                    onCheckedChange = { isEphemeral = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00FF66))
                )
                Text("Snapchat-Modus (Ephemer)", color = Color.LightGray)
            }

            // Sende-Button mit automatischer AGI-Prüfung
            Button(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        GlobalMeshEngine.broadcastToSwarm(
                            context = context,
                            content = inputMessage,
                            isEphemeral = isEphemeral,
                            onBlocked = { reason ->
                                statusMessage = reason
                            },
                            onSuccess = { packetId ->
                                statusMessage = "Gesendet! ID: ${packetId.take(8)}..."
                                inputMessage = ""
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66))
            ) {
                Text("Broadcast", color = Color.Black)
            }
        }

        if (statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = statusMessage, color = Color(0xFFFF3333))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))

        // --- LIVE FEED DER SCHWARM-BEITRÄGE (WhatsApp / Instagram Feed Style) ---
        Text(
            text = "Eingehende Schwarm-Pakete (${posts.size})",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF00FF66)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121A14))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = post.senderNode, color = Color(0xFF00FF66), style = MaterialTheme.typography.bodySmall)
                            if (post.isEphemeral) {
                                Text(text = "🔥 Ephemer", color = Color(0xFFFF9900), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = post.content, color = Color.White)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Hops: ${post.ttlHopCount} | ID: ${post.postId.take(8)}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

