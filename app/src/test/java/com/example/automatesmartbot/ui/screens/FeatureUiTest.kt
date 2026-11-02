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
    @Test fun newCareerRoutesKeepOriginalBottomNavigation() {
        val original = com.example.automatesmartbot.ui.navigation.bottomNavItems.map { it.route }
        assertEquals(listOf("dashboard","cari_lowongan","kirim_lamaran","riwayat","profil"), original)
        assertTrue(com.example.automatesmartbot.ui.navigation.appShellItems.any { it.route == "career_documents" })
    }
    @Test fun resignationEditorGeneratesEditableTextAndExportControl() {
        compose.setContent { KarirMateTheme(ThemeMode.LIGHT) { CareerLettersScreen {} } }
        val inputs = compose.onAllNodes(hasSetTextAction())
        inputs[0].performTextReplacement("Dina")
        inputs[1].performScrollTo().performTextReplacement("PT Contoh")
        inputs[2].performScrollTo().performTextReplacement("Admin")
        inputs[3].performScrollTo().performTextReplacement("2026-10-31")
        inputs[4].performScrollTo().performTextReplacement("2026-12-01")
        compose.onNodeWithText("Buat Draf Resign").performScrollTo().performClick()
        compose.onNodeWithText("Ekspor PDF").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Preview A4").performScrollTo().performClick()
        compose.onNodeWithText("Tutup Preview").assertExists()
    }
    @Test fun trainingEvaluationPersistsCompletionAndEnablesPdfCertificate() {
        compose.activity.getSharedPreferences("career_training",0).edit().clear().commit()
        compose.setContent { KarirMateTheme(ThemeMode.LIGHT) { JobTrainingScreen() } }
        val course = com.example.automatesmartbot.data.career.TrainingCourses.all.first()
        // Follow the real catalog -> lesson -> practice -> exam -> certificate flow.
        compose.onNodeWithText(course.title).performScrollTo().performClick()
        val practice = "Saya akan periksa data yang masuk, melakukan verifikasi sumber informasi, mencatat setiap temuan secara lengkap, lalu melapor kepada penanggung jawab agar langkah perbaikan dapat dievaluasi dengan benar dan pekerjaan berjalan sesuai prosedur."
        course.lessons.forEach { lesson ->
            compose.onNodeWithText(lesson.first).performScrollTo().performClick()
            compose.onNodeWithText("Lanjut ke Latihan →").performScrollTo().performClick()
            compose.onAllNodes(hasSetTextAction())[0].performTextInput(practice)
            compose.onNodeWithText("Kumpulkan Jawaban").performScrollTo().performClick()
        }
        compose.onNodeWithText("Evaluasi Akhir →").performScrollTo().performClick()
        course.questions.forEachIndexed { index, q ->
            compose.onNodeWithText(q.options[q.correct]).performScrollTo().performClick()
            compose.onNodeWithText(if (index == course.questions.lastIndex) "Selesai" else "Berikutnya ›").performScrollTo().performClick()
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Dina Peserta")
        compose.onNodeWithText("Lihat Hasil").performScrollTo().performClick()
        compose.onNodeWithText("⬇ Unduh Sertifikat (PDF)").performScrollTo().assertIsEnabled()
        compose.runOnIdle {
            val history = org.json.JSONArray(compose.activity.getSharedPreferences("career_training",0).getString("certificates","[]"))
            assertEquals("Dina Peserta",history.getJSONObject(0).getString("name"))
            assertEquals(100,history.getJSONObject(0).getInt("score"))
        }
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
        compose.onNodeWithText("Soal 2 dari 10", substring = true).assertExists()
        compose.onNodeWithText("Sebelumnya").performScrollTo().performClick()
        compose.onNodeWithText("Soal 1 dari 10", substring = true).assertExists()
        compose.onAllNodes(isSelected()).assertCountEquals(1)
    }
}
