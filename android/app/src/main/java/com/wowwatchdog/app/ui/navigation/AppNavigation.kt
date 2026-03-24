package com.wowwatchdog.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.wowwatchdog.app.data.local.SettingsDataStore
import com.wowwatchdog.app.ui.console.ConsoleScreen
import com.wowwatchdog.app.ui.dashboard.DashboardScreen
import com.wowwatchdog.app.ui.detail.ServiceDetailScreen
import com.wowwatchdog.app.ui.logs.LogScreen
import com.wowwatchdog.app.ui.settings.SettingsScreen
import com.wowwatchdog.app.ui.theme.LocalWatchdogColors
import com.wowwatchdog.app.ui.theme.WoWWatchdogTheme
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = LocalWatchdogColors.current

    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = colors.surface
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                unselectedIconColor = colors.onBackgroundSubtle,
                                unselectedTextColor = colors.onBackgroundSubtle,
                                indicatorColor = colors.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onServiceClick = { role ->
                        navController.navigate(Screen.ServiceDetail.createRoute(role))
                    }
                )
            }
            composable(Screen.Console.route) {
                ConsoleScreen()
            }
            composable(Screen.Logs.route) {
                LogScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.ServiceDetail.route,
                arguments = listOf(navArgument("role") { type = NavType.StringType })
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: "mysql"
                ServiceDetailScreen(
                    role = role,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
