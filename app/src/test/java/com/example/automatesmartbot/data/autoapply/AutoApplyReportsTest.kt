package com.example.automatesmartbot.data.autoapply

import com.example.automatesmartbot.data.network.ApiService
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class AutoApplyReportsTest {
    @Test fun unconfirmedSentAndDryRunCanNeverBecomeConfirmedDelivery() {
        assertTrue(DeliveryStatus.label(JSONObject("{\"status\":\"sent\"}")).startsWith("Belum pasti"))
        assertTrue(DeliveryStatus.label(JSONObject("{\"status\":\"sent\",\"gmail_message_id\":\"gmail-real-confirmation\"}")).startsWith("Terkirim"))
        assertFalse(DeliveryStatus.label(JSONObject("{\"status\":\"dry-run\"}")).startsWith("Terkirim"))
    }
    @Test fun reportFiltersAndSimulationUseRealRoutesAndPreserveServerReason()=runTest {
        MockWebServer().use { server ->
            server.start()
            val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
            val repo=AutoApplyReportRepository(api)
            server.enqueue(MockResponse().setBody("{\"items\":[],\"summary\":{\"total_sent\":12},\"auto_apply\":{\"remaining_today\":3},\"next_offset\":null}"))
            val report=repo.report("7d");assertEquals(12,report.summary.getInt("total_sent"));assertEquals("/karirmate/report?period=7d&offset=0",server.takeRequest().path)
            server.enqueue(MockResponse().setBody("{\"mode\":\"simulation\",\"sending_enabled\":false,\"sent\":0,\"plan\":[]}"));assertEquals(0,repo.simulate().getInt("sent"));assertEquals("/auto-apply/simulate",server.takeRequest().path)
            server.enqueue(MockResponse().setResponseCode(409).setBody("{\"code\":\"CANDIDATE_NOT_FOUND\",\"error\":\"Tautkan akun\"}"))
            try { repo.report("today");fail("Expected error") } catch(e:IllegalStateException) { assertTrue(e.message!!.contains("CANDIDATE_NOT_FOUND"));assertTrue(e.message!!.contains("Tautkan akun")) }
        }
    }
}
