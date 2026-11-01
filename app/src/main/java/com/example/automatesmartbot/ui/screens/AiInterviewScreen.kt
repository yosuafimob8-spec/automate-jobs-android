package com.example.automatesmartbot.ui.screens

import android.os.Build
import android.speech.SpeechRecognizer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.data.autoapply.*
import com.example.automatesmartbot.data.cv.CvContent
import com.example.automatesmartbot.data.cv.hasUserText
import com.example.automatesmartbot.ui.components.*
import com.example.automatesmartbot.ui.components.interview.*
import com.example.automatesmartbot.ui.viewmodel.InterviewViewModel
import org.json.JSONObject

@Composable
fun AiInterviewScreen(isPro: Boolean = false, cv: CvContent = CvContent(), vm: InterviewViewModel = viewModel()) {
    val capabilities by vm.capabilities.collectAsState(); val session by vm.session.collectAsState(); val history by vm.history.collectAsState(); val error by vm.error.collectAsState(); val busy by vm.busy.collectAsState(); val answer by vm.answer.collectAsState(); val rtc by vm.webRtcAnswer.collectAsState()
    var language by rememberSaveable { mutableStateOf("id") }; var position by rememberSaveable { mutableStateOf(cv.contact.headline) }; var difficulty by rememberSaveable { mutableStateOf("beginner") }; var interviewer by rememberSaveable { mutableStateOf("hr") }; var mode by rememberSaveable { mutableStateOf("text") }; var backendCv by rememberSaveable { mutableStateOf(!cv.hasUserText()) }; var showHistory by rememberSaveable { mutableStateOf(false) }; var confirmFinish by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localAsr = remember { Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
    val serverPro = isPro && capabilities?.optBoolean("deep_analysis") == true
    val voiceAvailable = serverPro && (localAsr || capabilities?.optBoolean("stt_available") == true)
    val video = capabilities?.optJSONObject("video")
    val videoAvailable = serverPro && video?.optBoolean("available") == true
    LaunchedEffect(isPro) { vm.load() }
    if (confirmFinish) AlertDialog(onDismissRequest = { confirmFinish = false }, title = { Text("Selesaikan sesi sekarang?") }, text = { Text("Jawaban yang sudah dikirim tetap tersimpan. Penilaian menggunakan jawaban yang tersedia.") }, confirmButton = { TextButton(onClick = { confirmFinish = false; vm.finish() }) { Text("Selesaikan") } }, dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Lanjutkan") } })
    Column(Modifier.fillMaxSize().imePadding()) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Card(Modifier.fillMaxWidth().padding(12.dp)) { Column(Modifier.padding(12.dp)) { Text(it, color = MaterialTheme.colorScheme.error); Text("Jawaban yang diketik tetap disimpan. Sinkronkan sesi sebelum mencoba ulang jika respons terputus."); TextButton(onClick = { vm.sync() }, enabled = !busy) { Text("Sinkronkan Sesi") } } } }
        if (showHistory) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { JourneyHeader("Riwayat Interview", "Hasil berasal dari sesi yang tersimpan pada akun Anda."); TextButton(onClick = { showHistory = false }) { Text("Kembali ke Interview") } }
                history?.optJSONObject("progress")?.let { p -> item { Card { Column(Modifier.padding(16.dp)) { Text("Perkembangan · PRO", style = MaterialTheme.typography.titleMedium); Text("${p.optInt("sessions")} sesi · nilai terakhir ${p.optInt("latest")} · perubahan ${p.optInt("change")}"); Text(p.text("basis")) } } } }
                val records = history?.objects("items").orEmpty()
                if (records.isEmpty() && !busy) item { Text("Belum ada riwayat atau data belum berhasil dimuat."); OutlinedButton(onClick = { vm.loadHistory() }, enabled = !busy) { Text("Muat Ulang Riwayat") } }
                items(records, key = { it.text("id") }) { r -> Card(onClick = { vm.openHistory(r.text("id")); showHistory = false }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text(r.optJSONObject("settings")?.text("position").orEmpty(), style = MaterialTheme.typography.titleMedium); Text(r.text("created_at")); Text(r.text("status")); r.optJSONObject("result")?.let { Text("Nilai latihan: ${it.getInt("overall")}/100") }
                } } }
            }
        } else if (session == null) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { JourneyHeader("AI Interview", "${if (serverPro) "PRO" else "FREE / menunggu verifikasi"} · Indonesia / English · berbasis CV") }
                item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (capabilities?.text("question_engine") == "ollama") "Model pertanyaan self-hosted dikonfigurasi. Status provider aktual ditampilkan pada setiap sesi." else "Model AI self-hosted belum terhubung. Interview teks memakai mesin pertanyaan dan evaluasi terstruktur yang berfungsi tanpa GPU.")
                    OutlinedButton(onClick = { vm.loadHistory(); showHistory = true }, enabled = !busy) { Text("Riwayat & Perkembangan") }
                    InterviewChoice("Bahasa", listOf("id" to "Indonesia", "en" to "English"), language, { language = it })
                    AppInput("Posisi pekerjaan *", position, { position = it })
                    InterviewChoice("Tingkat kesulitan", listOf("beginner" to "Pemula", "intermediate" to "Menengah", "professional" to "Profesional"), difficulty, { difficulty = it })
                    InterviewChoice("Pewawancara", listOf("hr" to "HRD", "user" to "User", "manager" to "Manajer"), interviewer, { interviewer = it })
                    InterviewChoice("Mode", listOf("text" to "Teks · FREE", "voice" to "Suara · PRO", "video" to "Video Avatar · PRO"), mode, { mode = it })
                    Text("FREE: interview teks dan penilaian dasar. PRO: pertanyaan adaptif, analisis mendalam, rekomendasi, suara/video bila layanan tersedia, serta perkembangan hasil.")
                    if (!voiceAvailable) Text("Suara belum tersedia: perlu PRO terverifikasi dan pengenalan suara offline atau STT self-hosted.")
                    if (!videoAvailable) Text("Video belum tersedia: ${video?.text("reason")?.ifBlank { "GPU worker dan avatar berizin belum terhubung" } ?: "status backend belum dimuat"}. Interview teks tetap tersedia.")
                    Text("Sumber CV", style = MaterialTheme.typography.titleMedium)
                    Row { RadioButton(!backendCv, { backendCv = false }, enabled = cv.hasUserText()); Text("CV aktif KarirMate${cv.contact.fullName.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", Modifier.padding(top = 12.dp)) }
                    Row { RadioButton(backendCv, { backendCv = true }); Text("CV PDF yang tersimpan di backend", Modifier.padding(top = 12.dp)) }
                    if (!cv.hasUserText()) Text("Belum ada data CV aktif. Pilih CV backend atau isi CV Builder terlebih dahulu.")
                    Button(onClick = {
                        val settings = mutableMapOf<String, Any>("language" to language, "position" to position, "difficulty" to difficulty, "interviewer" to interviewer, "mode" to mode, "use_backend_cv" to backendCv)
                        if (!backendCv) settings["cv"] = cv
                        vm.start(settings)
                    }, enabled = !busy && capabilities != null && position.trim().length >= 2 && (mode == "text" || mode == "voice" && voiceAvailable || mode == "video" && videoAvailable), modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Mulai Interview") }
                    TextButton(onClick = { vm.load() }, enabled = !busy) { Text("Periksa Ketersediaan Layanan") }
                } }
            }
        } else {
            val current = session!!; val settings = current.getJSONObject("settings"); val result = current.optJSONObject("result"); val completed = current.text("status") == "completed"
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { JourneyHeader(if (completed) "Hasil Interview" else "Pertanyaan ${current.getInt("question_number")}/${current.getInt("question_limit")}", "${settings.text("position")} · ${settings.text("language").uppercase()} · ${settings.text("mode")}", if (completed) 1f else current.getInt("question_number").toFloat()/current.getInt("question_limit")) }
                item { Text("Provider pertanyaan: ${current.text("provider")}"); if (current.text("notice").isNotBlank()) Text(current.text("notice")) }
                if (!completed) {
                    item { Card { Text(current.getString("question"), Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge) } }
                    if (settings.text("mode") == "video" && serverPro) item { AvatarInterviewView(current.getString("question"), rtc?.text("sdp"), video?.optJSONArray("ice_servers"), vm::offer, vm::showError, Modifier.fillMaxWidth().height(360.dp)) }
                    if (settings.text("mode") in listOf("voice", "video") && serverPro) item { InterviewVoiceControls(vm, current.getString("question"), settings.text("language"), capabilities?.optBoolean("stt_available") == true, capabilities?.optBoolean("tts_available") == true, busy, autoSpeak = settings.text("mode") == "voice") }
                    item { AppInput("Jawaban Anda — periksa sebelum dikirim", answer, vm::updateAnswer, minLines = 6) }
                    item { TextButton(onClick = { confirmFinish = true }, enabled = !busy && current.objects("answers").isNotEmpty()) { Text("Selesaikan Sesi Lebih Awal") } }
                } else if (result != null) {
                    item { InterviewResult(result, serverPro) }
                    item { OutlinedButton(onClick = { vm.loadHistory(); showHistory = true }) { Text("Lihat Riwayat & Perkembangan") }; Button(onClick = vm::newSetup) { Text("Interview Baru") } }
                }
                item { Text("Jawaban tersimpan", style = MaterialTheme.typography.titleMedium) }
                items(current.objects("answers")) { turn -> Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(turn.text("question"), style = MaterialTheme.typography.titleMedium); Text(turn.text("answer")) } } }
            }
            if (!completed) Surface(shadowElevation = 6.dp) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Button(onClick = { vm.submit() }, enabled = !busy && answer.trim().length >= 3, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text(if (busy) "Memproses..." else "Kirim Jawaban") } } }
        }
    }
}

