package com.wowwatchdog.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowwatchdog.app.ui.theme.LocalWatchdogColors
import com.wowwatchdog.app.ui.theme.StatusGreen
import com.wowwatchdog.app.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalWatchdogColors.current
    val themes = listOf("Default", "Legion", "Wrath of the Lich King", "Cataclysm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(16.dp))

        // Server Connection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Server Connection", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)

                OutlinedTextField(
                    value = uiState.config.host,
                    onValueChange = { viewModel.updateHost(it) },
                    label = { Text("Host / IP Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("192.168.1.100") }
                )

                OutlinedTextField(
                    value = uiState.config.port.toString(),
                    onValueChange = { viewModel.updatePort(it) },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.config.apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = { viewModel.testConnection() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isTesting && uiState.config.host.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.onBackground)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (uiState.isTesting) "Testing..." else "Test Connection")
                }

                uiState.testResult?.let { result ->
                    val isSuccess = result.startsWith("Connected")
                    Text(
                        text = result,
                        color = if (isSuccess) StatusGreen else StatusRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // NTFY Notifications
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Push Notifications (NTFY)", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)

                OutlinedTextField(
                    value = uiState.config.ntfyServer,
                    onValueChange = { viewModel.updateNtfyServer(it) },
                    label = { Text("NTFY Server") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.config.ntfyTopic,
                    onValueChange = { viewModel.updateNtfyTopic(it) },
                    label = { Text("NTFY Topic") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Appearance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)

                Text("Theme", style = MaterialTheme.typography.bodyMedium, color = colors.onBackgroundSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { theme ->
                        FilterChip(
                            selected = uiState.config.theme == theme,
                            onClick = { viewModel.updateTheme(theme) },
                            label = {
                                Text(
                                    when (theme) {
                                        "Wrath of the Lich King" -> "WotLK"
                                        else -> theme
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Polling Interval: ${uiState.config.pollingIntervalSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackgroundSubtle
                )
                Slider(
                    value = uiState.config.pollingIntervalSeconds.toFloat(),
                    onValueChange = { viewModel.updatePollingInterval(it.toInt()) },
                    valueRange = 3f..60f,
                    steps = 18
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save button
        Button(
            onClick = { viewModel.save() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            if (uiState.isSaved) {
                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Saved!")
            } else {
                Text("Save Settings")
            }
        }
    }
}
