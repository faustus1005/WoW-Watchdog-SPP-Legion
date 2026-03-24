package com.wowwatchdog.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowwatchdog.app.ui.common.ConfirmationDialog
import com.wowwatchdog.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onServiceClick: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalWatchdogColors.current

    var showStopAllDialog by remember { mutableStateOf(false) }

    if (showStopAllDialog) {
        ConfirmationDialog(
            title = "Stop All Services",
            message = "Are you sure you want to stop all services?",
            onConfirm = { viewModel.stopAll() },
            onDismiss = { showStopAllDialog = false }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.status.serverName.ifEmpty { "WoW Watchdog" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (uiState.isConnected) StatusGreen else StatusRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isConnected) {
                                "Watchdog: ${uiState.status.watchdog.state}"
                            } else {
                                "Disconnected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onBackgroundSubtle
                        )
                    }
                }

                if (uiState.status.expansion != "Unknown") {
                    Text(
                        text = uiState.status.expansion,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.highlight
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Error banner
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.startAll() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.buttonStart)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start All")
                }
                Button(
                    onClick = { showStopAllDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.buttonStop)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop All")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Service cards
            Text(
                text = "Services",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )
            Spacer(Modifier.height(8.dp))

            ServiceCard(
                name = "MySQL",
                role = "mysql",
                info = uiState.status.services.mysql,
                onStart = { viewModel.startService("mysql") },
                onStop = { viewModel.stopService("mysql") },
                onRestart = { viewModel.restartService("mysql") },
                onClick = { onServiceClick("mysql") }
            )
            Spacer(Modifier.height(8.dp))

            ServiceCard(
                name = "Authserver",
                role = "auth",
                info = uiState.status.services.authserver,
                onStart = { viewModel.startService("auth") },
                onStop = { viewModel.stopService("auth") },
                onRestart = { viewModel.restartService("auth") },
                onClick = { onServiceClick("auth") }
            )
            Spacer(Modifier.height(8.dp))

            ServiceCard(
                name = "Worldserver",
                role = "world",
                info = uiState.status.services.worldserver,
                onStart = { viewModel.startService("world") },
                onStop = { viewModel.stopService("world") },
                onRestart = { viewModel.restartService("world") },
                onClick = { onServiceClick("world") }
            )

            // World restart count
            if (uiState.status.worldRestartCount > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Worldserver restarts: ${uiState.status.worldRestartCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.warning,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
