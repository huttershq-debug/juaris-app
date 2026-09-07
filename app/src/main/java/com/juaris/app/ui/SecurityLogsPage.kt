package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juaris.app.SecurityLogDao
import com.juaris.app.SecurityLogEntity

@Composable
fun SecurityLogsPage(logDao: SecurityLogDao) {
    // Falls Flow genutzt wird, hier sicher einbinden:
    // val logs by logDao.getAllLogs().collectAsState(initial = emptyList())
    
    // Fallback-Liste falls DAO direkt abgefragt wird oder als Platzhalter:
    var logs by remember { mutableStateOf(listOf<SecurityLogEntity>()) }
    
    LaunchedEffect(Unit) {
        try {
            logs = logDao.getAllLogsList() // Passe dies an deine DAO-Methode an (z.B. getAllLogs())
        } catch (e: Exception) {
            // Fallback bei leerer DB
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Datenbank-Sicherheitslogs",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Persistente Room-Datenbank Protokolle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Keine Logs in der Datenbank gespeichert.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Modul: ${log.module}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = log.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Status: ${log.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (log.status == "SAFE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

