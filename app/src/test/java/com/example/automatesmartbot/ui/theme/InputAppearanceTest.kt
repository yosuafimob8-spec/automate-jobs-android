package com.example.automatesmartbot.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.automatesmartbot.ui.screens.TrainingSlideBackdrop
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InputAppearanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun ratio(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

    @Test fun fieldAllStatesLabelsPlaceholdersCursorAndButtonsHavePairedContrast() {
        val mode = mutableStateOf(ThemeMode.LIGHT)
        var assertions: (() -> Unit)? = null
        compose.setContent { KarirMateTheme(mode.value) {
            val fields = karirInputColors()
            val buttons = karirButtonColors()
            val scheme = MaterialTheme.colorScheme
            val pairs = listOf(
                fields.focusedTextColor to fields.focusedContainerColor,
                fields.unfocusedTextColor to fields.unfocusedContainerColor,
                fields.errorTextColor to fields.errorContainerColor,
                fields.disabledTextColor to fields.disabledContainerColor,
                fields.focusedLabelColor to fields.focusedContainerColor,
                fields.unfocusedLabelColor to fields.unfocusedContainerColor,
                fields.focusedPlaceholderColor to fields.focusedContainerColor,
                fields.unfocusedPlaceholderColor to fields.unfocusedContainerColor,
                buttons.contentColor to buttons.containerColor,
                buttons.disabledContentColor to buttons.disabledContainerColor
            )
            assertions = {
                pairs.forEach { (foreground, background) ->
                    assertTrue("Insufficient contrast $foreground on $background", ratio(foreground, background) >= 4.5f)
                }
                assertTrue(ratio(fields.cursorColor, fields.focusedContainerColor) >= 3f)
                assertTrue(ratio(fields.focusedLeadingIconColor, fields.focusedContainerColor) >= 3f)
                assertTrue(ratio(fields.unfocusedTrailingIconColor, fields.unfocusedContainerColor) >= 3f)
                assertTrue(ratio(scheme.error, scheme.surface) >= 4.5f)
                assertEquals(scheme.onSurface, fields.focusedTextColor)
                assertEquals(scheme.onSurface, fields.unfocusedTextColor)
                assertEquals(KarirOrange, buttons.containerColor)
            }
        } }
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { value ->
            compose.runOnIdle { mode.value = value }
            compose.runOnIdle { assertions!!() }
        }
    }

    @Test @Config(qualifiers = "night")
    fun automaticThemeUsesDarkSystemConfiguration() {
        var surface = Color.Unspecified
        compose.setContent { KarirMateTheme(ThemeMode.AUTO) { surface = MaterialTheme.colorScheme.surface } }
        compose.runOnIdle { assertEquals(KarirMateDarkColors.surface, surface) }
    }

    @Test @Config(qualifiers = "notnight")
    fun automaticThemeUsesLightSystemAndInvalidSavedModeFallsBack() {
        val context = compose.activity
        context.getSharedPreferences("karir_mate_preferences", 0).edit().putString("theme_mode", "invalid").commit()
        assertEquals(ThemeMode.AUTO, ThemePreference(context).mode)
        var surface = Color.Unspecified
        compose.setContent { KarirMateTheme(ThemeMode.AUTO) { surface = MaterialTheme.colorScheme.surface } }
        compose.runOnIdle { assertEquals(KarirMateLightColors.surface, surface) }
    }

    @Test fun slideForegroundsAndOrangeButtonHaveReadableContrast() {
        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFFFFB787)).forEach { ink ->
            assertTrue(ratio(ink, TrainingSlideBackdrop) >= 4.5f)
        }
        listOf(KarirMateLightColors, KarirMateDarkColors).forEach { scheme ->
            assertTrue(ratio(scheme.onPrimary, scheme.primary) >= 4.5f)
        }
        assertTrue(ratio(Color(0xFFA8430F), Color(0xFFFFF0E5)) >= 4.5f)
    }
}
