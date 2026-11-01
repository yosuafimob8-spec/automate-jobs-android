package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.ui.theme.KarirMateTheme
import com.example.automatesmartbot.ui.theme.ThemeMode
import com.example.automatesmartbot.ui.viewmodel.InterviewViewModel
import com.example.automatesmartbot.data.autoapply.DeliveryStatus
import com.example.automatesmartbot.ui.navigation.*
import org.json.JSONObject
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
class NationalInterviewUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun storedTextSessionHasVisibleSubmitOnSmallDarkScreenAndPreservesDraft() {
        val saved=SavedStateHandle(mapOf("interview_session" to """{"id":"fixture","revision":0,"status":"active","question_number":1,"question_limit":5,"provider":"structured_rules","question":"Ceritakan pengalaman administrasi yang benar-benar Anda miliki.","settings":{"position":"Admin","language":"id","mode":"text"},"answers":[]}"""))
        val vm=InterviewViewModel(saved) { error("No network provider in this UI fixture") }
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { Box(Modifier.size(360.dp,640.dp)) { AiInterviewScreen(vm=vm) } } }
        compose.onNode(hasScrollAction()).performScrollToNode(hasSetTextAction())
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("Pengalaman administrasi nyata.")
        compose.onNodeWithText("Kirim Jawaban").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle {assertEquals("Pengalaman administrasi nyata.",vm.answer.value)}
    }
    @Test fun interviewScoresDoNotExposeProExampleToFreeAndShowActualMethod() {
        val result=JSONObject("""{"overall":70,"quality":72,"relevance":65,"communication":73,"evaluation_method":"structured_text_rules","note":"Berdasarkan teks, bukan penampilan","strengths":["Contoh relevan"],"weaknesses":["Perjelas hasil"],"recommendations":["Latih STAR"],"improved_answer":"PRO private example"}""")
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { InterviewResult(result,false) } }
        compose.onNodeWithText("70/100").assertExists();compose.onNodeWithText("Metode: structured_text_rules").assertExists();compose.onNodeWithText("PRO private example").assertDoesNotExist()
    }
    @Test fun newDirectoryRouteRetainsAllFiveOriginalBottomTabsAndGmailStatusIsNotHrReceipt() {
        assertEquals(listOf("dashboard","cari_lowongan","kirim_lamaran","riwayat","profil"),bottomNavItems.map{it.route})
        assertTrue(appShellItems.any{it.route=="company_directory"})
        val accepted=DeliveryStatus.label(JSONObject("""{"status":"sent","gmail_message_id":"fixture-confirmation"}"""))
        assertTrue(accepted.startsWith("Diterima Gmail API"));assertTrue(accepted.contains("belum membuktikan dibaca HR"))
        assertEquals("Disiapkan",DeliveryStatus.label(JSONObject("{\"status\":\"prepared\"}")))
        assertEquals("Dalam antrean",DeliveryStatus.label(JSONObject("{\"status\":\"queued\"}")))
    }
}
