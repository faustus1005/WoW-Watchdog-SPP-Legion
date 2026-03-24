package com.wowwatchdog.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Console : Screen("console", "Console", Icons.Default.Terminal)
    data object Logs : Screen("logs", "Logs", Icons.Default.Article)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object ServiceDetail : Screen("service_detail/{role}", "Service", Icons.Default.Info) {
        fun createRoute(role: String) = "service_detail/$role"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Console, Logs, Settings)
    }
}
