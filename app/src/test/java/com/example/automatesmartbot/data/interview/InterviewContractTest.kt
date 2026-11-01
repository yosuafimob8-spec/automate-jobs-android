package com.example.automatesmartbot.data.interview

import com.example.automatesmartbot.data.autoapply.readJson
import com.example.automatesmartbot.data.network.ApiService
import com.example.automatesmartbot.ui.viewmodel.InterviewViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class InterviewContractTest {
    private val dispatcher=StandardTestDispatcher()
    @Before fun setup(){ Dispatchers.setMain(dispatcher) }
    @After fun cleanup(){ Dispatchers.resetMain() }
    @Test fun activeCvUsesSharedFactualContextWithoutSendingPrivateContacts()=runTest(dispatcher) {
        MockWebServer().use { server ->
            server.start()
            val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(InterviewApi::class.java)
            val cv=com.example.automatesmartbot.data.cv.CvContent(
                contact=com.example.automatesmartbot.data.cv.CvContact(fullName="Private Person",email="private@example.com"),
                summary="Pengalaman administrasi dokumen dan menyusun laporan mingguan dengan Excel.")
            server.enqueue(MockResponse().setBody("{\"session\":{\"id\":\"session\",\"revision\":0}}"))
            InterviewRepository(api).start(mapOf("cv" to cv,"use_backend_cv" to false))
            val context=JSONObject(server.takeRequest().body.readUtf8()).getString("cv")
            assertTrue(context.contains("Excel"));assertFalse(context.contains("private@example.com"));assertFalse(context.contains("Private Person"))
        }
    }
    @Test fun directoryFiltersAndOneEmailPreviewUseCorrectAuthenticatedApiRoutes()=runTest(dispatcher) {
        MockWebServer().use { server -> server.start(); val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
            server.enqueue(MockResponse().setBody("{\"success\":true,\"items\":[]}"));api.companies("93","Merauke","Logistik","Admin",50).readJson()
            val search=server.takeRequest();assertTrue(search.path!!.contains("province=93"));assertTrue(search.path!!.contains("city=Merauke"));assertTrue(search.path!!.contains("offset=50"))
            server.enqueue(MockResponse().setBody("{\"preview\":{\"sending_enabled\":false}}"));val preview=api.testEmailPreview(mapOf("recipient" to "fixture@example.com")).readJson();assertFalse(preview.getJSONObject("preview").getBoolean("sending_enabled"))
            val request=server.takeRequest();assertEquals("/auto-apply/test-email-preview",request.path);assertTrue(request.body.readUtf8().contains("fixture@example.com"))
        }
    }
    @Test fun interviewEndpointsCarryLanguageCvRevisionAndStableRequestIdentity()=runTest(dispatcher) {
        MockWebServer().use { server -> server.start();val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(InterviewApi::class.java);val repo=InterviewRepository(api)
            server.enqueue(MockResponse().setBody("{\"session\":{\"id\":\"session\",\"revision\":0}}"));repo.start(mapOf("language" to "en","mode" to "text","cv" to mapOf("summary" to "Actual user CV")))
            val start=server.takeRequest();assertEquals("/interviews/sessions",start.path);val body=JSONObject(start.body.readUtf8());assertEquals("en",body.getString("language"));assertEquals("Actual user CV",body.getJSONObject("cv").getString("summary"))
            server.enqueue(MockResponse().setBody("{\"session\":{\"id\":\"session\",\"revision\":1}}"));repo.answer("session",mapOf("answer" to "Actual answer","request_id" to "request-123","revision" to 0))
            val answer=server.takeRequest();assertEquals("/interviews/sessions/session/answers",answer.path);assertEquals("request-123",JSONObject(answer.body.readUtf8()).getString("request_id"))
            server.enqueue(MockResponse().setResponseCode(403).setBody("{\"code\":\"PRO_REQUIRED\",\"error\":\"PRO aktif diperlukan\"}"))
            try {repo.offer("session","v=0 mock-offer");fail("Expected denial")}catch(e:IllegalStateException){assertTrue(e.message!!.contains("PRO_REQUIRED"))}
        }
    }
    @Test fun offlineReplyRetainsTextAndRetriesSameRequestAcrossViewModelRestoration()=runTest(dispatcher) {
        val api=FixtureInterviewApi();val repo=InterviewRepository(api);val saved=SavedStateHandle();val vm=InterviewViewModel(saved){repo}
        vm.start(mapOf("language" to "id")).join();vm.updateAnswer("Saya mengelola administrasi dan laporan.");api.fail=true;vm.submit().join()
        assertEquals("Saya mengelola administrasi dan laporan.",vm.answer.value);assertNotNull(vm.error.value)
        val restored=InterviewViewModel(saved){repo};api.fail=false;restored.submit().join()
        assertEquals(2,api.submitted.size);assertEquals(api.submitted[0]["request_id"],api.submitted[1]["request_id"]);assertEquals("",restored.answer.value);assertEquals(1,restored.session.value!!.getInt("revision"))
    }
    @Test fun syncAcknowledgementClearsOnlyTheAlreadyAcceptedDraft()=runTest(dispatcher) {
        val api=FixtureInterviewApi();val saved=SavedStateHandle();val vm=InterviewViewModel(saved){InterviewRepository(api)}
        vm.start(mapOf("language" to "id")).join();vm.updateAnswer("Jawaban telah diterima server tetapi respons terputus.");api.fail=true;vm.submit().join()
        val pending=JSONObject(saved.get<String>("pending_request")!!)
        api.serverSession=JSONObject().put("id","fixture-session").put("revision",1).put("status","active").put("answers",org.json.JSONArray().put(JSONObject().put("request_id",pending.getString("request_id"))))
        vm.sync().join();assertEquals("",vm.answer.value);assertNull(saved.get<String>("pending_request"));assertEquals(1,api.submitted.size)
    }
    @Test fun providerFailureDoesNotCreateFakeInterviewOrScore()=runTest(dispatcher) {
        val api=FixtureInterviewApi();val vm=InterviewViewModel(SavedStateHandle()){InterviewRepository(api)};api.failStart=true
        vm.start(mapOf("language" to "id")).join();assertNull(vm.session.value);assertNotNull(vm.error.value)
    }
}

