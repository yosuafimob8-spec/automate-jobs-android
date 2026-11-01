package com.example.automatesmartbot.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automatesmartbot.data.interview.InterviewRepository
import com.example.automatesmartbot.data.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class InterviewViewModel @JvmOverloads constructor(private val saved: SavedStateHandle, private val repository: () -> InterviewRepository = { InterviewRepository(requireNotNull(ApiClient.interviewApi) { "Layanan interview belum diinisialisasi." }) }) : ViewModel() {
    val capabilities = MutableStateFlow<JSONObject?>(null)
    val session = MutableStateFlow<JSONObject?>(saved.get<String>("interview_session")?.let(::JSONObject))
    val history = MutableStateFlow<JSONObject?>(null)
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val answer = saved.getStateFlow("interview_answer", "")
    val webRtcAnswer = MutableStateFlow<JSONObject?>(null)
    fun updateAnswer(text: String) { saved["interview_answer"] = text }
    private fun store(value: JSONObject) {
        val pending = saved.get<String>("pending_request")?.let(::JSONObject)
        val turns = value.optJSONArray("answers")
        val acknowledged = pending != null && turns != null && (0 until turns.length()).any {
            turns.getJSONObject(it).optString("request_id") == pending.optString("request_id")
        }
        if (acknowledged) {
            if (answer.value.trim() == pending!!.optString("answer")) saved["interview_answer"] = ""
            saved.remove<String>("pending_request")
        }
        saved["interview_session"] = value.toString(); session.value = value
    }
    fun load() = request { repo -> capabilities.value = repo.capabilities(); session.value?.let { store(repo.session(it.getString("id"))) } }
    fun start(settings: Map<String, Any>) = request { repo -> store(repo.start(settings)); saved["interview_answer"] = ""; saved.remove<String>("pending_request"); webRtcAnswer.value = null }
    fun submit() = request { repo ->
        val current = session.value ?: error("Sesi belum tersedia.")
        val text = answer.value.trim(); require(text.length >= 3) { "Tuliskan jawaban terlebih dahulu." }
        val pending = saved.get<String>("pending_request")?.let(::JSONObject)
        val request = if (pending != null && pending.optString("answer") == text && pending.optInt("revision") == current.getInt("revision")) pending else JSONObject().put("request_id", UUID.randomUUID().toString()).put("answer", text).put("revision", current.getInt("revision"))
        saved["pending_request"] = request.toString()
        store(repo.answer(current.getString("id"), mapOf("answer" to request.getString("answer"), "request_id" to request.getString("request_id"), "revision" to request.getInt("revision"))))
        saved["interview_answer"] = ""; saved.remove<String>("pending_request")
    }
    fun sync() = request { repo -> session.value?.let { store(repo.session(it.getString("id"))) } }
    fun finish() = request { repo -> session.value?.let { store(repo.finish(it.getString("id"))) } }
    fun loadHistory() = request { repo -> history.value = repo.history() }
    fun openHistory(id: String) = request { repo -> val value = repo.session(id); saved.remove<String>("pending_request"); saved["interview_answer"] = ""; store(value); webRtcAnswer.value = null }
    fun offer(sdp: String) = request { repo ->
        val id = session.value?.getString("id") ?: error("Sesi video belum tersedia.")
        webRtcAnswer.value = repo.offer(id, sdp); store(repo.session(id))
    }
    fun transcribe(audio: ByteArray, format: String) = request { repo ->
        val id = session.value?.getString("id") ?: error("Sesi belum tersedia.")
        updateAnswer(repo.transcribe(id, audio, format))
    }
    suspend fun questionAudio(): ByteArray = repository().speech(session.value?.getString("id") ?: error("Sesi belum tersedia."))
    fun showError(value: String) { error.value = value }
    fun newSetup() { saved.remove<String>("pending_request"); session.value = null; saved.remove<String>("interview_session"); saved["interview_answer"] = ""; webRtcAnswer.value = null }
    private fun request(action: suspend (InterviewRepository) -> Unit) = viewModelScope.launch {
        if (busy.value) return@launch
        busy.value = true; error.value = null
        try { action(repository()) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error.value = e.message ?: "Permintaan gagal. Jawaban Anda tetap disimpan." }
        finally { busy.value = false }
    }
}
