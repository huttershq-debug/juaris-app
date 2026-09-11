package com.juaris.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.AirGestureCore
import kotlinx.coroutines.launch

val NeonGiftgruen = Color(0xFF00FF66)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { 10 })
    var gestureEnabled by remember { mutableStateOf(false) }
   
    // Visueller Status direkt auf dem Handy-Bildschirm
    var debugStatusText by remember { mutableStateOf("Warte auf Aktivierung...") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            gestureEnabled = true
            debugStatusText = "Berechtigung erteilt, initialisiere..."
            Toast.makeText(context, "Kamera-Berechtigung erteilt!", Toast.LENGTH_SHORT).show()
        } else {
            gestureEnabled = false
            debugStatusText = "Kamera-Berechtigung verweigert!"
            Toast.makeText(context, "Kamera-Berechtigung erforderlich!", Toast.LENGTH_LONG).show()
        }
    }

    if (gestureEnabled) {
        DisposableEffect(lifecycleOwner) {
            val gestureCore = AirGestureCore(context)
            debugStatusText = "Starte AirGestureCore..."
           
            gestureCore.startGestureDetection(
                lifecycleOwner = lifecycleOwner,
                onGestureDetected = { action ->
                    // Schutz vor Überspringen: Nur triggern, wenn Pager gerade NICHT scrollt
                    if (action == AirGestureCore.GestureAction.TRIGGERED) {
                        if (!pagerState.isScrollInProgress) {
                            coroutineScope.launch {
                                val nextTab = (pagerState.currentPage + 1) % 10
                                pagerState.animateScrollToPage(nextTab)
                            }
                        }
                    }
                },
                onDebugInfo = { info ->
                    debugStatusText = info
                }
            )
            onDispose {
                gestureCore.stopGestureDetection()
                debugStatusText = "Gestenerkennung gestoppt."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Juaris Security Command Center",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGiftgruen
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Aktiver Tab: ${pagerState.currentPage + 1} / 10",
            fontSize = 14.sp,
            color = Color.Gray
        )
       
        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 8) {
                AirGestureControlView(
                    isGestureActive = gestureEnabled,
                    debugText = debugStatusText,
                    onToggle = { activate ->
                        if (activate) {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            gestureEnabled = false
                            debugStatusText = "Manuell deaktiviert."
                        }
                    }
                )
            } else {
                TabContentScreen(tabIndex = page + 1)
            }
        }
    }
}

@Composable
fun TabContentScreen(tabIndex: Int) {
    val auditLogs = listOf(
        "System secured for Tab $tabIndex",
        "No threats detected in sector $tabIndex",
        "Biometric/Gesture bridge active"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .padding(8.dp)
    ) {
        Text(
            text = "Sicherheits-Tab 0$tabIndex",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGiftgruen
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = log,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AirGestureControlView(isGestureActive: Boolean, debugText: String, onToggle: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wischgesten vor der Frontkamera",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
       
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "Kamera-Scanner: ", color = Color.Gray)
            Switch(
                checked = isGestureActive,
                onCheckedChange = { onToggle(it) }
            )
        }
       
        Spacer(modifier = Modifier.height(24.dp))
       
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LIVE-KAMERA STATUS:",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = debugText,
                        color = NeonGiftgruen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

