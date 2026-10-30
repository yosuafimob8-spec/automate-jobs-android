package com.example.automatesmartbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextWhite,
    primaryContainer = PrimaryBlueVariant,
    onPrimaryContainer = TextWhite,
    secondary = LightBlueAccent,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray
)

@Composable
fun AutoMateSmartBotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
