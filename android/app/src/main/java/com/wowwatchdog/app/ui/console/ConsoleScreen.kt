package com.wowwatchdog.app.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowwatchdog.app.ui.theme.LocalWatchdogColors
import com.wowwatchdog.app.ui.theme.StatusGreen
import com.wowwatchdog.app.ui.theme.StatusRed

@Composable
fun ConsoleScreen(
    viewModel: ConsoleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalWatchdogColors.current
    var commandText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(uiState.outputLines.size) {
        if (uiState.outputLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.outputLines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text("Worldserver Console", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(12.dp))

        if (!uiState.isConnected) {
            // Connection form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RA Console Connection", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.host,
                            onValueChange = { viewModel.updateHost(it) },
                            label = { Text("Host") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.port,
                            onValueChange = { viewModel.updatePort(it) },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isConnecting,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.onBackground)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (uiState.isConnecting) "Connecting..." else "Connect")
                    }

                    uiState.error?.let { error ->
                        Text(error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            // Connected: output + command input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Circle, null, tint = StatusGreen, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Connected", style = MaterialTheme.typography.bodySmall, color = StatusGreen)
                }
                TextButton(onClick = { viewModel.disconnect() }) {
                    Text("Disconnect", color = StatusRed)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Output area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                items(uiState.outputLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.info,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Command input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter command...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendCommand(commandText)
                        commandText = ""
                    })
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendCommand(commandText)
                        commandText = ""
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = colors.primary)
                }
            }
        }
    }
}
