package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.theme.KarirMateTheme
import com.example.automatesmartbot.ui.theme.ThemeMode
import com.example.automatesmartbot.ui.viewmodel.CvBuilderViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w480dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CvCatalogUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun catalogPaginationFilterProPreviewAndFreeSelectionFollowHierarchy() {
        val vm = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
        vm.newDocument(); vm.updateContent { CvTemplates.sample.copy(contact = CvTemplates.sample.contact.copy(fullName = "Pengguna Katalog")) }; vm.navigate("Template")
        // Even a forged UI flag must not authorize a PRO template.
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { CvBuilderScreen(vm, isPro = true) { vm.goBack() } } }
        compose.onNodeWithText("1 / 7").performScrollTo().assertExists()
        compose.onNodeWithText("Berikutnya").performScrollTo().performClick()
        compose.onNodeWithText("2 / 7").performScrollTo().assertExists()
        compose.onNodeWithText("ATS 06").performScrollTo().performClick()
        compose.onNodeWithText("ATS 06").assertExists()
        compose.onNodeWithText("Gunakan Template Ini").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("Preview Template", vm.history.value.last())
            assertEquals("ats_01", vm.state.value.templateId)
            vm.goBack()
        }
        compose.onNodeWithText("ATS", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("1 / 2").performScrollTo().assertExists()
        compose.onNodeWithText("ATS 03").performScrollTo().performClick()
        compose.onNodeWithText("Gunakan Template Ini").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("CV Saya", vm.history.value.last())
            assertEquals("ats_03", vm.state.value.templateId)
            assertEquals("Pengguna Katalog", vm.state.value.draft.contact.fullName)
            val reopened = CvBuilderViewModel(compose.activity.application, SavedStateHandle())
            assertEquals(vm.state.value.draft, reopened.state.value.draft)
            assertEquals("ats_03", reopened.state.value.templateId)
        }
    }
}
