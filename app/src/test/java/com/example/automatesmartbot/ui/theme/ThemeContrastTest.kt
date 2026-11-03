package com.example.automatesmartbot.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun ratio(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }
    @Test fun sharedTextAndOrangeButtonsHaveReadableContrast() {
        listOf(KarirMateLightColors, KarirMateDarkColors).forEach { colors ->
            assertTrue(ratio(colors.onSurface, colors.surface) >= 4.5f)
            assertTrue(ratio(colors.onSurfaceVariant, colors.surfaceVariant) >= 4.5f)
            assertTrue(ratio(colors.onPrimary, colors.primary) >= 4.5f)
            assertTrue(ratio(colors.onPrimaryContainer, colors.primaryContainer) >= 4.5f)
        }
        assertTrue(ratio(Color.White, ActionOrange) >= 4.5f)
        assertTrue(ratio(KarirMateLightColors.onSecondary, KarirMateLightColors.secondary) >= 4.5f)
        assertTrue(ratio(KarirMateDarkColors.onSecondary, KarirMateDarkColors.secondary) >= 4.5f)
        assertTrue(ratio(KarirMateLightColors.onBackground, KarirMateLightColors.background) >= 4.5f)
        assertTrue(ratio(KarirMateDarkColors.onBackground, KarirMateDarkColors.background) >= 4.5f)
    }
}
