package com.wowwatchdog.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wowwatchdog.app.data.model.ServiceInfo
import com.wowwatchdog.app.ui.common.ConfirmationDialog
import com.wowwatchdog.app.ui.theme.*

@Composable
fun ServiceCard(
    name: String,
    role: String,
    info: ServiceInfo,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalWatchdogColors.current
    val statusColor = when {
        info.held -> StatusYellow
        info.running && info.healthy -> StatusGreen
        info.running -> StatusYellow
        else -> StatusRed
    }
    val statusText = when {
        info.held -> "Held"
        info.running && info.healthy -> "Running"
        info.running -> "Starting..."
        else -> "Stopped"
    }

    var showStopDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        ConfirmationDialog(
            title = "Stop $name",
            message = "Are you sure you want to stop $name?",
            onConfirm = onStop,
            onDismiss = { showStopDialog = false }
        )
    }

    if (showRestartDialog) {
        ConfirmationDialog(
            title = "Restart $name",
            message = "Are you sure you want to restart $name?",
            onConfirm = onRestart,
            onDismiss = { showRestartDialog = false }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status LED
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(Modifier.width(12.dp))

            // Name and status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onBackground
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (info.running) colors.onBackgroundSubtle else StatusRed
                )
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!info.running) {
                    IconButton(
                        onClick = onStart,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            tint = colors.buttonStart
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = colors.primary
                        )
                    }
                    IconButton(
                        onClick = { showStopDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = colors.buttonStop
                        )
                    }
                }
            }
        }
    }
}
