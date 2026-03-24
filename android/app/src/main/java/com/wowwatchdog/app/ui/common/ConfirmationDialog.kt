package com.wowwatchdog.app.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.wowwatchdog.app.ui.theme.LocalWatchdogColors

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalWatchdogColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = colors.onBackground) },
        text = { Text(message, color = colors.onBackgroundSubtle) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text("Confirm", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.onBackgroundSubtle)
            }
        },
        containerColor = colors.surface
    )
}
