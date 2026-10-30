package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.network.ApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PsychotestViewModel : ViewModel() {
    val catalog = MutableStateFlow<List<JsonObject>>(emptyList())
    val questions = MutableStateFlow<List<JsonObject>>(emptyList())
    val session = MutableStateFlow<JsonObject?>(null)
    val result = MutableStateFlow<JsonObject?>(null)
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val history = MutableStateFlow<List<JsonObject>>(emptyList())
    val stats = MutableStateFlow<JsonObject?>(null)
    val leaderboard = MutableStateFlow<List<JsonObject>>(emptyList())

    fun loadCatalog() = request { api ->
        val body = api.catalog().bodyOrError()
        catalog.value = body.getAsJsonArray("modes")?.mapNotNull { it.asJsonObject } ?: emptyList()
    }
    fun start(testId: String) = request { api ->
        val body = api.start(JsonObject().apply { addProperty("test_id", testId) }).bodyOrError()
        session.value = body.getAsJsonObject("session")
        loadQuestions(api, session.value!!.get("id").asString)
    }
    fun answer(questionId: String, optionId: String) {
        val current = session.value ?: return
        request { api ->
            val answers = JsonArray().apply { add(JsonObject().apply { addProperty("question_id", questionId); addProperty("option_id", optionId) }) }
            val body = JsonObject().apply { addProperty("revision", current.get("revision")?.asInt ?: 0); add("answers", answers) }
            val response = api.answers(current.get("id").asString, body).bodyOrError()
            session.value = response.getAsJsonObject("session")
            val selected = questions.value.map { q -> if (q.get("question_id").asString == questionId) q.deepCopy().apply { addProperty("selected_option_id", optionId) } else q }
            questions.value = selected
        }
    }
    fun submit() = request { api ->
        val id = session.value?.get("id")?.asString ?: return@request
        val body = api.submit(id).bodyOrError()
        session.value = body.getAsJsonObject("session")
        result.value = body.getAsJsonObject("result")
    }
    fun retryWrong() = request { api ->
        val id = session.value?.get("id")?.asString ?: return@request
        val body = api.retryWrong(id).bodyOrError(); session.value = body.getAsJsonObject("session"); result.value = null
        loadQuestions(api, session.value!!.get("id").asString)
    }
    fun loadExtras() = request { api ->
        history.value = api.history().bodyOrError().getAsJsonArray("items")?.map { it.asJsonObject } ?: emptyList()
        stats.value = api.stats().bodyOrError()
        leaderboard.value = api.leaderboard().bodyOrError().getAsJsonArray("boards")?.flatMap { it.asJsonObject.getAsJsonArray("entries")?.map { e -> e.asJsonObject } ?: emptyList() } ?: emptyList()
    }
    private suspend fun loadQuestions(api: com.example.automatesmartbot.data.network.PsychotestApi, id: String) {
        val body = api.questions(id).bodyOrError()
        session.value = body.getAsJsonObject("session")
        questions.value = body.getAsJsonArray("questions")?.mapNotNull { it.asJsonObject } ?: emptyList()
    }
    private fun request(block: suspend (com.example.automatesmartbot.data.network.PsychotestApi) -> Unit) = viewModelScope.launch {
        loading.value = true; error.value = null
        try { block(ApiClient.psychotestApi ?: error("Layanan psikotes belum dikonfigurasi.")) }
        catch (e: Exception) { error.value = e.localizedMessage ?: "Permintaan psikotes gagal." }
        finally { loading.value = false }
    }
    private fun retrofit2.Response<JsonObject>.bodyOrError(): JsonObject = body()?.takeIf { it.get("ok")?.asBoolean == true } ?: error(errorBody()?.string() ?: "Respons server tidak valid")
}
