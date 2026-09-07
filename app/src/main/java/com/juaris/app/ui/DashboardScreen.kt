package com.juaris.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun JuarisDashboardScreen() {
    val auditLogs = listOf("System secured", "No threats detected")

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
        Spacer(modifier = Modifier.height(16.dp))
       
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
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

@Composable
fun ControlToggleItem(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    TacticalPulseCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(description, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonGiftgruen,
                    checkedTrackColor = Color(0xFF003311)
                )
            )
        }
    }
}