@Composable
private fun InterviewChoice(label: String, choices: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { choices.forEach { (id, title) -> FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(title) }) } }
}

@Composable
internal fun InterviewResult(result: JSONObject, pro: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${result.getInt("overall")}/100", style = MaterialTheme.typography.headlineLarge)
        listOf("quality" to "Kualitas jawaban", "relevance" to "Relevansi posisi", "communication" to "Komunikasi teks").forEach { (key, label) -> Text("$label: ${result.getInt(key)}/100"); LinearProgressIndicator(progress = { (result.getInt(key)/100f).coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth()) }
        Text("Metode: ${result.text("evaluation_method")}"); Text(result.text("note"))
        if (result.text("evaluation_notice").isNotBlank()) Text(result.text("evaluation_notice"))
        listOf("strengths" to "Kelebihan", "weaknesses" to "Perlu ditingkatkan", "recommendations" to "Latihan berikutnya").forEach { (key,label) -> Text(label, style = MaterialTheme.typography.titleMedium); result.optJSONArray(key)?.let { a -> repeat(a.length()) { Text("• ${a.getString(it)}") } } }
        result.optJSONObject("english_feedback")?.let { en -> Text("English — ${en.text("basis")}"); Text(en.text("recommendation")); en.optJSONArray("patterns_to_review")?.let { if (it.length()>0) Text("Periksa pola: $it") } }
        if (pro) { Text("Analisis Mendalam · PRO", style = MaterialTheme.typography.titleMedium)
            result.optJSONArray("deep_analysis")?.let { turns -> repeat(turns.length()) { index ->
                val turn = turns.getJSONObject(index)
                Text("Jawaban ${index + 1}: kualitas ${turn.optInt("quality")}, relevansi ${turn.optInt("relevance")}, komunikasi ${turn.optInt("communication")}")
                if (turn.text("feedback").isNotBlank()) Text(turn.text("feedback"))
                turn.optJSONArray("evidence")?.let { evidence -> repeat(evidence.length()) { Text("• ${evidence.getString(it)}") } }
            } }
            ; if (result.text("improved_answer").isNotBlank()) Text(result.text("improved_answer")); result.optJSONArray("practice_plan")?.let { a -> repeat(a.length()) { Text("• ${a.getString(it)}") } } }
        else Text("Analisis mendalam dan perkembangan tersedia untuk PRO terverifikasi.")
    }
}
