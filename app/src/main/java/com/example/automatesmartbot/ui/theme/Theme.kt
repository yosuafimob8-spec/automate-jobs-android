package com.example.automatesmartbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = TextWhite,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = Navy,
    secondary = Cyan,
    onSecondary = Navy,
    background = LightBackground,
    onBackground = Navy,
    surface = LightSurface,
    onSurface = Navy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF47627E)
)

@Composable
fun KarirMateTheme(
    mode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
