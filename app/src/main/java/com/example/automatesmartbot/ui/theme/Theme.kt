package com.example.automatesmartbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KarirMateDarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextWhite,
    primaryContainer = PrimaryBlueVariant,
    onPrimaryContainer = TextWhite,
    secondary = LightBlueAccent,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGray
)

val KarirMateLightColors = lightColorScheme(
    primary = Color(0xFFA8430F),
    onPrimary = TextWhite,
    primaryContainer = Color(0xFFFFF2E5),
    onPrimaryContainer = KarirTextDark,
    secondary = ActionOrange,
    onSecondary = Color.White,
    background = Color(0xFFFFFBF7),
    onBackground = KarirTextDark,
    surface = LightSurface,
    onSurface = KarirTextDark,
    surfaceVariant = Color(0xFFFFF2E5),
    onSurfaceVariant = Color(0xFF526277)
)

@Composable
fun KarirMateTheme(
    mode: ThemeMode = ThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (mode) {
        ThemeMode.AUTO -> false // KarirMate screens use fixed light backgrounds; prevent unreadable inherited dark text
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) KarirMateDarkColors else KarirMateLightColors,
        typography = Typography,
        content = content
    )
}

/** Match inherited component text to screens that intentionally use a fixed canvas. */
@Composable
fun FixedCanvasTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (dark) KarirMateDarkColors else KarirMateLightColors
    MaterialTheme(colorScheme = colors) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.onSurface,
            content = content
        )
    }
}
