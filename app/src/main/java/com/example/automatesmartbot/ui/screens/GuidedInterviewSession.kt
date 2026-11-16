package com.example.automatesmartbot.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.automatesmartbot.R
import com.example.automatesmartbot.data.interview.GuidedTts
import com.example.automatesmartbot.data.interview.StarFeedback
import com.example.automatesmartbot.data.interview.cleanRepeats
import com.example.automatesmartbot.data.interview.mergeFinalResults
import com.example.automatesmartbot.data.interview.scoreStarAnswer
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Satu soal dalam sesi terpandu: pertanyaan + respons HRD setelah jawaban. */
data class GuidedItem(val title: String, val question: String, val response: String)

data class GuidedAnswer(
    val title: String,
    val question: String,
    val answer: String,
    val feedback: StarFeedback,
)

private val stageDark = Color(0xFF0B2231)
private val stageCard = Color(0xFF123240)

/** Self-view kamera (CameraX, kamera depan). Fallback ke placeholder bila izin ditolak / gagal. */
@Composable
private fun CameraSelfView(modifier: Modifier, cameraEnabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraError by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    DisposableEffect(Unit) {
        onDispose { runCatching { cameraProvider?.unbindAll() } }
    }
    if (!cameraEnabled || cameraError) {
        Box(
            modifier = modifier.background(Color(0xFF1E3A4A), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("📷", fontSize = 22.sp)
        }
        return
    }
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            runCatching {
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    runCatching {
                        val provider = future.get()
                        cameraProvider = provider
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                        )
                    }.onFailure { cameraError = true }
                }, ContextCompat.getMainExecutor(ctx))
            }.onFailure { cameraError = true }
            previewView
        },
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    )
}

private enum class SessionPhase { ASK, RESPONSE, CLOSING, CANDIDATE, RESULT }

/**
 * Sesi video-call Latihan Terpandu.
 * Alur: soal dibacakan persis -> jawaban (suara/ketik) -> respons HRD dibacakan
 * -> LANGSUNG soal berikutnya -> setelah soal terakhir: penutup -> pertanyaan
 * kandidat -> hasil (feedback STAR per soal).
 */
