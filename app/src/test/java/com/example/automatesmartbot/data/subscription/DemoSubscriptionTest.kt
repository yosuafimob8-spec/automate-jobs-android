package com.example.automatesmartbot.data.subscription

import android.app.Application
import com.example.automatesmartbot.data.cv.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DemoSubscriptionTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    @Test fun allAgreedPlansHaveCorrectPricesDurationsAndSurviveRestart() {
        assertEquals(listOf(5000,25000,79000,499000), DemoPlans.all.map { it.rupiah })
        assertEquals(listOf(1,7,30,365), DemoPlans.all.map { it.days })
        DemoPlans.all.forEach { plan ->
            File(app.filesDir,"pro_demo_subscription.json").delete()
            val store = DemoSubscriptionStore(app) { 1000L }
            val result = store.activate(plan.id, "request_${plan.id}")
            assertEquals(1000 + plan.days * 86_400_000L, result.expiresAt)
            assertEquals(result, DemoSubscriptionStore(app).current())
            assertTrue(result.active(result.expiresAt - 1))
            assertFalse(result.active(result.expiresAt))
        }
    }
    @Test fun renewalAddsFromActiveExpiryAndExpiredRenewalStartsNow() {
        var now = 1000L
        val store = DemoSubscriptionStore(app) { now }
        val first = store.activate("daily", "request_0001")
        now += 100
        val second = store.activate("weekly", "request_0002")
        assertEquals(first.expiresAt + 7 * 86_400_000L, second.expiresAt)
        now = second.expiresAt + 100
        assertFalse(store.current().active(now))
        val third = store.activate("monthly", "request_0003")
        assertEquals(now + 30 * 86_400_000L, third.expiresAt)
    }
    @Test fun repeatedAndConcurrentRequestsDoNotActivateTwice() {
        val store = DemoSubscriptionStore(app) { 1000L }
        val first = store.activate("yearly", "same_request")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = (1..12).map { executor.submit<DemoSubscription> { DemoSubscriptionStore(app) { 1000L }.activate("yearly", "same_request") } }.map { it.get() }
            assertTrue(results.all { it == first })
            assertEquals(first, store.current())
            assertTrue(runCatching { store.activate("daily", "same_request") }.isFailure)
            assertEquals(first, DemoSubscriptionStore(app).current())
        } finally { executor.shutdownNow() }
    }
    @Test fun invalidPlanOverflowAndFailedWritesDoNotClaimSuccess() {
        val store = DemoSubscriptionStore(app) { 1000L }
        assertTrue(runCatching { store.activate("invalid", "request_valid") }.isFailure)
        assertFalse(store.current().active(1000))
        assertTrue(runCatching { DemoSubscriptionStore(app) { Long.MAX_VALUE - 100 }.activate("yearly", "request_overflow") }.isFailure)
        val blocked = File(app.filesDir, "pro_demo_subscription.json.new").apply { mkdir() }
        try { assertTrue(runCatching { store.activate("daily", "request_fail") }.isFailure); assertFalse(store.current().active(1000)) }
        finally { blocked.delete() }
    }
    @Test fun accountBindingAndOpaqueTokenRenewalPreserveOnlyOwnersDemoState() {
        var token: String? = "opaque-demo-first"
        bindCvWorkspace(app, token!!, "id:demo-owner-a")
        val first = DemoSubscriptionStore(app, { token }) { 1000L }.activate("weekly", "request_account")
        token = "opaque-demo-renewed"
        val renewed = DemoSubscriptionStore(app, { token }) { 1000L }
        assertFalse(renewed.current().active(1000))
        assertTrue(runCatching { renewed.activate("daily", "request_unbound") }.isFailure)
        bindCvWorkspace(app, token!!, "id:demo-owner-a")
        assertEquals(first, renewed.current())
        token = "opaque-demo-other"; bindCvWorkspace(app, token!!, "id:demo-owner-b")
        assertFalse(DemoSubscriptionStore(app, { token }) { 1000L }.current().active(1000))
    }

    @Test fun demoActivationDoesNotAuthorizeProductionTemplatesOrMutateSessionPrefs() = runTest {
        val prefs = app.getSharedPreferences("secure_auth_prefs", 0)
        val before = prefs.all.toMap()
        DemoSubscriptionStore(app).activate("yearly", "request_demo")
        assertEquals(before, prefs.all)
        val authority = ServerCvAccess(token = { null }, fetch = { error("No server call without an authenticated session") })
        authority.refresh()
        assertFalse(authority.canUse(CvTemplates.catalog.first { CvTemplates.isProTemplate(it.id) }.id))
    }
}
