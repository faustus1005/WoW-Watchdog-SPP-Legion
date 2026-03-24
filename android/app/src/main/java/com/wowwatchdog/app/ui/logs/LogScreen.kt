package com.wowwatchdog.app.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowwatchdog.app.ui.theme.LocalWatchdogColors
import com.wowwatchdog.app.ui.theme.StatusRed

@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalWatchdogColors.current
    val listState = rememberLazyListState()

    // Auto-scroll when new lines and auto-refresh is on
    LaunchedEffect(uiState.lines.size) {
        if (uiState.autoRefresh && uiState.lines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.lines.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Watchdog Logs",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Auto", style = MaterialTheme.typography.bodySmall, color = colors.onBackgroundSubtle)
                Switch(
                    checked = uiState.autoRefresh,
                    onCheckedChange = { viewModel.toggleAutoRefresh() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, "Refresh", tint = colors.primary)
            }
        }

        Spacer(Modifier.height(8.dp))

        uiState.error?.let { error ->
            Text(error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        // Log output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            items(uiState.lines) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.info,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
