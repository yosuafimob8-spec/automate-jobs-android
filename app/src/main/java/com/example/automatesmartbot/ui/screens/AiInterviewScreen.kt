package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class InterviewTurn(val question: String, val answer: String)
private data class InterviewReply(
    val question: String = "",
    val feedback: String = "",
    val score: Int? = null,
    val improvement: String = "",
    val finalReport: String = ""
)

private suspend fun requestInterview(
    endpoint: String, role: String, language: String, cv: String,
    action: String, history: List<InterviewTurn>
): InterviewReply = withContext(Dispatchers.IO) {
    require(endpoint.startsWith("https://") || endpoint.startsWith("http://10.0.2.2:")) {
        "Gunakan endpoint HTTPS untuk layanan AI Interview."
    }
    val payload = JSONObject().apply {
        put("role", role)
        put("language", language)
        put("cv_text", cv.take(6000))
        put("action", action)
        put("history", JSONArray().apply {
            history.forEach { turn ->
                put(JSONObject().put("question", turn.question).put("answer", turn.answer))
            }
        })
    }
    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 45000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Layanan AI Interview belum siap (HTTP ${connection.responseCode}).")
        }
        val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val finalReport = if (action == "finish") buildString {
            append("Nilai: ${response.optInt("overall_score", 0)}/100\\n\\n")
            for (key in listOf("strengths", "improvements")) {
                append(if (key == "strengths") "Kelebihan:\\n" else "Yang perlu diperbaiki:\\n")
                val items = response.optJSONArray(key) ?: JSONArray()
                for (i in 0 until items.length()) append("• ${items.optString(i)}\\n")
                append("\\n")
            }
            append("Contoh jawaban: ${response.optString("example_answer")}\\n\\n")
            append(response.optString("encouragement"))
            val english = response.optString("english_feedback")
            if (english.isNotBlank()) append("\\n\\nEnglish: $english")
        } else ""
        InterviewReply(
            question = response.optString("question"),
            feedback = response.optString("feedback"),
            score = if (response.has("answer_score") && !response.isNull("answer_score"))
                response.optInt("answer_score") else null,
            improvement = response.optString("improvement"),
            finalReport = finalReport
        )
    } finally {
        connection.disconnect()
    }
}

@Composable
fun AiInterviewScreen() {
    val roles = listOf("Admin Warehouse", "Supervisor Logistik", "Staff Operasional", "Operator Produksi", "Sales", "Lainnya")
    var role by rememberSaveable { mutableStateOf(roles.first()) }
    var language by rememberSaveable { mutableStateOf("id") }
    var cv by rememberSaveable { mutableStateOf("") }
    var endpoint by rememberSaveable { mutableStateOf("") }
    var started by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<InterviewTurn>()) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var report by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun send(action: String, turns: List<InterviewTurn>) {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            try {
                val response = requestInterview(endpoint.trim(), role, language, cv, action, turns)
                history = turns
                feedback = buildString {
                    if (response.score != null) append("Nilai jawaban: ${response.score}/100\\n")
                    append(response.feedback)
                    if (response.improvement.isNotBlank()) append("\\nSaran: ${response.improvement}")
                }
                if (action == "finish") {
                    report = response.finalReport
                } else {
                    question = response.question
                    started = true
                    answer = ""
                }
            } catch (ex: Exception) {
                error = ex.message ?: "Gagal menghubungi AI Interview."
            } finally {
                loading = false
            }
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Interview", style = MaterialTheme.typography.headlineSmall)
        Text("Wawancara adaptif dengan AI. Membutuhkan server AI Interview aktif.")
        if (!started) {
            Text("Pilih posisi")
            roles.forEach { item ->
                Row {
                    RadioButton(selected = role == item, onClick = { role = item })
                    Text(item, Modifier.padding(top = 12.dp))
                }
            }
            Row {
                RadioButton(selected = language == "id", onClick = { language = "id" })
                Text("Indonesia", Modifier.padding(top = 12.dp))
                Spacer(Modifier.width(12.dp))
                RadioButton(selected = language == "en", onClick = { language = "en" })
                Text("English", Modifier.padding(top = 12.dp))
            }
            OutlinedTextField(cv, { cv = it }, label = { Text("Ringkasan CV / pengalaman (opsional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("Alamat API AI Interview (sementara untuk pengujian)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(enabled = !loading && endpoint.isNotBlank(), onClick = {
                history = emptyList()
                report = ""
                feedback = ""
                send("start", emptyList())
            }) { Text("Mulai wawancara AI") }
        } else {
            if (report.isNotBlank()) {
                Text("Hasil evaluasi", style = MaterialTheme.typography.titleLarge)
                Text(report)
                Button(onClick = {
                    started = false
                    question = ""
                    history = emptyList()
                    report = ""
                    feedback = ""
                }) { Text("Mulai sesi baru") }
            } else {
                Text("Pertanyaan ${history.size + 1}", style = MaterialTheme.typography.titleMedium)
                Text(question)
                if (feedback.isNotBlank()) Text(feedback)
                OutlinedTextField(answer, { answer = it }, label = { Text("Jawaban Anda") },
                    modifier = Modifier.fillMaxWidth(), minLines = 4)
                Button(enabled = !loading && answer.isNotBlank(), onClick = {
                    send("reply", history + InterviewTurn(question, answer.trim()))
                }) { Text("Kirim jawaban") }
                OutlinedButton(enabled = !loading && history.isNotEmpty(), onClick = {
                    send("finish", history)
                }) { Text("Akhiri dan lihat evaluasi") }
            }
        }
        if (loading) CircularProgressIndicator()
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("Catatan: hasil AI adalah umpan balik latihan, bukan keputusan rekrutmen.",
            style = MaterialTheme.typography.bodySmall)
    }
}
