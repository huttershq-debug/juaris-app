package com.juaris.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.juaris.app.GlobalMeshEngine
import com.juaris.app.JuarisDatabase
import com.juaris.app.MeshPostEntity
import kotlinx.coroutines.launch

enum class SwarmSubTab {
    RADAR, CIPHER_CHAT, GHOST_STORIES, MATRIX_FEED
}

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableStateOf(SwarmSubTab.RADAR) }

    var messageInput by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<String?>(null) }
    
    val db = remember { JuarisDatabase.getDatabase(context) }
    val meshPosts by db.meshDao().getAllPosts().collectAsState(initial = emptyList())

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri?.toString()
        if (uri != null) {
            Toast.makeText(context, "Medienanhang geladen", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Swarm & Social Hub",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = "Dezentrales P2P Mesh-Ökosystem",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab.ordinal,
            containerColor = Color(0xFF080808),
            edgePadding = 0.dp
        ) {
            SwarmSubTab.values().forEach { tab ->
                Tab(
                    selected = selectedSubTab == tab,
                    onClick = { selectedSubTab = tab },
                    text = {
                        Text(
                            text = tab.name.replace("_", " "),
                            color = if (selectedSubTab == tab) NeonGiftgruen else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedSubTab) {
            SwarmSubTab.RADAR -> {
                MeshRadarView()
            }
            SwarmSubTab.CIPHER_CHAT -> {
                CipherChatView(
                    posts = meshPosts,
                    messageInput = messageInput,
                    onMessageChange = { messageInput = it },
                    selectedMediaUri = selectedMediaUri,
                    onSelectMedia = { mediaLauncher.launch("image/*") },
                    onSend = {
                        if (messageInput.isNotBlank() || selectedMediaUri != null) {
                            GlobalMeshEngine.broadcastToSwarmWithMedia(
                                context = context,
                                content = messageInput,
                                mediaUri = selectedMediaUri,
                                mediaType = if (selectedMediaUri != null) "MEDIA" else "TEXT",
                                isEphemeral = false
                            ) { hash ->
                                Toast.makeText(context, "Broadcast gesendet! Hash: ${hash.take(8)}...", Toast.LENGTH_SHORT).show()
                                messageInput = ""
                                selectedMediaUri = null
                            }
                        }
                    }
                )
            }
            SwarmSubTab.GHOST_STORIES -> {
                GhostStoriesView(posts = meshPosts)
            }
            SwarmSubTab.MATRIX_FEED -> {
                MatrixFeedView(posts = meshPosts)
            }
        }
    }
}

@Composable
fun MeshRadarView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, NeonGiftgruen), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mesh-Radar Aktiv", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Suche nach direkten P2P-Nachbarn im lokalen Funkfeld...", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CipherChatView(
    posts: List<MeshPostEntity>,
    messageInput: String,
    onMessageChange: (String) -> Unit,
    selectedMediaUri: String?,
    onSelectMedia: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, NeonGiftgruen.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = post.senderNode, style = MaterialTheme.typography.bodySmall, color = NeonGiftgruen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = post.content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        if (post.mediaUri != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "[Medienanhang vorhanden]", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = onMessageChange,
                placeholder = { Text("Cipher-Nachricht eingeben...", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGiftgruen,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            IconButton(onClick = onSelectMedia) {
                Icon(Icons.Default.Add, contentDescription = "Medium hinzufügen", tint = NeonGiftgruen)
            }

            Button(
                onClick = onSend,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGiftgruen)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Senden", tint = Color.Black)
            }
        }
    }
}

@Composable
fun GhostStoriesView(posts: List<MeshPostEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFFF3333)), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ghost-Stories (Ephemeral Feed)", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF3333))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ephemere, selbstlöschende P2P-Momente.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
        items(posts.filter { it.isEphemeral }) { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFFF3333).copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = post.senderNode, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF3333))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun MatrixFeedView(posts: List<MeshPostEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Global Matrix Feed", style = MaterialTheme.typography.titleMedium, color = NeonGiftgruen)
            Spacer(modifier = Modifier.height(4.dp))
        }
        items(posts) { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, NeonGiftgruen.copy(alpha = 0.3f)), RoundedCornerShape(6.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = post.senderNode, fontSize = 11.sp, color = NeonGiftgruen)
                        Text(text = "Hops: ${post.ttlHopCount}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.content, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}