@Composable
fun GuidedInterviewSession(
    items: List<GuidedItem>,
    closingText: String?,
    candidateName: String?,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var index by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf(SessionPhase.ASK) }
    var chat by remember { mutableStateOf(emptyList<Pair<Boolean, String>>()) }
    var answerText by remember { mutableStateOf("") }
    var answers by remember { mutableStateOf(emptyList<GuidedAnswer>()) }
    var listening by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var candidateQuestion by remember { mutableStateOf("") }
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var camGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var micOn by remember { mutableStateOf(true) }
    var camOn by remember { mutableStateOf(true) }
    var advanceToken by remember { mutableStateOf(0) }

    val tts = remember { GuidedTts(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        micGranted = result[Manifest.permission.RECORD_AUDIO] == true
        camGranted = result[Manifest.permission.CAMERA] == true
        if (result[Manifest.permission.RECORD_AUDIO] != true) {
            error = "Izin mikrofon belum diberikan. Jawaban suara tidak aktif — Anda tetap bisa mengetik jawaban."
        }
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        )
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        listening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // HANYA hasil final yang dipakai; tidak ada interim yang di-append.
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                answerText = mergeFinalResults(answerText, spoken)
            } else {
                error = "Suara tidak terdeteksi. Coba lagi atau ketik jawaban Anda."
            }
        } else {
            error = "Pengenalan suara dibatalkan. Coba lagi atau ketik jawaban Anda."
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }
        speaking = true
        tts.speak(text) {
            speaking = false
            onDone?.invoke()
        }
    }

    fun goNext() {
        tts.stop()
        speaking = false
        answerText = ""
        if (index + 1 < items.size) {
            index += 1
            phase = SessionPhase.ASK
        } else {
            phase = if (closingText.isNullOrBlank()) SessionPhase.RESULT else SessionPhase.CLOSING
        }
    }

    fun advanceAfterResponse() {
        // Penjaga token: onDone TTS dan timer fallback tidak boleh memajukan dua kali.
        val token = ++advanceToken
        scope.launch {
            // Fallback bila callback TTS tidak kembali (mis. suara belum terpasang).
            val words = items.getOrNull(index)?.response.orEmpty().split(Regex("\\s+")).size
            delay((words * 450L + 4_000L).coerceAtMost(25_000L))
            if (token == advanceToken && phase == SessionPhase.RESPONSE) {
                goNext()
            }
        }
    }

    fun submitAnswer() {
        val item = items.getOrNull(index) ?: return
        val cleaned = cleanRepeats(answerText.trim())
        val feedback = scoreStarAnswer(cleaned)
        answers = answers + GuidedAnswer(item.title, item.question, cleaned, feedback)
        chat = chat + (true to if (cleaned.isBlank()) "(dilewati)" else cleaned)
        if (item.response.isBlank()) {
            // Mode bank: tidak ada respons HRD baku — langsung soal berikutnya.
            goNext()
            return
        }
        phase = SessionPhase.RESPONSE
        // Tampilkan + bacakan respons HRD, lalu LANGSUNG lanjut (bukan "cek feedback").
        chat = chat + (false to item.response)
        speak(item.response) {
            if (phase == SessionPhase.RESPONSE) goNext()
        }
        advanceAfterResponse()
    }

    // Bacakan soal saat masuk fase ASK (sekali per soal).
    LaunchedEffect(index, phase) {
        if (phase == SessionPhase.ASK) {
            val item = items.getOrNull(index) ?: return@LaunchedEffect
            if (chat.none { !it.first && it.second == item.question }) {
                chat = chat + (false to item.question)
            }
            speak(item.question)
        }
        if (phase == SessionPhase.CLOSING && !closingText.isNullOrBlank()) {
            speak(closingText)
        }
    }

    val current = items.getOrNull(index)
    val totalScore = if (answers.isEmpty()) 0 else answers.sumOf { it.feedback.score } / answers.size
    val ink = KarirTextDark
    val muted = KarirTextMuted

    Column(Modifier.fillMaxSize().background(Color(0xFFF5FAFC))) {
        // ---- Panggung video-call ----
        Box(
            Modifier.fillMaxWidth().background(stageDark).padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "🔴 REC • SIMULASI LIVE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Text(
                        "Soal ${minOf(index + 1, items.size)} / ${items.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { i, _ ->
                        Box(
                            Modifier.weight(1f).height(5.dp)
                                .background(
                                    if (i < index || phase == SessionPhase.RESULT) Color(0xFF2DD4BF)
                                    else if (i == index) Color(0xFF2DD4BF).copy(alpha = 0.55f)
                                    else Color.White.copy(alpha = 0.25f),
                                    RoundedCornerShape(50),
                                ),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.ai_interview_hr_portrait),
                        contentDescription = "Bu Nadia, HR Recruiter",
                        modifier = Modifier.size(148.dp).clip(CircleShape)
                            .border(3.dp, Color(0xFF2DD4BF), CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    // Self-view kamera di pojok panggung.
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(width = 92.dp, height = 68.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(12.dp)),
                    ) {
                        CameraSelfView(
                            Modifier.fillMaxSize(),
                            cameraEnabled = camGranted && camOn,
                        )
                    }
                }
                Text(
                    "Bu Nadia",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (speaking) "HR Recruiter • Sedang berbicara…"
                    else if (listening) "HR Recruiter • Mendengarkan…"
                    else "HR Recruiter • Giliran Anda menjawab — bicara atau ketik jawaban Anda",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                // Bubble soal / respons terakhir.
                val bubble = chat.lastOrNull { !it.first }?.second.orEmpty()
                if (bubble.isNotBlank()) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(bubble, Modifier.padding(14.dp), color = ink, fontSize = 14.sp)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { micOn = !micOn },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text(if (micOn) "🎙 Mic: Nyala" else "🎙 Mic: Mati") }
                    OutlinedButton(
                        onClick = { camOn = !camOn },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text(if (camOn) "📷 Kamera: Nyala" else "📷 Kamera: Mati") }
                    Button(
                        onClick = { tts.stop(); speaking = false; onExit() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    ) { Text("Akhiri", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                if (!micGranted || !camGranted) {
                    Text(
                        "Izin kamera/mikrofon belum diberikan. Ketuk tombol di bawah untuk mengaktifkan — jawaban ketik tetap bisa dipakai.",
                        color = Color(0xFFFFD166),
                        fontSize = 12.sp,
                    )
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    ) { Text("Aktifkan Kamera & Mic") }
                }
            }
        }

        // ---- Isi bawah panggung ----
        when (phase) {
            SessionPhase.ASK, SessionPhase.RESPONSE -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "PERTANYAAN ${index + 1} • ${current?.title?.uppercase().orEmpty()}",
                        color = KarirOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                    )
                    Text(current?.question.orEmpty(), color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ketik jawaban Anda…", color = muted) },
                        label = { Text("Jawaban Anda", color = muted) },
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ink, unfocusedTextColor = ink, cursorColor = KarirOrange,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = phase == SessionPhase.ASK && !listening,
                            onClick = {
                                if (!micGranted) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                    error = "Berikan izin mikrofon terlebih dahulu."
                                    return@OutlinedButton
                                }
                                listening = true
                                error = ""
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Jawab pertanyaan interview")
                                }
                                runCatching { voiceLauncher.launch(intent) }
                                    .onFailure {
                                        listening = false
                                        error = "Pengenalan suara tidak tersedia di perangkat ini. Ketik jawaban Anda."
                                    }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (listening) "🎙 Mendengarkan…" else "🎙 Rekam Suara") }
                        OutlinedButton(
                            enabled = phase == SessionPhase.ASK,
                            onClick = { current?.let { speak(it.question) } },
                            modifier = Modifier.weight(1f),
                        ) { Text("🔊 Dengarkan Pertanyaan") }
                    }
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    if (phase == SessionPhase.ASK) {
                        Button(
                            onClick = { submitAnswer() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Kirim Jawaban ➜", color = Color.White, fontWeight = FontWeight.Bold) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = {
                                answerText = ""
                                submitAnswer()
                            }) { Text("Lewati Soal Ini", color = KarirOrange) }
                            TextButton(onClick = { phase = SessionPhase.RESULT }) { Text("Selesai & Lihat Hasil", color = KarirOrange) }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = KarirOrange, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Bu Nadia menjawab, lalu lanjut ke soal berikutnya…", color = ink, fontSize = 13.sp)
                        }
                        TextButton(onClick = { goNext() }) { Text("Lanjut ke Soal Berikutnya ➜", color = KarirOrange) }
                    }
                    Text(
                        "Penilaian otomatis (offline) memakai struktur STAR — Situasi, Tugas, Tindakan, Hasil — dan tampil di hasil akhir, bukan dibacakan sebagai pengganti soal.",
                        color = muted,
                        fontSize = 12.sp,
                    )
                }
            }
            SessionPhase.CLOSING -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("PENUTUP INTERVIEW", color = KarirOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Text(closingText.orEmpty(), Modifier.padding(16.dp), color = ink, fontSize = 14.sp)
                    }
                    OutlinedButton(onClick = { closingText?.let { speak(it) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("🔊 Dengarkan Penutup")
                    }
                    Text("Ada pertanyaan untuk HRD?", color = ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = candidateQuestion,
                        onValueChange = { candidateQuestion = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tulis pertanyaan Anda untuk HRD…", color = muted) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                speak("Terima kasih atas pertanyaan Anda. Kami catat dan akan menjawabnya pada tahap berikutnya. Selamat melanjutkan aktivitas.")
                                phase = SessionPhase.RESULT
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        ) { Text("Kirim Pertanyaan", color = Color.White, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { phase = SessionPhase.RESULT },
                            modifier = Modifier.weight(1f),
                        ) { Text("Lewati") }
                    }
                }
            }
            SessionPhase.CANDIDATE -> Unit
            SessionPhase.RESULT -> {
                GuidedResultView(
                    answers = answers,
                    totalScore = totalScore,
                    candidateName = candidateName,
                    candidateQuestion = candidateQuestion,
                    onRestart = onRestart,
                    onExit = onExit,
                )
            }
        }
    }
}

