package com.example.automatesmartbot.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

private val InterviewOrange = Color(0xFFFF792E)
private val InterviewNavy = Color(0xFF172943)
private val InterviewPositions = listOf("Admin Warehouse", "Supervisor Logistik", "Staff Operasional", "Operator Produksi", "Sales", "Lainnya")

private fun makeInterviewQuestions(role: String, english: Boolean): List<String> =
    if (english) listOf(
        "Please introduce yourself and describe your relevant work experience.",
        "Why are you interested in the $role position?",
        "What skills make you suitable for the $role role?",
        "Tell me about a workplace problem you solved.",
        "How would you prioritize your work as $role?"
    ) else listOf(
        "Perkenalkan diri dan pengalaman kerja yang relevan.",
        "Mengapa Anda tertarik melamar sebagai $role?",
        "Keterampilan apa yang membuat Anda cocok sebagai $role?",
        "Ceritakan masalah di tempat kerja yang berhasil Anda selesaikan.",
        "Bagaimana Anda menentukan prioritas pekerjaan sebagai $role?"
    )

@Composable
fun AiInterviewScreen() {
    val context = LocalContext.current
    var page by remember { mutableStateOf("menu") }
    var mode by remember { mutableStateOf("text") }
    var position by remember { mutableStateOf(InterviewPositions.first()) }
    var customPosition by remember { mutableStateOf("") }
    var english by remember { mutableStateOf(false) }
    var cvUri by remember { mutableStateOf<Uri?>(null) }
    var cvName by remember { mutableStateOf<String?>(null) }
    var answer by remember { mutableStateOf("") }
    var questionIndex by remember { mutableIntStateOf(0) }
    val responses = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val role = if (position == "Lainnya") customPosition.trim() else position
    val questions = remember(role, english) { makeInterviewQuestions(role.ifBlank { "posisi ini" }, english) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            cvUri = uri
            cvName = name ?: uri.lastPathSegment ?: "CV.pdf"
        }
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!recognized.isNullOrBlank()) answer = recognized
            else error = "Suara tidak dikenali. Coba lagi atau ketik jawaban."
        } else {
            error = "Perekaman dibatalkan. Kamu tetap bisa mengetik jawaban."
        }
    }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (status != TextToSpeech.SUCCESS) error = "Pembaca suara tidak tersedia di perangkat."
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
            ttsReady = false
        }
    }
    fun back() {
        tts?.stop()
        page = when (page) {
            "finished", "session" -> "setup"
            "setup" -> "menu"
            else -> "menu"
        }
    }
    BackHandler(enabled = page != "menu") { back() }
    fun speak() {
        val engine = tts
        if (engine == null || !ttsReady) {
            error = "Pembaca suara belum siap. Coba lagi sebentar."
            return
        }
        val available = engine.setLanguage(if (english) Locale.US else Locale("id", "ID"))
        if (available < TextToSpeech.LANG_AVAILABLE) {
            error = "Paket suara bahasa yang dipilih belum tersedia."
            return
        }
        if (engine.speak(questions[questionIndex], TextToSpeech.QUEUE_FLUSH, null, "interview-question") == TextToSpeech.ERROR)
            error = "Gagal membacakan pertanyaan."
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (page != "menu") TextButton(onClick = { back() }) { Text("← Kembali", color = InterviewNavy) }
        error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { error = null }) { Text("Tutup pesan") }
        }
        when (page) {
            "menu" -> {
                Text("AI Interview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = InterviewNavy)
                Text("Pilih jenis latihan wawancara kerja.")
                InterviewOption("Interview Teks", "Jawab pertanyaan melalui teks.") { mode = "text"; page = "setup" }
                InterviewOption("Interview Suara", "Dengarkan pertanyaan dan jawab menggunakan mikrofon.") { mode = "voice"; page = "setup" }
                Text("Interview Video: belum tersedia", color = Color.Gray)
            }
            "setup" -> {
                Text(if (mode == "voice") "Interview Suara" else "Interview Teks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = InterviewNavy)
                Text("Posisi yang dilamar", fontWeight = FontWeight.Bold)
                InterviewPositions.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable { position = item }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = position == item, onClick = { position = item })
                        Text(item)
                    }
                }
                if (position == "Lainnya") OutlinedTextField(customPosition, { customPosition = it }, label = { Text("Nama posisi") }, modifier = Modifier.fillMaxWidth())
                Text("Bahasa", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = !english, onClick = { english = false }, label = { Text("Indonesia") })
                    FilterChip(selected = english, onClick = { english = true }, label = { Text("English") })
                }
                Text("CV (opsional)", fontWeight = FontWeight.Bold)
                if (cvUri == null) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) { Text("Pilih CV PDF") }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2E9)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(cvName ?: "CV dipilih", color = InterviewNavy, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Ganti CV") }
                                OutlinedButton(onClick = { cvUri = null; cvName = null }) { Text("Hapus CV") }
                            }
                        }
                    }
                }
                Text("CV hanya dipilih sebagai referensi pengguna; isi CV belum dianalisis otomatis.", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        if (role.isBlank()) error = "Isi nama posisi terlebih dahulu."
                        else {
                            error = null
                            responses.clear()
                            questionIndex = 0
                            answer = ""
                            page = "session"
                        }
                    },
                    enabled = role.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = InterviewOrange)
                ) { Text(if (mode == "voice") "Mulai Interview Suara" else "Mulai Interview Teks") }
                if (role.isBlank()) Text("Isi nama posisi agar sesi bisa dimulai.", color = MaterialTheme.colorScheme.error)
            }
            "session" -> {
                Text(if (mode == "voice") "Interview Suara" else "Interview Teks", style = MaterialTheme.typography.headlineMedium, color = InterviewNavy)
                Text("Pertanyaan ${questionIndex + 1} dari ${questions.size} • $role")
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2E9)), modifier = Modifier.fillMaxWidth()) {
                    Text(questions[questionIndex], modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.titleLarge)
                }
                if (mode == "voice") {
                    OutlinedButton(onClick = { speak() }) { Text("Dengarkan Pertanyaan") }
                    OutlinedButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (english) "en-US" else "id-ID")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Jawab pertanyaan interview")
                        }
                        runCatching { speech.launch(intent) }.onFailure {
                            error = "Layanan pengenalan suara tidak tersedia. Gunakan jawaban teks."
                        }
                    }) { Text("Jawab dengan Mikrofon") }
                }
                OutlinedTextField(answer, { answer = it }, label = { Text("Jawaban") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (answer.isNotBlank()) {
                            responses.add(answer.trim())
                            answer = ""
                            tts?.stop()
                            if (questionIndex == questions.lastIndex) page = "finished"
                            else questionIndex++
                        }
                    },
                    enabled = answer.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = InterviewOrange)
                ) { Text(if (questionIndex == questions.lastIndex) "Selesaikan Interview" else "Simpan Jawaban & Lanjut") }
            }
            "finished" -> {
                Text("Latihan Selesai", style = MaterialTheme.typography.headlineMedium, color = InterviewNavy)
                Text("${responses.size} jawaban tercatat dalam sesi ini.")
                responses.forEachIndexed { index, response ->
                    Text("${index + 1}. ${questions[index]}", fontWeight = FontWeight.Bold)
                    Text(response)
                    HorizontalDivider()
                }
                Text("Ini latihan lokal. Belum ada analisis CV, pertanyaan dinamis AI, atau penilaian AI.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { page = "setup" }, colors = ButtonDefaults.buttonColors(containerColor = InterviewOrange)) { Text("Latihan Lagi") }
            }
        }
    }
}

@Composable
private fun InterviewOption(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, InterviewOrange),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = InterviewNavy)
            Text(description, color = Color.DarkGray)
        }
    }
}
