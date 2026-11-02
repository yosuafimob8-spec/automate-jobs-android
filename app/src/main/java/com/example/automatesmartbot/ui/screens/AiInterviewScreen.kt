package com.example.automatesmartbot.ui.screens

import com.example.automatesmartbot.BuildConfig
import com.example.automatesmartbot.data.security.SessionManager
import android.content.Intent
import android.net.Uri
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


private fun extractCv(context: android.content.Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri).orEmpty()
    val pdf = mime == "application/pdf"
    val docx = mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    require(pdf || docx) { "Pilih PDF atau DOCX." }
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            require(output.size() <= 10 * 1024 * 1024) { "CV maksimal 10 MB." }
        }
        output.toByteArray()
    } ?: error("CV tidak dapat dibuka.")
    require(bytes.size <= 10 * 1024 * 1024) { "CV maksimal 10 MB." }
    val text = if (pdf) {
        PDFBoxResourceLoader.init(context.applicationContext)
        PDDocument.load(bytes).use {
            require(it.numberOfPages <= 40) { "CV maksimal 40 halaman." }
            PDFTextStripper().getText(it)
        }
    } else {
        val output = StringBuilder()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.name == "t") output.append(parser.nextText())
                        else if (event == XmlPullParser.END_TAG && parser.name == "p") output.append(10.toChar())
                        require(output.length <= 100000) { "Isi CV terlalu panjang." }
                        event = parser.next()
                    }
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        output.toString()
    }
    val result = text.trim()
    require(result.length >= 30) { "Teks CV tidak terbaca. PDF hasil scan memerlukan OCR." }
    return result.take(6000)
}

private data class InterviewTurn(val question: String, val answer: String)
private data class InterviewReply(
    val question: String = "",
    val feedback: String = "",
    val score: Int? = null,
    val improvement: String = "",
    val finalReport: String = "",
    val overallScore: Int? = null,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val encouragement: String = "",
    val awaitingAnswer: Boolean = false,
    val conversationState: String = ""
)

private suspend fun requestInterview(
    endpoint: String, accessToken: String, role: String, language: String, cv: String,
    action: String, history: List<InterviewTurn>, interviewer: String
): InterviewReply = withContext(Dispatchers.IO) {
    require(endpoint.startsWith("https://") || endpoint.startsWith("http://10.0.2.2:") || endpoint.startsWith("http://127.0.0.1:")) {
        "Gunakan endpoint HTTPS untuk layanan AI Interview."
    }
    val payload = JSONObject().apply {
        put("role", role)
        put("interviewer", interviewer)
        put("language", language)
        put("cv_text", cv.take(1300))
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
        readTimeout = 130000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Authorization", "Bearer $accessToken")
    }
    try {
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) {
            val status = connection.responseCode
            val details = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val message = runCatching { JSONObject(details).optString("error") }.getOrDefault("")
            throw IllegalStateException(if (message.isNotBlank()) "AI Interview (HTTP $status): $message" else "Layanan AI Interview belum siap (HTTP $status).")
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
            finalReport = finalReport,
            overallScore = if (action == "finish") response.optInt("overall_score", 0) else null,
            strengths = if (action == "finish") (response.optJSONArray("strengths") ?: JSONArray()).let { a -> (0 until a.length()).map { a.optString(it) } } else emptyList(),
            improvements = if (action == "finish") (response.optJSONArray("improvements") ?: JSONArray()).let { a -> (0 until a.length()).map { a.optString(it) } } else emptyList(),
            encouragement = response.optString("encouragement"),
            awaitingAnswer = response.optBoolean("awaiting_answer", false),
            conversationState = response.optString("conversation_state")
        )
    } finally {
        connection.disconnect()
    }
}

