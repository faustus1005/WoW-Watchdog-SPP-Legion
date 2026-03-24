package com.wowwatchdog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wowwatchdog.app.ui.navigation.AppNavigation
import com.wowwatchdog.app.ui.theme.WoWWatchdogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WoWWatchdogTheme {
                AppNavigation()
            }
        }
    }
}
