package com.juaris.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juaris.app.R
import com.juaris.app.SecurityLogDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityLogsPage(logDao: SecurityLogDao) {
    val logs by logDao.getAllLogs().collectAsState(initial = emptyList())
    val dateFormatter = remember {
        SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.logs_page_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.logs_page_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (logs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = stringResource(R.string.logs_empty_message),
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
                    val isImportant = log.status.equals("IMPORTANT", ignoreCase = true)
                    val isBlocked = log.status.equals("BLOCKED", ignoreCase = true) ||
                                    log.status.equals("QUARANTINE", ignoreCase = true) ||
                                    log.status.equals("WARNING", ignoreCase = true)

                    val borderColor = when {
                        isImportant -> Color(0xFFFFD700)
                        isBlocked -> Color(0xFFFF3333)
                        else -> Color(0xFF00E676)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.logs_module_prefix, log.module),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF00E676)
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

                            Text(
                                text = log.description,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            if (!log.details.isNullOrBlank()) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB0BEC5)
                                )
                            }

                            Text(
                                text = dateFormatter.format(Date(log.timestamp)),
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

