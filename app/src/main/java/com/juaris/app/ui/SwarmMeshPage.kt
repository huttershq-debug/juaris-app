package com.juaris.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.juaris.app.JuarisDatabase
import com.juaris.app.GlobalMeshEngine

// Rechtlich saubere, eigenständige Juaris-Bezeichnungen
enum class SwarmSubTab {
    RADAR, CIPHER_CHAT, GHOST_STORIES, MATRIX_FEED
}

@Composable
fun SwarmMeshPage() {
    var activeTab by remember { mutableStateOf(SwarmSubTab.RADAR) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- HAUPTÜBERSCHRIFT ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JUARIS SWARM & SOCIAL HUB",
                color = NeonGiftgruen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "0% Cloud / P2P",
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // --- DIE 4 UNTERPUNKTE (RECHTLICH SICHER) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SubTabButton("Radar", activeTab == SwarmSubTab.RADAR) { activeTab = SwarmSubTab.RADAR }
            SubTabButton("Cipher-Chat", activeTab == SwarmSubTab.CIPHER_CHAT) { activeTab = SwarmSubTab.CIPHER_CHAT }
            SubTabButton("Ghost-Stories", activeTab == SwarmSubTab.GHOST_STORIES) { activeTab = SwarmSubTab.GHOST_STORIES }
            SubTabButton("Matrix-Feed", activeTab == SwarmSubTab.MATRIX_FEED) { activeTab = SwarmSubTab.MATRIX_FEED }
        }

        Divider(color = Color(0xFF1E2923), thickness = 1.dp)

        // --- INHALT JE NACH AUSGEWÄHLTEM BEREICH ---
        when (activeTab) {
            SwarmSubTab.RADAR -> SwarmRadarView()
            SwarmSubTab.CIPHER_CHAT -> CipherChatView()
            SwarmSubTab.GHOST_STORIES -> GhostStoriesView()
            SwarmSubTab.MATRIX_FEED -> MatrixFeedView()
        }
    }
}

// Hilfs-Button für die Unterpunkte
@Composable
fun RowScope.SubTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(38.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) NeonGiftgruen else Color(0xFF0F1412)
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, NeonGiftgruen.copy(alpha = if (isSelected) 1f else 0.3f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else NeonGiftgruen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- BEREICH 1: MESH RADAR ---
@Composable
fun SwarmRadarView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        TacticalPulseCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Aktive P2P-Tunnel (Global)", color = Color.White, fontWeight = FontWeight.Bold, fontSize: 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Direkte Verbindung zu Nodes weltweit aktiv. Keine Server, keine Cloud.", color = Color.Gray, fontSize: 11.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(listOf("Node_Alpha (Wien - Secure)", "CyberPeer_07 (Global P2P)", "MatrixNode_X (Direct Socket)")) { node ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121C16)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonGiftgruen.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = node, color = Color.White, fontSize: 12.sp)
                        Text(text = "Online", color = NeonGiftgruen, fontSize: 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- BEREICH 2: CIPHER-CHAT (Direkter P2P Chat) ---
@Composable
fun CipherChatView() {
    var messageText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(
        "Node_Alpha: P2P-Kanal verschlüsselt (AES-256).",
        "Du: Bereit für den globalen Release!"
    ) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                TacticalPulseCard {
                    Text(text = msg, color = Color.White, modifier = Modifier.padding(12.dp), fontSize: 12.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Verschlüsselte Nachricht...", color = Color.Gray, fontSize: 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGiftgruen,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        messages.add("Du: $messageText")
                        messageText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Senden", color = Color.Black, fontWeight = FontWeight.Bold, fontSize: 11.sp)
            }
        }
    }
}

// --- BEREICH 3: GHOST-STORIES (Ephemerer Medien-Capture) ---
@Composable
fun GhostStoriesView() {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("TEXT") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            selectedMediaType = if (it.toString().contains("video", ignoreCase = true)) "VIDEO" else "IMAGE"
            Toast.makeText(context, "Ghost-Payload geladen!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(text = "Ephemere P2P-Stories (Löschung nach Ansicht)", color = Color.Gray, fontSize: 11.sp)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(4) { index ->
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFF0F1412), RoundedCornerShape(35.dp))
                        .border(2.dp, NeonGiftgruen, RoundedCornerShape(35.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Ghost $index", color = Color.White, fontSize: 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        TacticalPulseCard {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "LOKALER GHOST CAPTURE", color = Color.White, fontWeight = FontWeight.Bold, fontSize: 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Medien aufnehmen und direkt verschlüsselt an den Schwarm übertragen.", color = Color.Gray, fontSize: 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                
                OutlinedButton(
                    onClick = { launcher.launch("*/*") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGiftgruen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedMediaUri == null) "Foto/Video auswählen" else "Mediendatei angehängt")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (selectedMediaUri != null) {
                            GlobalMeshEngine.broadcastToSwarmWithMedia(
                                context = context,
                                content = "[Ghost-Media-Post]",
                                mediaUri = selectedMediaUri?.toString(),
                                mediaType = selectedMediaType,
                                isEphemeral = true
                            ) { hash ->
                                Toast.makeText(context, "Ghost gesendet! Hash: ${hash.take(8)}", Toast.LENGTH_SHORT).show()
                                selectedMediaUri = null
                            }
                        } else {
                            Toast.makeText(context, "Bitte zuerst Medien auswählen!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "ALS GHOST SENDEN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize: 12.sp)
                }
            }
        }
    }
}

// --- BEREICH 4: MATRIX-FEED (Datenbank-gestützter Live Feed) ---
@Composable
fun MatrixFeedView() {
    val context = LocalContext.current
    val db = remember { JuarisDatabase.getDatabase(context) }
    val meshPosts by db.meshDao().getAllMeshPosts().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(text = "Dezentraler Matrix-Stream (${meshPosts.size} Einträge)", color = Color.Gray, fontSize: 11.sp)
        }

        items(meshPosts) { post ->
            TacticalPulseCard {
                Column(modifier = Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(post.senderNode, style = MaterialTheme.typography.bodySmall, color = NeonGiftgruen, fontWeight = FontWeight.Bold)
                        if (post.isEphemeral) {
                            Text("⚡ Ghost (Ephemer)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF3333))
                        } else {
                            Text("🌐 ${post.mediaType}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(post.content, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                   
                    if (!post.mediaUri.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color.Black, RoundedCornerShape(12.dp))
                                .border(1.dp, NeonGiftgruen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "▶ Lokales Medium Abspielen", color = NeonGiftgruen, fontSize: 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hash-ID: ${post.postId.take(16)}... | Hops: ${post.ttlHopCount}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                }
            }
        }
    }
}