@Composable
private fun GuidedResultView(
    answers: List<GuidedAnswer>,
    totalScore: Int,
    candidateName: String?,
    candidateQuestion: String?,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val ink = KarirTextDark
    val muted = KarirTextMuted
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🏅", fontSize = 40.sp)
                Text("Hasil Latihan Interview", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text("$totalScore / 100", color = KarirOrange, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
                Text(
                    "Penilaian otomatis (offline) berdasarkan struktur STAR. " +
                        "Bukan nilai HRD sungguhan — pakai sebagai panduan latihan.",
                    color = muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                if (!candidateName.isNullOrBlank()) Text("Kandidat: $candidateName", color = ink, fontWeight = FontWeight.Bold)
                if (!candidateQuestion.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Pertanyaan Anda untuk HRD: \"$candidateQuestion\"", color = ink, fontSize = 13.sp)
                }
            }
        }
        if (answers.isEmpty()) {
            item { Text("Belum ada jawaban yang dikirim pada sesi ini.", color = muted) }
        }
        items(answers) { a ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(a.title, color = KarirOrange, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(a.question, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Jawaban Anda: ${a.answer.ifBlank { "(dilewati)" }}", color = ink, fontSize = 13.sp)
                    Text("Nilai: ${a.feedback.score} / 100", color = KarirOrange, fontWeight = FontWeight.Bold)
                    if (a.feedback.strengths.isNotEmpty()) {
                        Text("Kelebihan:", color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        a.feedback.strengths.forEach { Text("• $it", color = ink, fontSize = 13.sp) }
                    }
                    if (a.feedback.improvements.isNotEmpty()) {
                        Text("Yang perlu diperbaiki:", color = ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        a.feedback.improvements.forEach { Text("• $it", color = ink, fontSize = 13.sp) }
                    }
                    Text("Versi lebih kuat: ${a.feedback.improvedTip}", color = muted, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("↻ Ulangi Latihan") }
                Button(
                    onClick = onExit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                ) { Text("Selesai", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
