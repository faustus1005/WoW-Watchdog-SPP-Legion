package com.wowwatchdog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class WatchdogColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onBackground: Color,
    val onBackgroundSubtle: Color,
    val onBackgroundDisabled: Color,
    val warning: Color,
    val info: Color,
    val highlight: Color,
    val buttonStart: Color,
    val buttonStop: Color,
    val buttonSecondary: Color,
    val border: Color,
    val borderInput: Color
)

fun watchdogColorScheme(themeName: String): WatchdogColorScheme = when (themeName) {
    "Legion" -> WatchdogColorScheme(
        background = LegionColors.background,
        surface = LegionColors.panelGradientTop,
        surfaceVariant = LegionColors.inputBackground,
        primary = LegionColors.buttonPrimary,
        onBackground = LegionColors.textHeading,
        onBackgroundSubtle = LegionColors.textSubtle,
        onBackgroundDisabled = LegionColors.textDisabled,
        warning = LegionColors.textWarning,
        info = LegionColors.textInfo,
        highlight = LegionColors.textHighlight,
        buttonStart = LegionColors.buttonStart,
        buttonStop = LegionColors.buttonStop,
        buttonSecondary = LegionColors.buttonSecondary,
        border = LegionColors.borderStrong,
        borderInput = LegionColors.borderInput
    )
    "Wrath of the Lich King" -> WatchdogColorScheme(
        background = WotlkColors.background,
        surface = WotlkColors.panelGradientTop,
        surfaceVariant = WotlkColors.inputBackground,
        primary = WotlkColors.buttonPrimary,
        onBackground = WotlkColors.textHeading,
        onBackgroundSubtle = WotlkColors.textSubtle,
        onBackgroundDisabled = WotlkColors.textDisabled,
        warning = WotlkColors.textWarning,
        info = WotlkColors.textInfo,
        highlight = WotlkColors.textHighlight,
        buttonStart = WotlkColors.buttonStart,
        buttonStop = WotlkColors.buttonStop,
        buttonSecondary = WotlkColors.buttonSecondary,
        border = WotlkColors.borderStrong,
        borderInput = WotlkColors.borderInput
    )
    "Cataclysm" -> WatchdogColorScheme(
        background = CataclysmColors.background,
        surface = CataclysmColors.panelGradientTop,
        surfaceVariant = CataclysmColors.inputBackground,
        primary = CataclysmColors.buttonPrimary,
        onBackground = CataclysmColors.textHeading,
        onBackgroundSubtle = CataclysmColors.textSubtle,
        onBackgroundDisabled = CataclysmColors.textDisabled,
        warning = CataclysmColors.textWarning,
        info = CataclysmColors.textInfo,
        highlight = CataclysmColors.textHighlight,
        buttonStart = CataclysmColors.buttonStart,
        buttonStop = CataclysmColors.buttonStop,
        buttonSecondary = CataclysmColors.buttonSecondary,
        border = CataclysmColors.borderStrong,
        borderInput = CataclysmColors.borderInput
    )
    else -> WatchdogColorScheme(
        background = DefaultColors.background,
        surface = DefaultColors.panelGradientTop,
        surfaceVariant = DefaultColors.inputBackground,
        primary = DefaultColors.buttonPrimary,
        onBackground = DefaultColors.textHeading,
        onBackgroundSubtle = DefaultColors.textSubtle,
        onBackgroundDisabled = DefaultColors.textDisabled,
        warning = DefaultColors.textWarning,
        info = DefaultColors.textInfo,
        highlight = DefaultColors.textHighlight,
        buttonStart = DefaultColors.buttonStart,
        buttonStop = DefaultColors.buttonStop,
        buttonSecondary = DefaultColors.buttonSecondary,
        border = DefaultColors.borderStrong,
        borderInput = DefaultColors.borderInput
    )
}

val LocalWatchdogColors = staticCompositionLocalOf { watchdogColorScheme("Default") }

@Composable
fun WoWWatchdogTheme(
    themeName: String = "Default",
    content: @Composable () -> Unit
) {
    val watchdogColors = watchdogColorScheme(themeName)

    val materialColorScheme = darkColorScheme(
        background = watchdogColors.background,
        surface = watchdogColors.surface,
        surfaceVariant = watchdogColors.surfaceVariant,
        primary = watchdogColors.primary,
        onBackground = watchdogColors.onBackground,
        onSurface = watchdogColors.onBackground,
        onSurfaceVariant = watchdogColors.onBackgroundSubtle,
        outline = watchdogColors.border,
        outlineVariant = watchdogColors.borderInput,
        error = StatusRed
    )

    CompositionLocalProvider(LocalWatchdogColors provides watchdogColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = WatchdogTypography,
            content = content
        )
    }
}
