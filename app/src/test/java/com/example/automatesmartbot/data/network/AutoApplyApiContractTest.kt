package com.example.automatesmartbot.data.network

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AutoApplyApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ApiService
    @Before fun setup(){server=MockWebServer();server.start();api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)}
    @After fun cleanup(){server.shutdown()}
    @Test fun consentAndDailyLimitReachExistingEnableEndpoint()=runTest {
        server.enqueue(MockResponse().setBody("{\"success\":true}"))
        assertTrue(api.enableAutoApply(AutoApplyEnableRequest(true)).isSuccessful)
        val request=server.takeRequest();assertEquals("/auto-apply/enable",request.path)
        val text=request.body.readUtf8();assertTrue(text.contains("\"consent\":true"));assertTrue(text.contains("\"max_emails_per_day\":5"))
    }
    @Test fun targetSelectionUsesAuthenticatedPreferencesContract()=runTest {
        server.enqueue(MockResponse().setBody("{\"applied_to_delivery\":true,\"positions\":[\"Staff Logistik\"],\"locations\":[\"Bogor\"]}"))
        val preferences = AutoApplyTargetRequest(listOf("Staff Logistik"), listOf("Bogor"))
        assertTrue(api.saveAutoApplyPreferences(preferences).isSuccessful)
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/auto-apply/preferences", req.path)
        val body = req.body.readUtf8()
        assertTrue(body.contains("\"positions\":[\"Staff Logistik\"]"))
        assertTrue(body.contains("\"locations\":[\"Bogor\"]"))

        server.enqueue(MockResponse().setBody("{\"success\":true}"))
        assertTrue(api.enableAutoApply(AutoApplyEnableRequest(
            consent = true, positions = listOf("Staff Logistik"), locations = listOf("Bogor")
        )).isSuccessful)
        val enable = server.takeRequest()
        assertEquals("/auto-apply/enable", enable.path)
        val enableBody = enable.body.readUtf8()
        assertTrue(enableBody.contains("\"positions\":[\"Staff Logistik\"]"))
        assertTrue(enableBody.contains("\"locations\":[\"Bogor\"]"))
    }

    @Test fun oauthStartAndRawPdfUploadMatchBackendRoutes()=runTest {
        server.enqueue(MockResponse().setBody("{\"url\":\"https://accounts.google.com/o/oauth2/v2/auth\"}"))
        assertTrue(api.startGmailOAuth().isSuccessful);assertEquals("/gmail/oauth/start",server.takeRequest().path)
        server.enqueue(MockResponse().setBody("{\"success\":true}"))
        assertTrue(api.uploadAutoApplyCv("%PDF-fixture".toByteArray().toRequestBody("application/pdf".toMediaType())).isSuccessful)
        val request=server.takeRequest();assertEquals("PUT",request.method);assertEquals("/cv",request.path);assertEquals("application/pdf",request.getHeader("Content-Type"));assertEquals("%PDF-fixture",request.body.readUtf8())
    }
}