private class FixtureInterviewApi: InterviewApi {
    var fail=false;var failStart=false;var serverSession:JSONObject?=null;val submitted=mutableListOf<Map<String,Any>>()
    private fun response(revision:Int=0):Response<ResponseBody> = Response.success("{\"session\":{\"id\":\"fixture-session\",\"revision\":$revision,\"status\":\"active\"}}".toResponseBody())
    override suspend fun capabilities()=Response.success("{\"text\":true,\"deep_analysis\":false,\"question_engine\":\"structured_rules\"}".toResponseBody())
    override suspend fun start(settings:Map<String,Any>):Response<ResponseBody>{if(failStart)throw java.io.IOException("Fixture offline");return response()}
    override suspend fun session(id:String)=serverSession?.let { Response.success("{\"session\":$it}".toResponseBody()) } ?: response()
    override suspend fun answer(id:String,body:Map<String,Any>):Response<ResponseBody>{submitted+=body;if(fail)throw java.io.IOException("Fixture response lost");return response(1)}
    override suspend fun finish(id:String)=response(1)
    override suspend fun history()=Response.success("{\"items\":[]}".toResponseBody())
    override suspend fun offer(id:String,body:Map<String,String>)=Response.success("{\"sdp\":\"v=0 fixture\",\"type\":\"answer\"}".toResponseBody())
    override suspend fun transcribe(id:String,body:Map<String,String>)=Response.success("{\"text\":\"Actual transcript\"}".toResponseBody())
    override suspend fun speech(id:String)=Response.success("{\"audio_base64\":\"UklGRg==\"}".toResponseBody())
}
