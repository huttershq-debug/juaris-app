package com.juaris.app.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.GlobalMeshEngine
import com.juaris.app.NearbyMeshManager
import com.juaris.app.JuarisDatabase
import com.juaris.app.R

@Composable
fun SwarmMeshPage() {
    val context = LocalContext.current
    val db = remember { JuarisDatabase.getDatabase(context) }
    // KORREKTUR: currentTime übergeben, damit der Compiler nicht meckert
    val postsFlow = remember { db.meshDao().getAllActivePosts(currentTime = System.currentTimeMillis()) }
    val posts by postsFlow.collectAsState(initial = emptyList())

    val statusReadyText = stringResource(R.string.status_ready)
    val statusMeshActiveText = stringResource(R.string.status_mesh_active_search)

    var inputMessage by remember { mutableStateOf("") }
    var isEphemeral by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var scanStatusText by remember { mutableStateOf(statusReadyText) }
    var discoveredCount by remember { mutableStateOf(0) }

    val toastDeviceDiscovered = stringResource(R.string.toast_mesh_device_discovered)
    val statusConnectedTemplate = stringResource(R.string.status_mesh_connected_format)
    val statusConnectedDevicesTemplate = stringResource(R.string.status_mesh_connected_devices_format)
    val statusSentText = stringResource(R.string.status_sent)
    val toastP2pStartedText = stringResource(R.string.toast_p2p_started)
    val toastBluetoothMissingText = stringResource(R.string.toast_bluetooth_missing)
    val neonGreen = Color(0xFF00E676)

    val meshManager = remember {
        NearbyMeshManager(
            context = context,
            onDeviceDiscovered = { endpointId ->
                discoveredCount++
                scanStatusText = String.format(statusConnectedTemplate, endpointId, discoveredCount)
                Toast.makeText(context, toastDeviceDiscovered, Toast.LENGTH_SHORT).show()
            },
            onDeviceLost = { _ ->
                discoveredCount = (discoveredCount - 1).coerceAtLeast(0)
                scanStatusText = if (discoveredCount > 0) {
                    String.format(statusConnectedDevicesTemplate, discoveredCount)
                } else {
                    statusMeshActiveText
                }
            },
            onMessageReceived = { _, message ->
                GlobalMeshEngine.broadcastToSwarm(
                    context = context,
                    content = message,
                    isEphemeral = false,
                    meshManager = null,
                    onBlocked = {},
                    onSuccess = {}
                )
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            meshManager.stopMeshNode()
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            meshManager.startMeshNode()
            scanStatusText = statusMeshActiveText
            Toast.makeText(context, toastP2pStartedText, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, toastBluetoothMissingText, Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(stringResource(R.string.p2p_swarm_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(stringResource(R.string.p2p_swarm_subtitle), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mesh_broadcast_title), style = MaterialTheme.typography.titleMedium, color = neonGreen)
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        label = { Text(stringResource(R.string.label_message_placeholder), color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isEphemeral, onCheckedChange = { isEphemeral = it })
                            Text(stringResource(R.string.checkbox_ephemeral), color = Color.LightGray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    meshManager.broadcastMessage(inputMessage)
                                    GlobalMeshEngine.broadcastToSwarm(
                                        context = context,
                                        content = inputMessage,
                                        isEphemeral = isEphemeral,
                                        meshManager = meshManager,
                                        onBlocked = { reason -> statusMessage = reason },
                                        onSuccess = { _ ->
                                            statusMessage = statusSentText
                                            inputMessage = ""
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = neonGreen)
                        ) {
                            Text(stringResource(R.string.btn_broadcast), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(statusMessage, color = neonGreen, fontSize = 12.sp)
                    }
                }
            }
        }
        item { Text(stringResource(R.string.mesh_packets_count, posts.size), style = MaterialTheme.typography.titleMedium, color = neonGreen) }
        
        items(posts) { post ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = post.senderNodeHash, color = neonGreen, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.content, color = Color.White)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.mesh_hardware_title), style = MaterialTheme.typography.titleMedium, color = neonGreen)
                    Text(stringResource(R.string.mesh_status_format, scanStatusText), color = Color.White)
                    Button(
                        onClick = {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                arrayOf(
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                                    android.Manifest.permission.BLUETOOTH_CONNECT
                                )
                            } else {
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            }
                            bluetoothPermissionLauncher.launch(permissions)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = neonGreen)
                    ) {
                        Text(stringResource(R.string.btn_start_mesh_node), color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
