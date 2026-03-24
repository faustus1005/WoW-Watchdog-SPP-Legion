package com.wowwatchdog.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowwatchdog.app.ui.common.ConfirmationDialog
import com.wowwatchdog.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    role: String,
    onBack: () -> Unit,
    viewModel: ServiceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalWatchdogColors.current

    LaunchedEffect(role) { viewModel.loadService(role) }

    var showStopDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    if (showStopDialog) {
        ConfirmationDialog(
            title = "Stop ${uiState.displayName}",
            message = "Are you sure you want to stop ${uiState.displayName}?",
            onConfirm = { viewModel.stop() },
            onDismiss = { showStopDialog = false }
        )
    }
    if (showRestartDialog) {
        ConfirmationDialog(
            title = "Restart ${uiState.displayName}",
            message = "Are you sure you want to restart ${uiState.displayName}?",
            onConfirm = { viewModel.restart() },
            onDismiss = { showRestartDialog = false }
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text(uiState.displayName, color = colors.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status indicator
            val statusColor = when {
                uiState.info.held -> StatusYellow
                uiState.info.running && uiState.info.healthy -> StatusGreen
                uiState.info.running -> StatusYellow
                else -> StatusRed
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {
                    uiState.info.held -> "Held (auto-restart paused)"
                    uiState.info.running && uiState.info.healthy -> "Running"
                    uiState.info.running -> "Starting..."
                    else -> "Stopped"
                },
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            Spacer(Modifier.height(24.dp))

            // Action buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Actions", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.start() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.info.running,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.buttonStart)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start")
                        }
                        Button(
                            onClick = { showStopDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.info.running,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.buttonStop)
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }

                    Button(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.info.running,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Restart")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Hold toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hold (Pause Auto-Restart)", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                        Text(
                            "Prevents watchdog from restarting this service",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onBackgroundSubtle
                        )
                    }
                    Switch(
                        checked = uiState.info.held,
                        onCheckedChange = { viewModel.toggleHold() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.warning,
                            checkedTrackColor = colors.warning.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Error
            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(text = error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
