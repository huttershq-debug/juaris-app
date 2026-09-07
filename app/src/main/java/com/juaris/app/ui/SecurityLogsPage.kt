package com.juaris.app.ui // Passe das Package an deinen ui-Ordner an

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.runtime.*
import androidx.ui.unit.dp // Falls Compose 1.5+, nutze androidx.compose.ui.unit.dp
import com.juaris.app.SecurityLogDao
import com.juaris.app.SecurityLogEntity
import java.util.Date

@Composable
fun SecurityLogsPage(logDao: SecurityLogDao) {
    // Holt alle Logs in Echtzeit asynchron aus der Room-Database
    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sicherheits-Logs & Threat-Tracking", style = MaterialTheme.typography.titleLarge)
        Text("Alle abgefangenen Vektoren in Echtzeit (Offline-Vault).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Keine Bedrohungen verzeichnet. System sicher.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "⚠️ ${log.threatType}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = log.details, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val formattedTime = android.text.format.DateFormat.format("dd.MM.yyyy HH:mm:ss", Date(log.timestamp)).toString()
                            Text(text = formattedTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
