package com.juaris.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("TEXT") }
    var isEphemeralMode by remember { mutableStateOf(false) } // Snapchat Modus

    // Launcher für die lokale Medien-Auswahl (Fotos & Videos)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            selectedMediaType = if (it.toString().contains("video", ignoreCase = true)) "VIDEO" else "IMAGE"
            Toast.makeText(context, "Medien-Payload erfolgreich geladen!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("P2P-Schwarm & Mesh (Media)", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Instagram Feed • Snapchat Ephemer • 100% Lokal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        // Sende-Box für Text, Fotos und Videos
        item {
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Neuen Schwarm-Beitrag erstellen", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    
                    OutlinedTextField(
                        value = postInputText,
                        onValueChange = { postInputText = it },
                        label = { Text("Untertitel oder Nachricht", color = Color.Gray) },
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
                        OutlinedButton(
                            onClick = { launcher.launch("*/*") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGiftgruen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedMediaUri == null) "Foto/Video" else "Medien angehängt")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isEphemeralMode,
                                onCheckedChange = { isEphemeralMode = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeonGiftgruen)
                            )
                            Text("Snapchat-Modus", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            if (postInputText.isNotBlank() || selectedMediaUri != null) {
                                val textPayload = postInputText.ifBlank { "[Medien-Post]" }
                                GlobalMeshEngine.broadcastToSwarmWithMedia(
                                    context = context,
                                    content = textPayload,
                                    mediaUri = selectedMediaUri?.toString(),
                                    mediaType = selectedMediaType,
                                    isEphemeral = isEphemeralMode
                                ) { hash ->
                                    Toast.makeText(context, "Gesendet! Hash: ${hash.take(8)}...", Toast.LENGTH_SHORT).show()
                                    postInputText = ""
                                    selectedMediaUri = null
                                    selectedMediaType = "TEXT"
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

        // Live Feed Anzeige
        items(meshPosts) { post ->
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(post.senderNode, style = MaterialTheme.typography.bodySmall, color = NeonGiftgruen, fontWeight = FontWeight.Bold)
                        if (post.isEphemeral) {
                            Text("⚡ Ephemer (Snapchat)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF3333))
                        } else {
                            Text("🌐 ${post.mediaType}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(post.content, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    
                    if (!post.mediaUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📎 Mediendatei lokal verknüpft", style = MaterialTheme.typography.bodySmall, color = NeonGiftgruen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hash-ID: ${post.postId.take(16)}... | Hops: ${post.ttlHopCount}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
            }
        }
    }
}

