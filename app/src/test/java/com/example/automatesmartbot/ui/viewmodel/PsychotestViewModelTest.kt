package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.automatesmartbot.data.network.PsychotestApi
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PsychotestViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun fastAnswerChangesAreRetainedAndRevisionRequestsAreSerialized() = runTest(dispatcher) {
        val api = FakePsychotestApi()
        val vm = PsychotestViewModel(SavedStateHandle()) { api }
        vm.start("logic", 10); advanceUntilIdle()
        vm.answer("q0", "a")
        assertEquals("a", vm.questions.value.first().get("selected_option_id").asString)
        runCurrent()
        vm.answer("q0", "b"); vm.answer("q1", "c")
        advanceUntilIdle()
        assertEquals("b", api.answers["q0"]); assertEquals("c", api.answers["q1"])
        assertEquals(0, api.conflicts); assertEquals(0, vm.pendingCount.value)
        assertEquals("b", vm.questions.value.first().get("selected_option_id").asString)
    }
    @Test fun offlineAnswerSurvivesRestorationAndCanBeRetried() = runTest(dispatcher) {
        val api = FakePsychotestApi(); val handle = SavedStateHandle()
        val vm = PsychotestViewModel(handle) { api }; vm.start("logic", 10); advanceUntilIdle()
        api.failAnswers = true
        vm.answer("q0", "b"); advanceUntilIdle()
        assertEquals(1, vm.pendingCount.value); assertNotNull(vm.error.value)
        val restored = PsychotestViewModel(handle) { api }; advanceUntilIdle()
        assertEquals("b", restored.questions.value.first().get("selected_option_id").asString)
        api.failAnswers = false; restored.syncAnswers(); advanceUntilIdle()
        assertEquals("b", api.answers["q0"]); assertEquals(0, restored.pendingCount.value)
    }
    @Test fun failedSubmitIsRetryableAndFlushesAnswersBeforeScoring() = runTest(dispatcher) {
        val api = FakePsychotestApi(); val vm = PsychotestViewModel(SavedStateHandle()) { api }
        vm.start("logic", 10); advanceUntilIdle()
        api.failAnswers = true; vm.answer("q0", "a"); advanceUntilIdle()
        vm.submit(); advanceUntilIdle()
        assertFalse(vm.submitting.value); assertNull(vm.result.value)
        api.failAnswers = false; vm.submit(); advanceUntilIdle()
        assertNotNull(vm.result.value); assertEquals("a", api.answers["q0"])
    }
    @Test fun emptySessionFailsWithoutInventingAResult() = runTest(dispatcher) {
        val api = FakePsychotestApi().apply { emptyQuestions = true }
        val vm = PsychotestViewModel(SavedStateHandle()) { api }
        vm.start("logic", 10); advanceUntilIdle()
        assertNotNull(vm.error.value); assertTrue(vm.questions.value.isEmpty()); assertNull(vm.result.value)
    }
}

private class FakePsychotestApi : PsychotestApi {
    val answers = mutableMapOf<String, String>()
    var revision = 0; var conflicts = 0; var failAnswers = false; var emptyQuestions = false
    private fun current() = JsonObject().apply { addProperty("id", "session"); addProperty("revision", revision); addProperty("remaining_seconds", 600); addProperty("status", "active") }
    private fun ok() = JsonObject().apply { addProperty("ok", true); add("session", current()) }
    override suspend fun catalog() = Response.success(ok())
    override suspend fun start(body: JsonObject): Response<JsonObject> = Response.success(ok())
    override suspend fun questions(id: String): Response<JsonObject> = Response.success(ok().apply {
        add("questions", JsonArray().apply { if (!emptyQuestions) repeat(10) { n -> add(JsonObject().apply {
            addProperty("question_id", "q$n"); addProperty("prompt", "Question $n")
            answers["q$n"]?.let { addProperty("selected_option_id", it) }
        }) } })
    })
    override suspend fun answers(id: String, body: JsonObject): Response<JsonObject> {
        delay(50)
        if (failAnswers) throw java.io.IOException("offline test fixture")
        if (body.get("revision").asInt != revision) { conflicts++; return Response.error(409, "revision conflict".toResponseBody()) }
        body.getAsJsonArray("answers").forEach { x -> val a = x.asJsonObject; answers[a.get("question_id").asString] = a.get("option_id").asString }
        revision++; return Response.success(ok())
    }
    override suspend fun submit(id: String) = Response.success(ok().apply { add("result", JsonObject().apply { addProperty("score", answers.size); addProperty("max_score", 10) }) })
    override suspend fun result(id: String) = submit(id)
    override suspend fun retryWrong(id: String) = Response.success(ok())
    override suspend fun history(limit: Int, offset: Int) = Response.success(ok().apply { add("items", JsonArray()) })
    override suspend fun stats() = Response.success(ok())
    override suspend fun leaderboard(board: String, testId: String?) = Response.success(ok())
}
