package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.cv.CvTemplates
import com.example.automatesmartbot.ui.components.AppTopBar
import com.example.automatesmartbot.ui.theme.KarirMateTheme
import com.example.automatesmartbot.ui.theme.ThemeMode
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FeatureUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun cvHeaderAndAndroidBackKeepFormDataAndPopOneStep() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument(); vm.navigate("CV Saya")
        compose.setContent {
            val history by vm.history.collectAsState()
            KarirMateTheme(ThemeMode.LIGHT) {
                Column {
                    AppTopBar(history.last(), false, {}, { vm.goBack() })
                    CvBuilderScreen(vm) { vm.goBack() }
                }
            }
        }
        compose.onNodeWithText("Pendidikan").performScrollTo().performClick()
        compose.onNodeWithText("Tambah Pendidikan").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Universitas Pengguna")
        compose.onAllNodesWithContentDescription("Kembali").assertCountEquals(1)
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.runOnIdle { assertEquals("CV Saya", vm.history.value.last()) }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle {
            assertEquals("CV Builder", vm.history.value.last())
            assertEquals("Universitas Pengguna", vm.state.value.draft.education.first().school)
        }
    }
    @Test fun analystButtonOpensLetterTypeEditorAndPreview() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument(); vm.updateContent { CvTemplates.sample }; vm.navigate("CV Analyst")
        compose.setContent { KarirMateTheme(ThemeMode.LIGHT) { CvBuilderScreen(vm) { vm.goBack() } } }
        compose.onNodeWithText("Buat Surat Lamaran").performScrollTo().performClick()
        compose.onNodeWithText("Surat Lamaran Standar").performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("PT Tujuan")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("Admin")
        compose.onNodeWithText("Buat Draf Surat").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Editor Surat", vm.history.value.last()); assertTrue(vm.state.value.coverLetter!!.content.contains("PT Tujuan")) }
        compose.onNodeWithText("Preview Surat").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Preview Surat", vm.history.value.last()) }
        compose.onNodeWithText("Ekspor Surat PDF").performScrollTo().assertIsEnabled()
    }
    @Test fun previousIsDisabledOnFirstQuestionAndChoiceSurvivesNextPrevious() {
        val session = JsonObject().apply { addProperty("id", "ui-test"); addProperty("remaining_seconds", 600) }
        var questions by mutableStateOf((0..9).map { n -> JsonObject().apply {
            addProperty("question_id", "q$n"); addProperty("prompt", "Pertanyaan $n")
            add("options", JsonArray().apply { listOf("a", "b", "c", "d").forEach { value -> add(JsonObject().apply { addProperty("id", value); addProperty("text", "Pilihan $value soal $n") }) } })
        } })
        compose.setContent { KarirMateTheme(ThemeMode.LIGHT) {
            ExamPanel(session, questions, false, 0, { qid, oid -> questions = questions.map { q -> if (q.get("question_id").asString == qid) q.deepCopy().apply { addProperty("selected_option_id", oid) } else q } }, {}, {})
        } }
        compose.onNodeWithText("Sebelumnya").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Pilihan b soal 0").performScrollTo().performClick()
        compose.onNodeWithText("Selanjutnya").performScrollTo().performClick()
        compose.onNodeWithText("Soal 2 dari 10").assertExists()
        compose.onNodeWithText("Sebelumnya").performScrollTo().performClick()
        compose.onNodeWithText("Soal 1 dari 10").assertExists()
        compose.onAllNodes(isSelected()).assertCountEquals(1)
    }
}