@Composable
fun AiInterviewScreen(isPro: Boolean = false) {
    val roles = listOf("Admin Warehouse", "Supervisor Logistik", "Staff Operasional", "Operator Produksi", "Sales", "Lainnya")
    var role by rememberSaveable { mutableStateOf(roles.first()) }
    var interviewMode by rememberSaveable { mutableStateOf("text") }
    var interviewer by rememberSaveable { mutableStateOf("female") }
    var language by rememberSaveable { mutableStateOf("id") }
    var cv by rememberSaveable { mutableStateOf("") }
    var cvLoaded by rememberSaveable { mutableStateOf(false) }
    var cvFileName by rememberSaveable { mutableStateOf("") }
    var cvReading by remember { mutableStateOf(false) }
    // Service address is app-managed; never ask end users for provider credentials.
    val endpoint = BuildConfig.INTERVIEW_BASE_URL.takeIf { it.isNotBlank() }?.trimEnd('/')?.plus("/interview/turn")
    var started by rememberSaveable { mutableStateOf(false) }
    var question by rememberSaveable { mutableStateOf("") }
    var answer by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<InterviewTurn>()) }
    var chatMessages by remember { mutableStateOf(emptyList<Pair<Boolean, String>>()) }
    var pendingQuestion by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    var report by rememberSaveable { mutableStateOf("") }
    var resultScore by rememberSaveable { mutableIntStateOf(0) }
    var resultStrengths by remember { mutableStateOf(emptyList<String>()) }
    var resultImprovements by remember { mutableStateOf(emptyList<String>()) }
    var resultAdvice by rememberSaveable { mutableStateOf("") }
    var showResultDetails by rememberSaveable { mutableStateOf(false) }
    var showPreparation by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val session = remember(context) { SessionManager(context.applicationContext) }
    var ttsReady by remember { mutableStateOf(false) }
    val voiceMode = interviewMode == "voice"
    var pendingSpeech by remember { mutableStateOf<String?>(null) }
    val tts = remember { TextToSpeech(context) { status -> ttsReady = status == TextToSpeech.SUCCESS } }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                answer = spoken
                if (voiceMode) pendingSpeech = spoken
            }
            else error = "Suara tidak terdeteksi. Coba lagi."
        } else error = "Mikrofon dibatalkan atau tidak dapat mengenali suara."
    }
    val scope = rememberCoroutineScope()
    val cvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            cv = ""
            cvLoaded = false
            cvFileName = ""
            cvReading = true
            error = ""
            scope.launch {
                try {
                    cv = withContext(Dispatchers.IO) { extractCv(context, uri) }
                    cvLoaded = true
                    cvFileName = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: uri.lastPathSegment.orEmpty()
                } catch (ex: Exception) {
                    cvLoaded = false
                    error = ex.message ?: "Gagal membaca CV."
                } finally {
                    cvReading = false
                }
            }
        }
    }
    fun send(action: String, turns: List<InterviewTurn>) {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            try {
                val accessToken = session.getValidToken() ?: throw IllegalStateException("Silakan login kembali.")
                val serviceUrl = endpoint ?: throw IllegalStateException("Layanan AI Interview belum dikonfigurasi.")
                val response = requestInterview(serviceUrl, accessToken, role, language, cv, action, turns, interviewer)
                history = turns
                if (action == "start") {
                    report = ""
                    resultScore = 0
                    resultStrengths = emptyList()
                    resultImprovements = emptyList()
                    resultAdvice = ""
                    require(response.question.isNotBlank()) { "AI belum mengirim pertanyaan pembuka. Periksa layanan AI Interview." }
                    chatMessages = listOf(false to response.question)
                } else if (action == "reply") {
                    require(response.question.isNotBlank()) { "AI belum mengirim pertanyaan lanjutan. Coba ulangi jawaban." }
                    val latest = turns.lastOrNull()?.answer.orEmpty()
                    chatMessages = chatMessages + (true to latest) + (false to (response.feedback.takeIf { it.isNotBlank() }?.plus("\\n\\n") ?: "") + response.question)
                }
                feedback = buildString {
                    if (response.score != null) append("Nilai jawaban: ${response.score}/100\\n")
                    append(response.feedback)
                    if (response.improvement.isNotBlank()) append("\\nSaran: ${response.improvement}")
                }
                if (action == "finish") {
                    report = response.finalReport
                    resultScore = response.overallScore ?: 0
                    resultStrengths = response.strengths
                    resultImprovements = response.improvements
                    resultAdvice = response.encouragement
                } else {
                    question = response.question
                    pendingQuestion = if (response.awaitingAnswer) response.question else ""
                    if (voiceMode && ttsReady) {
                        tts.language = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("id-ID")
                        val speechStatus = tts.speak((response.feedback.takeIf { it.isNotBlank() }?.plus(". ") ?: "") + question, TextToSpeech.QUEUE_FLUSH, null, "interview-question")
                        if (speechStatus == TextToSpeech.ERROR) error = "Pembaca suara gagal. Pertanyaan tetap tersedia sebagai teks."
                    }
                    started = true
                    answer = ""
                }
            } catch (ex: Exception) {
                error = when (ex) {
                    is java.net.ConnectException -> "Server AI Interview tidak aktif. Jalankan layanan pada port 8790 di Termux."
                    is java.net.SocketTimeoutException -> "Server AI Interview tidak merespons. Periksa layanan dan koneksi model AI."
                    else -> ex.message ?: "Gagal menghubungi AI Interview."
                }
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(pendingSpeech, loading, started) {
        val speech = pendingSpeech
        if (speech != null && !loading && started && question.isNotBlank()) {
            pendingSpeech = null
            send("reply", history + InterviewTurn(question, speech.trim()))
        }
    }
    val orange = Color(0xFFFF712E)
    val teal = Color(0xFF10B8BF)
    val ink = Color(0xFF14284B)
    val muted = Color(0xFF607085)
    val shape = RoundedCornerShape(22.dp)
    fun resetSession() {
        started = false
        question = ""
        answer = ""
        history = emptyList()
        chatMessages = emptyList()
        pendingQuestion = ""
        report = ""
        feedback = ""
        showResultDetails = false
    }
    @Composable
    fun InfoCard(title: String, body: String, accent: Color = Color.White) {
        Card(modifier = Modifier.fillMaxWidth(), shape = shape,
            colors = CardDefaults.cardColors(containerColor = accent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = ink, style = MaterialTheme.typography.titleMedium)
                Text(body, color = muted)
            }
        }
    }
    if (!started && !showPreparation) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFC)).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("◧ KarirMate", color = orange, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                Text("✦ AI HR", color = teal, fontWeight = FontWeight.Bold)
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E3)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)) {
                Column(Modifier.fillMaxWidth().padding(26.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🤖  AI Interview", color = ink, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
                    Text("Latihan wawancara kerja dengan AI HR yang cerdas, realistis, dan sesuai posisi impianmu.", color = ink)
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Card(Modifier.fillMaxWidth().clickable { interviewMode = "text"; showPreparation = true },
                    shape = shape, border = BorderStroke(2.dp, orange),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Interview Teks", color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Wawancara interaktif via chat dengan AI HR", color = muted)
                        }
                        Text("›", color = orange, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Card(Modifier.fillMaxWidth().clickable { interviewMode = "voice"; showPreparation = true },
                    shape = shape, border = BorderStroke(2.dp, teal), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Interview Suara", color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text("Wawancara dengan pembaca suara dan mikrofon", color = muted)
                        }
                    }
                }

            }
        }
    } else if (!started) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFC)).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("←  Pilih Interviewer", modifier = Modifier.clickable { showPreparation = false }, color = ink,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text("Pilih AI HR yang akan mewawancarai kamu. Keduanya profesional, ramah, dan siap memberikan pengalaman wawancara yang realistis.", color = ink)
            listOf(Triple("male", "👨🏻‍💼  Mr. ILHAM", "AI HR laki-laki • Profesional, ramah, dan komunikatif."),
                Triple("female", "👩🏻‍💼  Ms. Tanti", "AI HR perempuan • Hangat, suportif, dan detail.")).forEach { (id, name, description) ->
                Card(Modifier.fillMaxWidth().clickable { interviewer = id }, shape = shape,
                    border = BorderStroke(if (interviewer == id) 2.dp else 1.dp, if (interviewer == id) orange else Color(0xFFE4E8EC)),
                    colors = CardDefaults.cardColors(containerColor = if (interviewer == id) Color(0xFFFFF4E9) else Color.White)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(description, color = muted)
                        }
                        RadioButton(selected = interviewer == id, onClick = { interviewer = id })
                    }
                }
            }
            Text("Posisi yang dilamar", color = ink, fontWeight = FontWeight.Bold)
            roles.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = role == item, onClick = { role = item })
                    Text(item, Modifier.clickable { role = item }, color = ink)
                }
            }
            Text("Bahasa", color = ink, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = language == "id", onClick = { language = "id" }, label = { Text("Indonesia") })
                FilterChip(selected = language == "en", onClick = { language = "en" }, label = { Text("English") })
            }
            OutlinedButton(enabled = !cvReading && !loading, onClick = {
                cvPicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (cvReading) "Membaca CV..." else "📄 Unggah CV PDF / DOCX (opsional)") }
            if (cvLoaded) {
                Text("✓ CV: $cvFileName", color = ink, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { cvPicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) { Text("Ganti CV") }
                    OutlinedButton(onClick = { cv = ""; cvLoaded = false; cvFileName = "" }) { Text("Hapus CV") }
                }
            }
            Text("CV maksimal 10 MB; PDF scan memerlukan OCR.", color = muted, style = MaterialTheme.typography.bodySmall)
            if (voiceMode) Text("Mode suara: AI membacakan pertanyaan. Jawab dengan mikrofon atau ketik.", color = ink)
            Button(enabled = !loading && !cvReading && endpoint != null, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = orange), shape = RoundedCornerShape(16.dp),
                onClick = {
                    history = emptyList()
                    chatMessages = emptyList()
                    pendingQuestion = ""
                    report = ""
                    feedback = ""
                    send("start", emptyList())
                }) { Text(if (voiceMode) "Mulai Interview Suara  ➜" else "Mulai Interview Teks  ➜", fontWeight = FontWeight.Bold) }
            if (endpoint == null) Text("Layanan AI Interview belum tersedia.", color = MaterialTheme.colorScheme.error)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    } else if (report.isNotBlank()) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFC)).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("←  Hasil Interview", modifier = Modifier.clickable { resetSession() }, color = ink,
                fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Card(shape = shape, colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0E3))) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏅", style = MaterialTheme.typography.displayMedium)
                    Text("Skor Akhir", color = ink, fontWeight = FontWeight.Bold)
                    Text("$resultScore / 100", color = orange, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.displayMedium)
                    Text("Hasil latihan wawancara", color = muted)
                }
            }
            InfoCard("📋 Kekuatan", resultStrengths.joinToString("\n• ", prefix = "• ").ifBlank { "Belum ada catatan." })
            InfoCard("🎯 Area Pengembangan", resultImprovements.joinToString("\n• ", prefix = "• ").ifBlank { "Belum ada catatan." })
            InfoCard("💡 Saran", resultAdvice.ifBlank { "Terus berlatih dengan jawaban yang spesifik dan terukur." })
            if (showResultDetails) InfoCard("📑 Pembahasan Lengkap", report)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { resetSession(); showPreparation = true }, modifier = Modifier.weight(1f)) { Text("↻ Ulangi") }
                Button(onClick = { showResultDetails = !showResultDetails }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text(if (showResultDetails) "Tutup Detail" else "Lihat Detail") }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Color(0xFFF8FBFC))) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", modifier = Modifier.clickable { started = false; showPreparation = true; tts.stop() }, color = ink, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Text(if (interviewer == "male") "👨🏻‍💼" else "👩🏻‍💼", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (interviewer == "male") "Mr. ILHAM" else "Ms. Tanti", color = ink, fontWeight = FontWeight.Bold)
                    Text("🟢 AI HR KarirMate • Interview berlangsung", color = muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(chatMessages) { (fromCandidate, message) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromCandidate) Arrangement.End else Arrangement.Start) {
                        Card(modifier = Modifier.fillMaxWidth(0.84f),
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp,
                                bottomStart = if (fromCandidate) 18.dp else 4.dp,
                                bottomEnd = if (fromCandidate) 4.dp else 18.dp),
                            colors = CardDefaults.cardColors(containerColor = if (fromCandidate) Color(0xFF009E9A) else Color(0xFFF0F2F5))) {
                            Text(message.replace("\\n", "\n"), modifier = Modifier.padding(14.dp),
                                color = if (fromCandidate) Color.White else ink)
                        }
                    }
                }
                if (loading) item { CircularProgressIndicator(color = teal) }
            }
            if (error.isNotBlank()) Text(error, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth().background(Color.White).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(answer, { answer = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Ketik jawaban Anda...", color = muted) }, maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(color = ink),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink, cursorColor = orange, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    shape = RoundedCornerShape(24.dp))
                if (voiceMode) IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (language == "en") "en-US" else "id-ID")
                    }
                    try { voiceLauncher.launch(intent) } catch (_: Exception) { error = "Pengenalan suara tidak tersedia." }
                }) { Text("🎙") }
                if (voiceMode) TextButton(onClick = {
                    if (ttsReady && question.isNotBlank()) {
                        tts.language = if (language == "en") Locale.ENGLISH else Locale.forLanguageTag("id-ID")
                        tts.speak(question, TextToSpeech.QUEUE_FLUSH, null, "interview-repeat")
                    } else error = "Mesin pembaca suara belum siap di perangkat."
                }) { Text("🔊") }
                Button(enabled = !loading && answer.isNotBlank(), onClick = {
                    send("reply", history + InterviewTurn(question, answer.trim()))
                }, colors = ButtonDefaults.buttonColors(containerColor = orange),
                    contentPadding = PaddingValues(12.dp)) { Text("➤") }
            }
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = !loading, onClick = {
                    send("reply", history + InterviewTurn(question, "lewati"))
                }) { Text("Lewati", color = teal) }
                TextButton(enabled = !loading, onClick = {
                    send("finish", history + (if (answer.isNotBlank()) listOf(InterviewTurn(question, answer.trim())) else emptyList()))
                }) { Text("Selesai & Evaluasi", color = orange) }
            }
        }
    }
}
