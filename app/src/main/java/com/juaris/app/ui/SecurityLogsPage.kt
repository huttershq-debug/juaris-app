package com.juaris.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.juaris.app.SecurityLogDao
import com.juaris.app.SecurityLogEntity

@Composable
fun SecurityLogsPage(logDao: SecurityLogDao) {
    // Holt die Logs reaktiv und in Echtzeit über den Flow aus der Room-DB
    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

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
        }
        items(logs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(text = "Modul: ${log.module}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Zeitpunkt: ${log.timestamp}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

