package com.juaris.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.ContextLocal
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juaris.app.AirGestureCore
import kotlinx.coroutines.launch

val NeonGiftgruen = Color(0xFF00FF66)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuarisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JuarisDashboardScreen()
                }
            }
        }
    }
}

@Composable
fun JuarisTheme(content: @Composable () -> Unit) {
    val hackerGreenColorScheme = darkColorScheme(
        primary = NeonGiftgruen,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF003311),
        onPrimaryContainer = NeonGiftgruen,
        background = Color.Black,
        onBackground = NeonGiftgruen,
        surface = Color(0xFF080808),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF121212),
        onSurfaceVariant = NeonGiftgruen,
        error = Color(0xFFFF3333)
    )
    MaterialTheme(
        colorScheme = hackerGreenColorScheme,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JuarisDashboardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Pager für genau 10 App-Tabs (unterstützt Touch-Wischen von Haus aus nativ)
    val pagerState = rememberPagerState(pageCount = { 10 })

    // AirGestureCore initialisieren und an den Lifecycle binden
    DisposableEffect(lifecycleOwner) {
        val gestureCore = AirGestureCore(context)
        gestureCore.startGestureDetection(lifecycleOwner) { action ->
            coroutineScope.launch {
                when (action) {
                    AirGestureCore.GestureAction.SWIPE_UP -> {
                        // Rauf wischen -> Nächster Tab (Rechts) mit Modulo-Sicherung für 10 Tabs
                        val nextTab = (pagerState.currentPage + 1) % 10
                        pagerState.animateScrollToPage(nextTab)
                    }
                    AirGestureCore.GestureAction.SWIPE_DOWN -> {
                        // Runter wischen -> Vorheriger Tab (Links)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Juaris Security Command Center",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGiftgruen
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Tab-Indikator Anzeige (Zeigt an, auf welchem der 10 Tabs man sich befindet)
        Text(
            text = "Aktiver Tab: ${pagerState.currentPage + 1} / 10",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // HorizontalPager für die 10 App-Tabs (Beinhaltet Touch-Bedienung + Kamera-Gesten)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            TabContentScreen(tabIndex = page + 1)
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


