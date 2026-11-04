package com.example.automatesmartbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KarirMateDarkColors = darkColorScheme(
    primary = KarirOrange,
    
    onPrimary = Color.Black,
    primaryContainer = PrimaryBlueVariant,
    onPrimaryContainer = TextWhite,
    secondary = KarirOrangeDeep,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF475569)
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
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFE8D6C9)
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
