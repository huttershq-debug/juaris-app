package com.juaris.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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

// Globale Definition für den DashboardScreen
val NeonGiftgruen = Color(0xFF00FF66)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Pager für genau 10 App-Tabs (unterstützt Touch-Wischen nativ)
    val pagerState = rememberPagerState(pageCount = { 10 })

    // 1. Zustand für den Schalter (Gestensteuerung standardmäßig aus)
    var gestureEnabled by remember { mutableStateOf(false) }

    // 2. Kamera-Berechtigungs-Launcher für Android Runtime Permissions
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            gestureEnabled = true
            Toast.makeText(context, "Kamera-Berechtigung erteilt! Gesten aktiv.", Toast.LENGTH_SHORT).show()
        } else {
            gestureEnabled = false
            Toast.makeText(context, "Kamera-Berechtigung für Gesten erforderlich!", Toast.LENGTH_LONG).show()
        }
    }

    // 3. AirGestureCore nur starten, WENN gestureEnabled auf true steht!
    if (gestureEnabled) {
        DisposableEffect(lifecycleOwner) {
            val gestureCore = AirGestureCore(context)
            gestureCore.startGestureDetection(lifecycleOwner) { action ->
                coroutineScope.launch {
                    when (action) {
                        AirGestureCore.GestureAction.SWIPE_UP -> {
                            val nextTab = (pagerState.currentPage + 1) % 10
                            pagerState.animateScrollToPage(nextTab)
                        }
                        AirGestureCore.GestureAction.SWIPE_DOWN -> {
                            val prevTab = if (pagerState.currentPage - 1 < 0) 9 else pagerState.currentPage - 1
                            pagerState.animateScrollToPage(prevTab)
                        }
                        AirGestureCore.GestureAction.NONE -> {}
                    }
                }
            }
            onDispose {
                gestureCore.stopGestureDetection()
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

        // HorizontalPager für die 10 Tabs (Touch + Gesten kombiniert)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            // Wenn es Tab 9 ist (Index 8), übergeben wir den Schalter & Launcher an deine Gesten-UI!
            if (page == 8) {
                // Beispielhafter Aufruf deiner Gesten-Page (passe den Funktionsnamen an deine UI an)
                AirGestureControlView(
                    isGestureActive = gestureEnabled,
                    onToggle = { activate ->
                        if (activate) {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        } else {
                            gestureEnabled = false
                            Toast.makeText(context, "Gesten-Steuerung deaktiviert.", Toast.LENGTH_SHORT).show()
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
                TacticalPulseCard {
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

// Falls du eine dedizierte Ansicht für den Gesten-Schalter in Tab 9 hast:
@Composable
fun AirGestureControlView(isGestureActive: Boolean, onToggle: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bediene Juaris mit vertikalen Wischgesten vor der Frontkamera",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
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
    }
}

