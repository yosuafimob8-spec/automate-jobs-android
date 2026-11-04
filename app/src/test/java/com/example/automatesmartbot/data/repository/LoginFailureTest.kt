package com.example.automatesmartbot.data.repository

import com.example.automatesmartbot.data.model.LoginRequest
import com.example.automatesmartbot.data.network.ApiService
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.ConnectException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class LoginFailureTest {
    @Test fun refusedLocalBackendHasAccurateActionableMessage() {
        val message = LoginFailure.network(ConnectException(), "http://127.0.0.1:8787/")
        assertTrue(message.contains("Termux"))
        assertTrue(message.contains("127.0.0.1:8787"))
        assertFalse(message.contains("sandi"))
        assertFalse(LoginFailure.network(ConnectException(), "https://login.example.org/").contains("Termux"))
    }

    @Test fun timeoutDnsAndAuthenticationFailuresRemainDistinct() {
        assertTrue(LoginFailure.network(SocketTimeoutException(), "http://127.0.0.1:8787/").contains("Waktu tunggu"))
        assertTrue(LoginFailure.network(UnknownHostException(), "https://login.example.org/").contains("Alamat server"))
        assertEquals("Email atau sandi tidak sesuai.", LoginFailure.response(401))
        assertTrue(LoginFailure.response(403).contains("403"))
        assertTrue(LoginFailure.response(429).contains("Terlalu banyak"))
        assertTrue(LoginFailure.response(503).contains("HTTP 503"))
        assertTrue(LoginFailure.response(204).contains("tidak lengkap"))
    }

    private fun repository(server: MockWebServer, timeoutMillis: Long = 2000): AuthRepository {
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().proxy(Proxy.NO_PROXY)
                .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS).retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
        return AuthRepository(sessionManager = null, loginApi = api, loginBaseUrl = { "http://127.0.0.1:8787/" })
    }

    @Test fun validResponseUsesRealLoginEndpointAndRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""{"access_token":"fixture-token","token_type":"bearer","email_verified":true}"""))
            val result = repository(server).login(LoginRequest("qa@example.test", "fixture-password"))
            assertTrue(result.isSuccess)
            assertEquals("fixture-token", result.getOrThrow().access_token)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/auth/login", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains("qa@example.test"))
            assertTrue(body.contains("fixture-password"))
        }
    }

    @Test fun emptyOrMissingTokenCannotBecomeSuccessfulLogin() = runTest {
        MockWebServer().use { server ->
            server.start()
            listOf("""{"access_token":""}""", "{}").forEach { body ->
                server.enqueue(MockResponse().setBody(body))
                val result = repository(server).login(LoginRequest("qa@example.test", "fixture-password"))
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull()!!.message!!.contains("token"))
            }
        }
    }

    @Test fun rejectedLoginAndBackendFailureHaveDifferentMessages() = runTest {
        MockWebServer().use { server ->
            server.start()
            val repository = repository(server)
            server.enqueue(MockResponse().setResponseCode(401))
            assertEquals(LoginFailure.response(401), repository.login(LoginRequest("qa@example.test", "wrong")).exceptionOrNull()!!.message)
            server.enqueue(MockResponse().setResponseCode(503))
            assertEquals(LoginFailure.response(503), repository.login(LoginRequest("qa@example.test", "fixture-password")).exceptionOrNull()!!.message)
            server.enqueue(MockResponse().setResponseCode(204))
            assertEquals(LoginFailure.response(204), repository.login(LoginRequest("qa@example.test", "fixture-password")).exceptionOrNull()!!.message)
        }
    }

    @Test fun slowAndStoppedBackendFailWithoutBypassingAuthentication() = runTest {
        val server = MockWebServer()
        server.start()
        val repository = repository(server, 100)
        server.enqueue(MockResponse().setHeadersDelay(400, TimeUnit.MILLISECONDS).setBody("{}"))
        try {
            assertTrue(repository.login(LoginRequest("qa@example.test", "fixture-password")).exceptionOrNull()!!.message!!.contains("Waktu tunggu"))
        } finally { server.shutdown() }
        val stopped = repository.login(LoginRequest("qa@example.test", "fixture-password"))
        assertTrue(stopped.isFailure)
        assertTrue(stopped.exceptionOrNull()!!.message!!.contains("Backend Termux belum merespons"))
    }
}
