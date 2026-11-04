package com.example.automatesmartbot.ui.screens

import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.example.automatesmartbot.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TrainingNarrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var mode by mutableStateOf(ThemeMode.LIGHT)
    private fun engine(): ShadowTextToSpeech = Shadow.extract(ShadowTextToSpeech.getLastTextToSpeechInstance())
    private fun bothThemes(check: (ThemeMode) -> Unit) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { value ->
            compose.runOnIdle { mode = value }
            check(value)
        }
    }
    private fun ready() {
        compose.runOnIdle {
            ShadowTextToSpeech.addLanguageAvailability(Locale("id", "ID"))
            engine().onInitListener.onInit(TextToSpeech.SUCCESS)
        }
        compose.waitForIdle()
    }
    private fun assertInk(label: String, expected: Color) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Rendered text layout for $label", results.isNotEmpty())
        assertEquals("Rendered ink for $label", expected, results.first().layoutInput.style.color)
    }

    @Test fun unavailableAndFailedEngineKeepBothControlsDisabledAndReadable() {
        compose.setContent { KarirMateTheme(mode) { TrainingNarration("Judul", "Isi materi") } }
        bothThemes { value ->
            val colors = if (value == ThemeMode.DARK) KarirMateDarkColors else KarirMateLightColors
            compose.onNodeWithText("Dengarkan").assertIsNotEnabled()
            compose.onNodeWithText("Berhenti").assertIsNotEnabled()
            assertInk("Dengarkan", colors.onSurface)
            assertInk("Berhenti", colors.onSurfaceVariant)
            compose.onNodeWithText("Dengarkan").performTouchInput { click() }
            compose.runOnIdle {
                assertNull(engine().lastSpokenText)
                engine().onInitListener.onInit(TextToSpeech.ERROR)
            }
            compose.onNodeWithText("Dengarkan").assertIsNotEnabled()
            compose.onNodeWithText("Berhenti").assertIsNotEnabled()
        }
    }

    @Test fun listenReplayAndStopRetainCallbacksAndReadableStatesInBothThemes() {
        val title = "Pemeriksaan alat"
        val body = "Periksa kabel dan catat hasil pemeriksaan."
        compose.setContent { KarirMateTheme(mode) { TrainingNarration(title, body) } }
        ready()
        bothThemes { value ->
            val colors = if (value == ThemeMode.DARK) KarirMateDarkColors else KarirMateLightColors
            compose.onNodeWithText("Dengarkan").assertIsEnabled()
            compose.onNodeWithText("Berhenti").assertIsNotEnabled()
            assertInk("Dengarkan", OnKarirOrange)
            assertInk("Berhenti", colors.onSurfaceVariant)
            compose.onNodeWithText("Dengarkan").performClick()
            compose.onNodeWithText("Putar Ulang").assertIsEnabled()
            compose.onNodeWithText("Berhenti").assertIsEnabled()
            assertInk("Putar Ulang", OnKarirOrange)
            assertInk("Berhenti", colors.onSurface)
            var spokenCount = 0
            compose.runOnIdle {
                assertEquals("$title. $body", engine().lastSpokenText)
                assertEquals(TextToSpeech.QUEUE_FLUSH, engine().queueMode)
                assertEquals(Locale("id", "ID"), engine().currentLanguage)
                assertFalse(engine().isStopped)
                spokenCount = engine().spokenTextList.size
            }
            compose.onNodeWithText("Putar Ulang").performClick()
            compose.runOnIdle { assertEquals(spokenCount + 1, engine().spokenTextList.size) }
            compose.onNodeWithText("Berhenti").performClick()
            compose.onNodeWithText("Dengarkan").assertIsEnabled()
            compose.onNodeWithText("Berhenti").assertIsNotEnabled()
            assertInk("Berhenti", colors.onSurfaceVariant)
            compose.runOnIdle { assertTrue(engine().isStopped) }
        }
    }

    @Test fun changingThemeDuringPlaybackPreservesEngineAndStopAction() {
        compose.setContent { KarirMateTheme(mode) { TrainingNarration("Judul", "Isi materi") } }
        ready()
        compose.onNodeWithText("Dengarkan").performClick()
        val original = ShadowTextToSpeech.getLastTextToSpeechInstance()
        var count = 0
        compose.runOnIdle { count = engine().spokenTextList.size }
        bothThemes { value ->
            compose.onNodeWithText("Putar Ulang").assertIsEnabled()
            compose.onNodeWithText("Berhenti").assertIsEnabled()
            assertInk("Berhenti", if (value == ThemeMode.DARK) KarirMateDarkColors.onSurface else KarirMateLightColors.onSurface)
            compose.runOnIdle {
                assertSame(original, ShadowTextToSpeech.getLastTextToSpeechInstance())
                assertEquals(count, engine().spokenTextList.size)
                assertFalse(engine().isShutdown)
            }
        }
        compose.onNodeWithText("Berhenti").performClick()
        compose.runOnIdle { assertTrue(engine().isStopped) }
    }

    @Test fun leavingAudioDisposesEngineWithoutChangingLessonText() {
        var visible by mutableStateOf(true)
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { if (visible) TrainingNarration("Judul", "Naskah asli.") } }
        ready()
        compose.onNodeWithText("Dengarkan").performClick()
        val original = engine()
        compose.runOnIdle { visible = false }
        compose.runOnIdle {
            assertEquals("Judul. Naskah asli.", original.lastSpokenText)
            assertTrue(original.isStopped)
            assertTrue(original.isShutdown)
        }
    }

    @Test fun enabledAndDisabledLabelsBordersAndBackgroundsHaveMeasuredContrast() {
        var check: (() -> Unit)? = null
        compose.setContent { KarirMateTheme(mode) {
            val stop = narrationStopButtonColors()
            val play = karirButtonColors()
            val activeBorder = narrationStopBorderColor(true)
            val inactiveBorder = narrationStopBorderColor(false)
            val card = ScreenSurface
            val colors = MaterialTheme.colorScheme
            check = {
                fun ratio(a: Color, b: Color): Float =
                    (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
                listOf(
                    Triple("listen/replay", play.contentColor, play.containerColor),
                    Triple("listen unavailable", play.disabledContentColor, play.disabledContainerColor),
                    Triple("stop active", stop.contentColor, stop.containerColor),
                    Triple("stop inactive", stop.disabledContentColor, stop.disabledContainerColor),
                    Triple("audio heading", colors.onSurface, card),
                    Triple("audio description", colors.onSurfaceVariant, card)
                ).forEach { (label, ink, background) ->
                    assertTrue("$label text contrast ${ratio(ink, background)}", ratio(ink, background) >= 4.5f)
                }
                listOf(
                    activeBorder to stop.containerColor,
                    activeBorder to card,
                    inactiveBorder to stop.disabledContainerColor,
                    inactiveBorder to card
                ).forEach { (border, background) ->
                    assertTrue("Stop border contrast ${ratio(border, background)}", ratio(border, background) >= 3f)
                }
                assertEquals(1f, stop.containerColor.alpha)
                assertEquals(1f, stop.disabledContainerColor.alpha)
            }
        } }
        bothThemes { compose.runOnIdle { check!!() } }
    }
}
