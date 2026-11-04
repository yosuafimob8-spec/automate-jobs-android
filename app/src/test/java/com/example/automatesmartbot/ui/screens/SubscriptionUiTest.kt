package com.example.automatesmartbot.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.automatesmartbot.data.subscription.DemoSubscriptionStore
import com.example.automatesmartbot.ui.theme.*
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
class SubscriptionUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun chooseConfirmActivateManageAndExpireUseSelectedPackage() {
        var page by mutableStateOf("upgrade")
        var index by mutableIntStateOf(0)
        var now = 1000L
        val store = DemoSubscriptionStore(compose.activity) { now }
        compose.setContent { KarirMateTheme(ThemeMode.DARK) { when(page) {
            "upgrade" -> UpgradeProScreen { index = it; page = "checkout" }
            "checkout" -> ProCheckoutScreen(index, { page = "manage" }, store)
            else -> ManageProSubscriptionScreen({ page = "upgrade" }, store)
        } } }
        compose.onNodeWithText("Pilih paket Bulanan").performScrollTo().performClick()
        compose.onNodeWithText("Paket Bulanan — Rp79.000").assertExists()
        compose.onNodeWithText("Aktifkan PRO Demo").assertIsNotEnabled()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Aktifkan PRO Demo").assertIsEnabled().performClick()
        compose.onNodeWithText("Aktifkan PRO Demo").assertIsNotEnabled()
        compose.runOnIdle { assertEquals("monthly", store.current().planId) }
        compose.onNodeWithText("Kelola Langganan").performScrollTo().performClick()
        compose.onNodeWithText("PRO DEMO aktif").assertExists()
        compose.runOnIdle { now = store.current().expiresAt }
        compose.mainClock.advanceTimeBy(1100)
        compose.waitForIdle()
        compose.onNodeWithText("Tidak ada PRO DEMO aktif").assertExists()
        compose.onNodeWithText("Lihat paket / Perpanjang").performClick()
        compose.onNodeWithText("Upgrade KarirMate PRO").assertExists()
    }
    @Test fun invalidCheckoutDoesNotSilentlyPurchaseAnotherPlan() {
        compose.setContent { KarirMateTheme(ThemeMode.LIGHT) { ProCheckoutScreen(999, {}) } }
        compose.onNodeWithText("Paket tidak valid. Kembali ke daftar paket.").assertExists()
        compose.onNodeWithText("Aktifkan PRO Demo").assertDoesNotExist()
    }
}
