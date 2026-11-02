package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.network.PsychotestApi
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response

class PsychotestViewModel @JvmOverloads constructor(private val savedState: SavedStateHandle, private val apiProvider: () -> PsychotestApi? = { ApiClient.psychotestApi }) : ViewModel() {
    val catalog = MutableStateFlow<List<JsonObject>>(emptyList())
    val questions = MutableStateFlow<List<JsonObject>>(emptyList())
    val session = MutableStateFlow<JsonObject?>(null)
    val result = MutableStateFlow<JsonObject?>(null)
    val loading = MutableStateFlow(false)
    val submitting = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val history = MutableStateFlow<List<JsonObject>>(emptyList())
    val stats = MutableStateFlow<JsonObject?>(null)
    val leaderboard = MutableStateFlow<List<JsonObject>>(emptyList())
    val pendingCount = MutableStateFlow(0)
    private val lock = Mutex()
    private val pending = savedState.get<HashMap<String, String>>("psychotest_pending") ?: hashMapOf()
    private var requests = 0

    init {
        pendingCount.value = pending.size
        savedState.get<String>("psychotest_session")?.let { id -> request { api -> loadQuestions(api, id) } }
    }
    fun loadCatalog() = request { api ->
        catalog.value = api.catalog().bodyOrError().getAsJsonArray("modes")?.map { it.asJsonObject }.orEmpty()
    }
    fun start(testId: String, questionCount: Int) = request { api -> lock.withLock {
        result.value = null; questions.value = emptyList(); session.value = null
        pending.clear(); savePending()
        val body = api.start(JsonObject().apply { addProperty("test_id", testId); addProperty("question_count", questionCount.coerceIn(10, 20)) }).bodyOrError()
        val current = requireNotNull(body.getAsJsonObject("session")) { "Sesi tidak tersedia." }
        val id = current.get("id").asString
        savedState["psychotest_session"] = id
        session.value = current
        loadQuestions(api, id)
    } }
    fun answer(questionId: String, optionId: String) {
        if (submitting.value || result.value != null || session.value == null) return
        if (questions.value.none { it.get("question_id").asString == questionId }) return
        pending[questionId] = optionId; savePending(); applyPending()
        syncAnswers()
    }
    fun syncAnswers() = request { api -> lock.withLock { flush(api) } }
    private fun savePending() { savedState["psychotest_pending"] = HashMap(pending); pendingCount.value = pending.size }
    private fun applyPending() {
        questions.value = questions.value.map { q ->
            pending[q.get("question_id").asString]?.let { value -> q.deepCopy().apply { addProperty("selected_option_id", value) } } ?: q
        }
    }
    private suspend fun flush(api: PsychotestApi) {
        val id = session.value?.get("id")?.asString ?: return
        while (pending.isNotEmpty()) {
            val batch = pending.toMap()
            fun body() = JsonObject().apply {
                addProperty("revision", session.value?.get("revision")?.asInt ?: 0)
                add("answers", JsonArray().apply { batch.forEach { (qid, oid) -> add(JsonObject().apply { addProperty("question_id", qid); addProperty("option_id", oid) }) } })
            }
            var response = api.answers(id, body())
            if (response.code() == 409) {
                // Refresh authoritative revision once; overlay local answers instead of discarding them.
                loadQuestions(api, id)
                if (result.value != null) return
                response = api.answers(id, body())
            }
            session.value = response.bodyOrError().getAsJsonObject("session")
            batch.forEach { (qid, oid) -> if (pending[qid] == oid) pending.remove(qid) }
            savePending()
        }
    }
    fun submit() {
        if (submitting.value) return
        submitting.value = true
        request { api ->
            try { lock.withLock {
                val id = session.value?.get("id")?.asString ?: return@withLock
                flush(api)
                if (result.value == null) {
                    val response = api.submit(id)
                    val body = if (response.code() == 409) api.result(id).bodyOrError() else response.bodyOrError()
                    session.value = body.getAsJsonObject("session"); result.value = requireNotNull(body.getAsJsonObject("result"))
                }
                savedState.remove<String>("psychotest_session")
            } } finally { submitting.value = false }
        }
    }
    fun loadExtras(includePro: Boolean = false) {
        request { api -> history.value = api.history().bodyOrError().getAsJsonArray("items")?.map { it.asJsonObject }.orEmpty() }
        if (includePro) {
            request { api -> stats.value = api.stats().bodyOrError() }
            request { api -> leaderboard.value = api.leaderboard().bodyOrError().getAsJsonArray("boards")?.flatMap { it.asJsonObject.getAsJsonArray("entries")?.map { e -> e.asJsonObject }.orEmpty() }.orEmpty() }
        }
    }
    private suspend fun loadQuestions(api: PsychotestApi, id: String) {
        val body = api.questions(id).bodyOrError()
        val current = requireNotNull(body.getAsJsonObject("session")) { "Sesi kosong dari server." }
        session.value = current
        if (current.get("status")?.asString == "submitted") {
            result.value = api.result(id).bodyOrError().getAsJsonObject("result")
            pending.clear(); savePending(); return
        }
        val loadedQuestions = body.getAsJsonArray("questions")?.map { it.asJsonObject }.orEmpty()
        require(loadedQuestions.isNotEmpty() && loadedQuestions.size <= 110) { "Server mengembalikan jumlah soal tidak valid (${loadedQuestions.size})." }
        questions.value = loadedQuestions
        applyPending()
    }
    private fun request(block: suspend (PsychotestApi) -> Unit) = viewModelScope.launch {
        requests++; loading.value = true; error.value = null
        try { block(apiProvider() ?: error("Layanan psikotes belum dikonfigurasi.")) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            error.value = when (e) {
                is HttpException -> when (e.code()) { 401 -> "Sesi akun berakhir. Silakan masuk kembali."; 403 -> "Akses ditolak server. Periksa langganan Anda."; else -> "API psikotes gagal (${e.code()}). Coba lagi; jawaban lokal tetap disimpan." }
                is java.io.IOException -> "Koneksi terputus. Jawaban lokal tetap disimpan; tekan Sinkronkan Jawaban."
                else -> e.message ?: "Permintaan psikotes gagal."
            }
        } finally { requests--; loading.value = requests > 0 }
    }
    private fun Response<JsonObject>.bodyOrError(): JsonObject {
        if (!isSuccessful) { errorBody()?.close(); throw HttpException(this) }
        return body()?.takeIf { it.get("ok")?.asBoolean == true } ?: error("Respons server tidak valid.")
    }
}
