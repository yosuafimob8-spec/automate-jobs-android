package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.career.PayrollCalculator
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.AppInput
import com.example.automatesmartbot.ui.navigation.Screen
import com.example.automatesmartbot.ui.screens.auth.LoginScreen
import com.example.automatesmartbot.ui.theme.*
import com.example.automatesmartbot.ui.viewmodel.AuthViewModel
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import com.example.automatesmartbot.ui.viewmodel.DashboardViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.NumberFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ElevenBugUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var mode by mutableStateOf(ThemeMode.LIGHT)
    private var assertedMode = ThemeMode.LIGHT
    private fun bothModes(check: () -> Unit) {
        listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { value ->
            assertedMode = value
            compose.runOnIdle { mode = value }
            check()
        }
    }
    private fun input(index: Int = 0) = compose.onAllNodes(hasSetTextAction())[index]
    private fun assertInputInk(index: Int = 0) {
        val results = mutableListOf<TextLayoutResult>()
        input(index).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Text layout must be available", results.isNotEmpty())
        val colors = if (assertedMode == ThemeMode.DARK) KarirMateDarkColors else KarirMateLightColors
        assertEquals(colors.onSurface, results.first().layoutInput.style.color)
    }

    private fun assertAuthLightInputInk(index: Int = 0) {
        val results = mutableListOf<TextLayoutResult>()
        input(index).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("Auth input layout must be available", results.isNotEmpty())
        assertEquals(KarirMateLightColors.onSurface, results.first().layoutInput.style.color)
    }

    @Test fun quickSimulationButtonCalculatesAfterEditsAndErrors() {
        compose.setContent { KarirMateTheme(mode) { SalaryCalculatorScreen() } }
        val expected = PayrollCalculator.calculate(PayrollCalculator.Input(
            5000000, 5000000, 2, PayrollCalculator.OvertimeDay.WORKDAY,
            5000000, 5000000, 5000000, 54000000, 12, 5000000))
        val amount = NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(expected.monthlyTakeHomeEstimate)
        bothModes {
            compose.onNodeWithText("Coba Simulasi Rp5.000.000").performScrollTo().performClick()
            input().performScrollTo().assertTextEquals("5000000")
            assertInputInk()
            compose.onNodeWithText("PERKIRAAN GAJI DITERIMA").performScrollTo().assertExists()
            compose.onAllNodesWithText(amount).assertCountEquals(2)
            input().performScrollTo().performTextReplacement("")
            compose.onNodeWithText("Hitung Estimasi Gaji").performScrollTo().performClick()
            compose.onNodeWithText("Isi nominal penghasilan dan dasar lembur terlebih dahulu.").performScrollTo().assertExists()
            compose.onNodeWithText("Coba Simulasi Rp5.000.000").performScrollTo().performClick()
            compose.onNodeWithText("Isi nominal penghasilan dan dasar lembur terlebih dahulu.").assertDoesNotExist()
            compose.onAllNodesWithText(amount).assertCountEquals(2)
            compose.onNodeWithText("Hari istirahat (5 hari kerja/minggu)").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithText("Coba Simulasi Rp5.000.000").performScrollTo().performClick()
            compose.onNodeWithText("Hari kerja biasa").performScrollTo().assertIsSelected()
        }
    }

    @Test fun everySalaryInputUsesReadableInkFocusedAndUnfocused() {
        lateinit var focus: FocusManager
        compose.setContent { KarirMateTheme(mode) { focus = LocalFocusManager.current; SalaryCalculatorScreen() } }
        compose.onNodeWithText("Atur BPJS, Pajak & THR  ▼").performScrollTo().performClick()
        bothModes {
            compose.onAllNodes(hasSetTextAction()).assertCountEquals(8)
            repeat(8) { index ->
                input(index).performScrollTo().performTextReplacement("123")
                assertInputInk(index)
                compose.runOnIdle { focus.clearFocus() }
                input(index).assertTextEquals("123")
                assertInputInk(index)
            }
        }
    }

    @Test fun workpediaSearchFiltersAcceptsClearAndKeepsReadableInk() {
        compose.setContent { KarirMateTheme(mode) { WorkPediaScreen {} } }
        bothModes {
            input().performTextReplacement("PKWTT")
            assertInputInk()
            compose.onAllNodes(hasText("PKWTT") and !hasSetTextAction(), useUnmergedTree = true).assertCountEquals(2)
            input().performTextReplacement("zzzz-not-a-work-term")
            compose.onNodeWithText("PKWTT", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("×").performClick()
            input().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
            compose.onAllNodes(hasText("PKWT") and !hasSetTextAction(), useUnmergedTree = true).assertCountEquals(2)
        }
    }

    @Test fun cvAllEditableStagesKeepTypedContentAndThemeAcrossNavigation() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument()
        vm.updateContent { CvTemplates.sample.copy(extras = listOf(CvExtraSection("references", "Referensi", "Nama rujukan"), CvExtraSection("languages", "Bahasa", "Indonesia"))) }
        compose.setContent { KarirMateTheme(mode) { CvBuilderScreen(vm) { vm.goBack() } } }
        val stages = listOf("Informasi Pribadi", "Pendidikan", "Pengalaman", "Keterampilan", "Objektif / Ringkasan", "Detail Proyek", "Sertifikasi", "Referensi", "Tambahkan Bagian Lainnya")
        bothModes {
            stages.forEach { stage ->
                compose.runOnIdle { vm.navigate(stage) }
                if (compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty()) {
                    val add = mapOf("Detail Proyek" to "Tambah Proyek", "Sertifikasi" to "Tambah Sertifikasi")[stage]
                    checkNotNull(add) { "Missing editor in $stage" }
                    compose.onNodeWithText(add).performScrollTo().performClick()
                }
                val count = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size
                assertTrue("Inputs in $stage", count > 0)
                repeat(count) { index ->
                    input(index).performScrollTo()
                    assertInputInk(index)
                }
                input().performScrollTo().performTextReplacement("Data pengguna $stage")
                compose.runOnIdle { vm.goBack(); vm.navigate(stage) }
                input().performScrollTo().assertTextEquals("Data pengguna $stage")
                compose.runOnIdle { vm.goBack() }
            }
        }
        compose.runOnIdle {
            val restored = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
            assertEquals(vm.state.value.draft, restored.state.value.draft)
        }
    }

    @Test fun resignationInputsEditableDraftAndPreviewWorkInBothThemes() {
        compose.setContent { KarirMateTheme(mode) { CareerLettersScreen {} } }
        bothModes {
            val values = listOf("Dina", "PT Tujuan", "Admin", "2026-11-04", "2026-12-04", "Pengembangan karier")
            values.forEachIndexed { index, value ->
                input(index).performScrollTo().performTextReplacement(value)
                assertInputInk(index)
            }
            compose.onNodeWithText("Buat Draf Resign").performScrollTo().performClick()
            input(6).performScrollTo().assertTextContains("PT Tujuan", substring = true)
            assertInputInk(6)
            input(6).performTextReplacement("Surat pengguna\nBaris kedua tetap dapat diedit")
            compose.onNodeWithText("Ekspor PDF").performScrollTo().assertIsEnabled()
            compose.onNodeWithText("Preview A4").performScrollTo().performClick()
            compose.onNodeWithText("Tutup Preview").performScrollTo().performClick()
            input(6).performScrollTo().assertTextEquals("Surat pengguna\nBaris kedua tetap dapat diedit")
        }
    }

    @Test fun applicationLetterEditorUsesTypedFactsAndRetainsPreviewExport() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument(); vm.updateContent { CvTemplates.sample }
        compose.setContent { KarirMateTheme(mode) { CvBuilderScreen(vm) { vm.goBack() } } }
        bothModes {
            compose.runOnIdle { vm.resetNavigation(); vm.navigate("Pilih Jenis Surat") }
            compose.onNodeWithText("Surat Lamaran Standar").performScrollTo().performClick()
            input().performScrollTo().performTextReplacement("PT Tujuan QA")
            input(1).performScrollTo().performTextReplacement("Admin Data")
            assertInputInk(); assertInputInk(1)
            compose.onNodeWithText("Buat Draf Surat").performScrollTo().performClick()
            input().performScrollTo().assertTextContains("PT Tujuan QA", substring = true)
            assertInputInk()
            input().performTextReplacement("Isi surat pengguna\nAdmin Data di PT Tujuan QA")
            compose.onNodeWithText("Preview Surat").performScrollTo().performClick()
            compose.onNodeWithText("Ekspor Surat PDF").performScrollTo().assertIsEnabled()
            compose.runOnIdle {
                assertEquals("Isi surat pengguna\nAdmin Data di PT Tujuan QA", vm.state.value.coverLetter!!.content)
                vm.goBack()
            }
            input().performScrollTo().assertTextEquals("Isi surat pengguna\nAdmin Data di PT Tujuan QA")
        }
    }

    @Test fun cvHomeOffersCreateEditTemplateAndAnalyzerWithoutLosingFeature() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument()
        compose.setContent { KarirMateTheme(mode) { CvBuilderScreen(vm) { vm.goBack() } } }
        bothModes {
            compose.onNodeWithText("+ Buat CV Baru").performScrollTo().assertIsEnabled()
            compose.onNodeWithText("Edit CV").performScrollTo().assertIsEnabled()
            compose.onNodeWithText("CV Analyst").performScrollTo().performClick()
            compose.runOnIdle { assertEquals("CV Analyst", vm.history.value.last()); vm.goBack() }
            compose.onNodeWithText("Template").performScrollTo().performClick()
            compose.runOnIdle { assertEquals("Template", vm.history.value.last()); vm.goBack() }
        }
    }

    @Test fun dashboardHasOnlyOneCvEntranceAndRetainsRoute() {
        var destination = ""
        val vm = DashboardViewModel()
        compose.setContent { KarirMateTheme(mode) { DashboardScreen({ destination = it }, vm) } }
        bothModes {
            compose.onAllNodesWithText("CV Analyze").assertCountEquals(0)
            compose.onNodeWithText("CV Builder").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(Screen.CvBuilder.route, destination) }
        }
    }

    @Test fun logoutRemainsClickableInBothThemesAndCallsOnlyLogoutCallback() {
        var logouts = 0
        var edits = 0
        compose.setContent { KarirMateTheme(mode) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ProfileIdentityCard(null, null, null, null, {}, { edits++ }, { logouts++ })
            }
        } }
        bothModes { compose.onNodeWithText("Keluar").performScrollTo().assertIsEnabled().performClick() }
        compose.runOnIdle { assertEquals(2, logouts); assertEquals(0, edits) }
    }

    @Test fun loginEmailPasswordMaskToggleAndValidationWorkInBothThemes() {
        val vm = AuthViewModel()
        var navigated = false
        compose.setContent { KarirMateTheme(mode) { LoginScreen({}, {}, { navigated = true }, vm) } }
        bothModes {
            input().performTextReplacement("")
            input(1).performTextReplacement("")
            compose.onNodeWithText("Masuk", substring = false).performClick()
            compose.onNodeWithText("Email dan password tidak boleh kosong.").assertExists()
            input().performTextReplacement("qa@example.test")
            input().assertTextContains("qa@example.test")
            assertAuthLightInputInk()
            input(1).performTextReplacement("fixture-password")
            input(1).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            assertAuthLightInputInk(1)
            compose.onNodeWithContentDescription("Tampilkan sandi").performClick()
            input(1).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
            input(1).assertTextContains("fixture-password")
            compose.onNodeWithContentDescription("Sembunyikan sandi").performClick()
            input(1).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            compose.onNodeWithText("Masuk", substring = false).assertIsEnabled()
        }
        compose.runOnIdle { assertFalse(navigated) }
    }

    @Test fun actualThemePickerRecomposesAndPersistsAcrossRecreation() {
        val preference = ThemePreference(compose.activity)
        var observed = Color.Unspecified
        compose.setContent { KarirMateTheme(preference.mode) {
            observed = MaterialTheme.colorScheme.surface
            AppearanceCard(preference.mode, preference::selectMode)
        } }
        compose.onNodeWithText("Gelap").performClick()
        compose.runOnIdle {
            assertEquals(KarirMateDarkColors.surface, observed)
            assertEquals(ThemeMode.DARK, ThemePreference(compose.activity).mode)
        }
        compose.onNodeWithText("Terang").performClick()
        compose.runOnIdle {
            assertEquals(KarirMateLightColors.surface, observed)
            assertEquals(ThemeMode.LIGHT, ThemePreference(compose.activity).mode)
        }
        compose.onNodeWithText("Ikuti sistem").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.AUTO, ThemePreference(compose.activity).mode) }
    }

    @Test fun trainingHistoryQrInputUsesThemeWithoutChangingOldCertificates() {
        val prefs = compose.activity.getSharedPreferences("career_training", 0)
        val previous = """[{"id":"legacy-certificate","course":"Pelatihan lama","name":"Pengguna lama","date":"2026-10-01","score":90}]"""
        prefs.edit().putString("certificates", previous).commit()
        compose.setContent { KarirMateTheme(mode) { JobTrainingScreen() } }
        compose.onNodeWithText("Lihat Riwayat Sertifikat", substring = true).performScrollTo().performClick()
        bothModes {
            input().performScrollTo().performTextReplacement("identitas-tidak-terdaftar")
            assertInputInk()
            compose.onNodeWithText("Periksa Sertifikat").performScrollTo().performClick()
            compose.onNodeWithText("Tidak ditemukan pada riwayat lokal perangkat ini.").performScrollTo().assertExists()
        }
        compose.runOnIdle { assertEquals(previous, prefs.getString("certificates", null)) }
    }

    @Test fun trainingPracticeInputKeepsTypedAnswerWhenThemeChanges() {
        val prefs = compose.activity.getSharedPreferences("career_training", 0)
        prefs.edit().clear().commit()
        compose.setContent { KarirMateTheme(mode) { JobTrainingScreen() } }
        val course = com.example.automatesmartbot.data.career.TrainingCourses.all.first()
        compose.onNodeWithText("Lanjutkan", substring = true).performScrollTo().performClick()
        compose.onNodeWithText(course.lessons.first().first).performScrollTo().performClick()
        compose.onNodeWithText("Lanjut ke Latihan →").performScrollTo().performClick()
        val answer = "Jawaban pengguna\nTetap tersimpan setelah mode tampilan diganti."
        input().performScrollTo().performTextReplacement(answer)
        bothModes {
            input().performScrollTo().assertTextContains(answer)
            assertInputInk()
        }
        compose.runOnIdle { assertTrue(prefs.all.values.contains(answer)) }
    }

    @Test fun visualSlidesKeepVerifiedBackdropAndNextPreviousContentInBothThemes() {
        val first = "Materi pertama dengan uraian yang cukup panjang untuk mengisi satu slide pembelajaran dan memastikan navigasi ke slide berikutnya tetap menggunakan naskah yang diberikan pengguna."
        val second = "Materi kedua memeriksa alat sebelum bekerja dan melaporkan temuan kepada penanggung jawab."
        compose.setContent { KarirMateTheme(mode) { Column { TrainingVideo("missing_test_media", 0, "Judul Materi", "$first $second") } } }
        bothModes {
            assertEquals(Color(0xFF152D43), TrainingSlideBackdrop)
            assertTrue(TrainingSlideBackdrop.luminance() > Color.Black.luminance())
            compose.onNodeWithText("Judul Materi").assertExists()
            compose.onNodeWithText(first).assertExists()
            compose.onNodeWithText("Sebelumnya").assertIsNotEnabled()
            compose.onNodeWithText("Berikutnya").performClick()
            compose.onNodeWithText(second).assertExists()
            compose.onNodeWithText("Berikutnya").assertIsNotEnabled()
            compose.onNodeWithText("Sebelumnya").performClick()
            compose.onNodeWithText(first).assertExists()
        }
    }

    @Test fun multilineAppInputRetainsInkFocusAndErrorState() {
        var value by mutableStateOf("")
        lateinit var focus: FocusManager
        compose.setContent { KarirMateTheme(mode) {
            focus = LocalFocusManager.current
            AppInput("Isi surat", value, { value = it }, minLines = 4, error = "Periksa isian")
        } }
        bothModes {
            input().performTextReplacement("Baris pertama\nBaris kedua")
            assertInputInk()
            compose.runOnIdle { focus.clearFocus() }
            input().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("Baris pertama\nBaris kedua")))
            assertInputInk()
            compose.onNodeWithText("Periksa isian").assertExists()
        }
    }
}
