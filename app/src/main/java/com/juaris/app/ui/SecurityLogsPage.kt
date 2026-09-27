package com.juaris.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.juaris.app.SecurityLogDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityLogsPage(logDao: SecurityLogDao) {
    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Life-Hub & Sicherheits-Logs",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Hier siehst du alle blockierten Bedrohungen sowie deine wichtigen Kalender-Einträge, Geburtstage und Fristen (100% lokal).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "Keine Einträge vorhanden. Dein Alltags- und Schutz-Hub ist bereit!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    // Visuelle Unterscheidung nach Status
                    val isImportant = log.status == "IMPORTANT"
                    val isBlocked = log.status == "BLOCKED" || log.status == "QUARANTINE"

                    val borderColor = when {
                        isImportant -> Color(0xFFFFD700) // Edles Gold für Geburtstage, Hochzeitstage & Fristen
                        isBlocked -> Color(0xFFFF3333) // Rot für Blockaden
                        else -> NeonGiftgruen // Grün für normale Statusmeldungen
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Kopfzeile: Modul & Status-Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Modul: ${log.module}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonGiftgruen
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = borderColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = log.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = borderColor
                                    )
                                }
                            }

                            // Hauptbeschreibung (z.B. "🎂 Geburtstag heute!")
                            Text(
                                text = log.description,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            // Zusätzliche Details (z.B. Ereignis-Name & Notizen)
                            if (!log.details.isNullOrBlank()) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB0BEC5)
                                )
                            }

                            // Zeitstempel
                            Text(
                                text = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
