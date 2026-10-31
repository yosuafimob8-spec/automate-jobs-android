package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
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
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RestorationUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun finishRemainsVisibleOnSmallScreenWithLongQuestionAndIncompleteAnswers() {
        val session=JsonObject().apply { addProperty("id","small-screen");addProperty("remaining_seconds",600) }
        val question=JsonObject().apply { addProperty("question_id","q1");addProperty("prompt","Pertanyaan panjang ".repeat(100));add("options",JsonArray().apply { repeat(4) { n -> add(JsonObject().apply { addProperty("id","$n");addProperty("text","Pilihan $n") }) } }) }
        var submitted=false
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { Box(Modifier.size(320.dp,480.dp)) { ExamPanel(session,listOf(question),false,0,{_,_->},{submitted=true},{}) } } }
        compose.onNodeWithText("Selesaikan Tes").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Selesaikan Tes").performClick()
        compose.onNodeWithText("Selesaikan").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(submitted) }
    }
    @Test fun personalStageSaveContinueIsVisibleAndValidatesWithoutClearingText() {
        val vm=CvBuilderViewModel(compose.activity.application,SavedStateHandle());vm.newDocument();vm.navigate("Informasi Pribadi")
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { Box(Modifier.size(360.dp,640.dp)) { CvBuilderScreen(vm) { vm.goBack() } } } }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Dina")
        compose.onNodeWithText("Simpan & Lanjut").assertIsDisplayed()
        compose.onNodeWithText("Simpan & Lanjut").performClick()
        compose.runOnIdle { assertEquals("Dina",vm.state.value.draft.contact.fullName);assertTrue(vm.state.value.validation.containsKey("email"));assertEquals("Informasi Pribadi",vm.history.value.last()) }
    }
    @Test fun freeResultsShowMotivationAndKeepProExplanationLocked() {
        val r=result()
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { PsychotestResultPanel(r,false,{},{}) } }
        compose.onNodeWithText("80/100").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        compose.onNodeWithText("Analisis Mendalam & Rekomendasi · PRO").assertIsDisplayed()
        compose.onNodeWithText("Pembahasan rahasia PRO").assertDoesNotExist()
    }
    @Test fun proResultRestoresRealCategoryAnalysisAndExplanations() {
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { PsychotestResultPanel(result(),true,{},{}) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        compose.onNodeWithText("Analisis Mendalam · PRO").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        compose.onNodeWithText("Capaian materi: 80.0%").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
        compose.onNodeWithText("Pembahasan rahasia PRO").assertIsDisplayed()
    }
    @Test fun newTrainingCatalogShowsEightFieldsAndKeepsLegacyEntry() {
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { JobTrainingScreen() } }
        compose.onNodeWithText("Administrasi Perkantoran").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Dasar Dunia Kerja").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Materi & Sertifikat Versi Sebelumnya").performScrollTo().assertIsDisplayed()
    }
    @Test fun dashboardKeepsCareerRoutesAndReportsUsableInDarkTheme() {
        var route = ""
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { Box(Modifier.width(320.dp)) { DashboardContent(null,false) { route = it } } } }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        compose.onNodeWithText("Pelatihan Kerja").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("job_training",route) }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
        compose.onNodeWithText("Buka Riwayat Lamaran").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("riwayat",route) }
    }
    private fun result()=JsonObject().apply {
        addProperty("score",8);addProperty("max_score",10);addProperty("title","Logika");addProperty("correct_count",8);addProperty("wrong_count",2);addProperty("blank_count",0)
        add("categories",JsonArray().apply { add(JsonObject().apply { addProperty("label","Logika");addProperty("percent",80) }) })
        add("questions",JsonArray().apply { add(JsonObject().apply { addProperty("prompt","Soal asli");addProperty("explanation","Pembahasan rahasia PRO") }) })
    }
}
