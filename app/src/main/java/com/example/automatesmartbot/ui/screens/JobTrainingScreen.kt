package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.automatesmartbot.data.career.TrainingServerGateway
import com.example.automatesmartbot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.automatesmartbot.data.career.TrainingCourses
import com.example.automatesmartbot.data.career.TrainingPositions
import com.example.automatesmartbot.data.career.TrainingPathways
import com.example.automatesmartbot.data.career.TrainingLevel
import com.example.automatesmartbot.data.career.TrainingPractice
import com.example.automatesmartbot.data.career.TrainingCertificateIdentity
import com.example.automatesmartbot.data.cv.CvRenderer
import com.example.automatesmartbot.ui.components.PdfExportButton
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

private val orangeTraining = Color(0xFFFF690D)
private val darkTraining = Color(0xFF152D43)
private val tealTraining = Color(0xFF009F89)
private val paleTraining = Color(0xFFF6F9FC)
private val trainingDisabledText = Color(0xFF475569)
private val trainingButtonColors @Composable get() = ButtonDefaults.outlinedButtonColors(
    contentColor = darkTraining,
    disabledContentColor = Color(0xFF475569),
    disabledContainerColor = Color(0xFFF1F5F9)
)

@Composable
private fun TrainingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = darkTraining),
        elevation = CardDefaults.cardElevation(2.dp)
    ) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable
private fun TrainingAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = orangeTraining,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = darkTraining
        )
    ) { Text(label, fontWeight = FontWeight.Bold, color = if (enabled) Color.White else darkTraining) }
}

@Composable
fun JobTrainingScreen() {
    val context = LocalContext.current
    val trainingScope = rememberCoroutineScope()
    val trainingApi = remember { BuildConfig.TRAINING_BASE_URL.takeIf { it.startsWith("https://") }?.let { TrainingServerGateway(it) } }
    var serverQuestions by remember { mutableStateOf<JSONArray?>(null) }
    var serverModules by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var trainingBusy by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("career_training", 0) }
    var courseId by rememberSaveable { mutableStateOf(TrainingCourses.all.first().id) }
    val course = remember(courseId) {
        TrainingCourses.all.firstOrNull { it.id == courseId } ?: TrainingPositions.groups.asSequence()
            .flatMap { it.positions.asSequence() }
            .flatMap { position -> TrainingLevel.entries.asSequence().map { TrainingPathways.courseFor(position, it) } }
            .firstOrNull { it.id == courseId } ?: TrainingCourses.all.first()
    }
    var stage by rememberSaveable { mutableStateOf("catalog") }
    var selectedCategory by rememberSaveable { mutableStateOf(0) }
    var selectedPosition by rememberSaveable { mutableStateOf("") }
    var selectedLevel by rememberSaveable { mutableStateOf(TrainingLevel.BASIC.name) }
    var module by rememberSaveable { mutableIntStateOf(0) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var message by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<JSONObject?>(null) }
    val completed = remember(courseId, version) { prefs.getStringSet("done_$courseId", emptySet()).orEmpty().toSet() }
    val answers = remember(courseId, version) { JSONArray(prefs.getString("answers_$courseId", "[]")) }
    val allDone = course.lessons.indices.all { it.toString() in completed }
    val serverAllDone = course.lessons.indices.all { it in serverModules }
    fun choose(id: String) {
        courseId = id; module = 0; stage = "modules"; message = ""; certificate = null
        serverQuestions = null
        serverModules = emptySet()
        if (trainingApi != null) trainingScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { trainingApi.progress(id) }
                val list = result.getJSONArray("completedModules")
                serverModules = (0 until list.length()).map { list.getInt(it) }.toSet()
            } catch (e: Exception) { message = "Progres server tidak tersedia: ${e.message}" }
        }
    }
    fun saveAnswer(index: Int, choice: Int) {
        answers.put(index, choice)
        prefs.edit().putString("answers_$courseId", answers.toString()).apply()
        version++
    }
    fun completeModule() {
        prefs.edit().putStringSet("done_$courseId", completed + module.toString()).apply()
        version++
        stage = "modules"
    }
    fun evaluate() {
        if (trainingApi != null) {
            val questions = serverQuestions
            if (questions == null) { message = "Soal server belum dimuat."; return }
            val values = (0 until questions.length()).map { answers.optInt(it, -1) }
            if (values.any { it < 0 }) { message = "Jawab semua pertanyaan."; return }
            trainingBusy = true
            trainingScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { trainingApi.submitExam(courseId, values) }
                    val cid = result.getString("certificateId")
                    val verification = withContext(Dispatchers.IO) { trainingApi.certificateStatus(cid) }
                    if (!verification.optBoolean("valid")) error("Sertifikat belum terverifikasi server")
                    val record = verification.getJSONObject("certificate")
                    val item = JSONObject().put("id", cid).put("name", record.getString("name"))
                        .put("course", record.getString("course")).put("courseId", courseId)
                        .put("date", record.getString("date")).put("score", record.getInt("score"))
                        .put("serverRegistered", true)
                    val history = JSONArray(prefs.getString("certificates", "[]"))
                    history.put(item)
                    prefs.edit().putString("certificates", history.toString()).apply()
                    certificate = item
                    stage = "certificate"
                    message = "Sertifikat terverifikasi server."
                } catch (e: Exception) {
                    val detail = e.message.orEmpty()
                    message = when {
                        "exam_not_passed" in detail -> "Nilai belum mencapai 80/100. Silakan ulangi ujian."
                        "modules_incomplete" in detail -> "Progres modul belum lengkap di server. Kembali ke daftar modul dan selesaikan modul yang belum tersimpan."
                        "invalid_or_incomplete_assessment" in detail -> "Data ujian belum lengkap atau tidak sesuai. Periksa jawaban lalu ulangi ujian."
                        else -> "Ujian gagal diproses. Periksa koneksi dan coba lagi."
                    }
                }
                finally { trainingBusy = false }
            }
            return
        }
        val values = course.questions.indices.map { answers.optInt(it, -1) }
        if (values.any { it < 0 }) { message = "Jawab semua pertanyaan."; return }
        if (name.isBlank()) { message = "Isi nama peserta untuk sertifikat."; return }
        val score = TrainingCourses.score(values, course.questions)
        message = "Nilai akhir: $score/100"
        if (score >= 80 && allDone) {
            val item = JSONObject().put("id", UUID.randomUUID().toString())
                .put("name", name.trim()).put("course", course.title)
                .put("courseId", courseId).put("date", LocalDate.now().toString()).put("score", score)
            val history = JSONArray(prefs.getString("certificates", "[]"))
            history.put(item)
            prefs.edit().putString("certificates", history.toString()).apply()
            certificate = item
            stage = "certificate"
        } else {
            message += ". Belum lulus; selesaikan modul dan ulangi evaluasi."
        }
    }
    CompositionLocalProvider(LocalContentColor provides darkTraining, LocalTextStyle provides LocalTextStyle.current.copy(color = darkTraining)) {
    Column(Modifier.fillMaxSize().background(paleTraining)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stage != "catalog") TextButton(onClick = {
                stage = when (stage) {
                    "modules" -> "positions"
                    "positions" -> "catalog"
                    "lesson", "practice", "exam" -> "modules"
                    else -> "catalog"
                }
            }) { Text("‹", color = darkTraining, fontSize = 28.sp) }
            Text("▣ KarirMate", color = orangeTraining, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Surface(color = orangeTraining, shape = RoundedCornerShape(50.dp)) {
                Text("♛ PRO", Modifier.padding(horizontal = 13.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (stage) {
                "catalog" -> {
                    Text("Pelatihan Kerja", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                    Text("Tingkatkan skill, kuasai kompetensi, dan dapatkan sertifikat penyelesaian.")
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEDB), contentColor = darkTraining), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Siapkan Karier Impianmu Lewat Pelatihan Terarah", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                            listOf("✓ Materi praktis", "✓ Latihan & evaluasi", "✓ Sertifikat penyelesaian KarirMate").forEach {
                                Text(it, color = tealTraining, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    TrainingHeroIllustration(Modifier.fillMaxWidth())
                    Text("Jelajahi Posisi Pekerjaan", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = darkTraining)
                    Text("${TrainingPositions.groups.size} bidang • ${TrainingPositions.count} posisi tersedia", color = darkTraining)
                    TrainingPositions.groups.forEachIndexed { index, group ->
                        OutlinedButton(colors = trainingButtonColors, onClick = { selectedCategory = index; stage = "positions" }, modifier = Modifier.fillMaxWidth()) {
                            Text("${group.category}  (${group.positions.size} posisi)", color = darkTraining, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Pilih Kategori Pelatihan", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = darkTraining)
                    TrainingCourses.all.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                Card(
                                    modifier = Modifier.weight(1f).height(120.dp).clickable { choose(item.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = darkTraining)
                                ) {
                                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("▣", color = orangeTraining, fontSize = 28.sp)
                                        Text(item.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp, color = darkTraining)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Sertifikat penyelesaian adalah bukti latihan internal, bukan sertifikasi kompetensi resmi BNSP.", fontSize = 12.sp)
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Riwayat Sertifikat") }
                }
                "positions" -> {
                    val group = TrainingPositions.groups[selectedCategory]
                    Text(group.category, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = darkTraining)
                    Text("Pilih posisi yang ingin dipelajari", color = darkTraining)
                    group.positions.forEach { position ->
                        TrainingCard {
                            Text(position, fontWeight = FontWeight.Bold, color = darkTraining)
                            TrainingLevel.entries.forEach { level ->
                                val training = TrainingPathways.courseFor(position, level)
                                val previous = when (level) {
                                    TrainingLevel.BASIC -> null
                                    TrainingLevel.INTERMEDIATE -> TrainingPathways.courseFor(position, TrainingLevel.BASIC).id
                                    TrainingLevel.ADVANCED -> TrainingPathways.courseFor(position, TrainingLevel.INTERMEDIATE).id
                                }
                                val history = JSONArray(prefs.getString("certificates", "[]"))
                                val unlocked = previous == null || (0 until history.length()).any {
                                    history.optJSONObject(it)?.optString("courseId") == previous
                                }
                                OutlinedButton(
                                    enabled = unlocked,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = darkTraining, disabledContentColor = trainingDisabledText, disabledContainerColor = Color(0xFFF1F5F9)),
                                    onClick = {
                                        selectedPosition = position
                                        selectedLevel = level.name
                                        choose(training.id)
                                    }
                                ) { Text(if (unlocked) "Mulai ${level.label}" else "🔒 ${level.label} — selesaikan tingkat sebelumnya", color = if (unlocked) darkTraining else trainingDisabledText) }
                            }
                        }
                    }
                }
                "modules" -> {
                    Text(course.title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                    if (selectedPosition.isNotBlank()) Text("Posisi pilihan: $selectedPosition")
                    TrainingCard {
                        Text("Pelajari materi dan selesaikan seluruh modul", fontWeight = FontWeight.Bold, color = darkTraining)
                        Text("${course.lessons.size} Modul  •  Evaluasi Akhir  •  Sertifikat", color = darkTraining)
                        LinearProgressIndicator(
                            progress = { completed.size.toFloat() / course.lessons.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth(),
                            color = orangeTraining
                        )
                    }
                    Text("Daftar Modul", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = darkTraining)
                    course.lessons.forEachIndexed { i, lesson ->
                        val done = i.toString() in completed
                        val unlocked = i == 0 || (i - 1).toString() in completed
                        Card(
                            onClick = { if (unlocked) { module = i; stage = "lesson" } },
                            enabled = unlocked,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = darkTraining, disabledContainerColor = Color(0xFFF1F5F9), disabledContentColor = trainingDisabledText),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (done) "✓" else if (unlocked) "▶" else "🔒", color = if (done) tealTraining else orangeTraining)
                                Spacer(Modifier.width(12.dp))
                                Text(lesson.first, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = if (unlocked) darkTraining else trainingDisabledText)
                                Text(if (done) "Selesai" else if (unlocked) "Mulai" else "Terkunci", fontSize = 11.sp, color = if (unlocked) darkTraining else trainingDisabledText)
                            }
                        }
                    }
                    TrainingAction("Evaluasi Akhir →", enabled = (if (trainingApi != null) serverAllDone else allDone) && !trainingBusy) {
                        if (trainingApi == null) { stage = "exam"; questionIndex = 0 }
                        else {
                            trainingBusy = true
                            trainingScope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) { trainingApi.questions(courseId) }
                                    serverQuestions = result.getJSONArray("questions")
                                    questionIndex = 0
                                    stage = "exam"
                                } catch (e: Exception) { message = "Gagal memuat soal server: ${e.message}" }
                                finally { trainingBusy = false }
                            }
                        }
                    }
                    if (message.isNotBlank()) Text(message)
                }
                "lesson" -> {
                    Text("Modul ${module + 1}", color = orangeTraining, fontWeight = FontWeight.Bold)
                    Text(course.lessons[module].first, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    TrainingLessonVisual(course.lessons[module].first)
                    TrainingVideo(courseId, module, course.lessons[module].first, course.lessons[module].second)
                    TrainingNarration(course.lessons[module].first, course.lessons[module].second)
                    TrainingCard {
                        Text("Tujuan Pembelajaran", fontWeight = FontWeight.Bold)
                        Text("✓ Memahami materi dan penerapannya di dunia kerja.", color = tealTraining)
                        HorizontalDivider()
                        Text("Materi", fontWeight = FontWeight.Bold)
                        Text(course.lessons[module].second)
                    }
                    TrainingAction("Lanjut ke Latihan →") { stage = "practice" }
                }
                "practice" -> {
                    Text("Latihan Praktik", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                    Text("Modul ${module + 1} – ${course.lessons[module].first}", color = darkTraining)
                    TrainingCard {
                        Text("🏠  Studi Kasus", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = darkTraining)
                        Text("Berikan contoh nyata bagaimana materi ini diterapkan dalam pekerjaan atau saat melamar kerja.", color = darkTraining)
                    }
                    var practice by rememberSaveable(courseId, module) {
                        mutableStateOf(prefs.getString("practice_${courseId}_$module", "").orEmpty())
                    }
                    OutlinedTextField(
                        value = practice,
                        onValueChange = {
                            practice = it
                            prefs.edit().putString("practice_${courseId}_$module", it).apply()
                        },
                        label = { Text("Jawaban latihan praktik", color = darkTraining) },
                        textStyle = LocalTextStyle.current.copy(color = darkTraining),
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = darkTraining,
                            unfocusedTextColor = darkTraining,
                            disabledTextColor = darkTraining,
                            focusedLabelColor = darkTraining,
                            unfocusedLabelColor = darkTraining,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = trainingDisabledText,
                            cursorColor = darkTraining
                        )
                    )
                    val practiceResult = TrainingPractice.assess(practice)
                    Text(practiceResult.feedback, color = if (practiceResult.accepted) tealTraining else orangeTraining)
                    TrainingAction("Kumpulkan Jawaban", enabled = practiceResult.accepted && !trainingBusy) {
                        if (trainingApi == null) completeModule()
                        else {
                            trainingBusy = true
                            trainingScope.launch {
                                try {
                                    withContext(Dispatchers.IO) { trainingApi.completeModule(courseId, module, practice) }
                                    serverModules = serverModules + module
                                    completeModule()
                                } catch (e: Exception) { message = "Modul belum tersimpan di server: ${e.message}" }
                                finally { trainingBusy = false }
                            }
                        }
                    }
                    if (message.isNotBlank()) Text(message)
                }
                "exam" -> {
                    Text("Evaluasi Akhir", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(course.title)
                    Text("Soal ${questionIndex + 1} dari ${serverQuestions?.length() ?: course.questions.size}", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (questionIndex + 1f) / (serverQuestions?.length() ?: course.questions.size).coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        color = orangeTraining
                    )
                    val remoteQuestion = serverQuestions?.optJSONObject(questionIndex)
                    val q = course.questions.getOrNull(questionIndex)
                    val questionPrompt = remoteQuestion?.optString("prompt") ?: q?.prompt.orEmpty()
                    val questionOptions = remoteQuestion?.optJSONArray("options")?.let { array -> (0 until array.length()).map { array.getString(it) } } ?: q?.options.orEmpty()
                    TrainingCard {
                        Text("${questionIndex + 1}. ${questionPrompt}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        questionOptions.forEachIndexed { index, option ->
                            val selected = answers.optInt(questionIndex, -1) == index
                            Card(
                                onClick = { saveAnswer(questionIndex, index) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFEAD7) else Color.White, contentColor = darkTraining),
                                border = BorderStroke(1.dp, if (selected) orangeTraining else Color(0xFFE1E5EA))
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected, onClick = { saveAnswer(questionIndex, index) })
                                    Text(option)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(colors = trainingButtonColors, 
                            onClick = { questionIndex-- },
                            enabled = questionIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("‹ Sebelumnya") }
                        Button(
                            onClick = {
                                if (questionIndex < (serverQuestions?.length() ?: course.questions.size) - 1) questionIndex++
                                else stage = "submit"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
            containerColor = orangeTraining,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = darkTraining
        )
                        ) { Text(if (questionIndex == (serverQuestions?.length() ?: course.questions.size) - 1) "Selesai" else "Berikutnya ›") }
                    }
                }
                "submit" -> {
                    Text("Selesaikan Evaluasi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(name, {
                        name = it
                        prefs.edit().putString("name", it).apply()
                    }, label = { Text("Nama lengkap peserta") }, modifier = Modifier.fillMaxWidth())
                    Text("Nilai minimal kelulusan: 80/100")
                    TrainingAction("Lihat Hasil") { evaluate() }
                    if (message.isNotBlank()) Text(message, color = orangeTraining)
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "exam"; questionIndex = 0 }) { Text("Ulangi Soal") }
                }
                "certificate" -> {
                    Text("Sertifikat Penyelesaian", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("✓ Selamat! Kamu telah menyelesaikan pelatihan.", color = tealTraining, fontWeight = FontWeight.Bold)
                    certificate?.let { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(2.dp, Color(0xFFD6A34D)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF8), contentColor = darkTraining),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("KARIRMATE", color = orangeTraining, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                                Text("PROGRAM PELATIHAN KERJA DIGITAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                HorizontalDivider(color = Color(0xFFD6A34D), thickness = 1.dp)
                                Text("SERTIFIKAT", fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Text("PENYELESAIAN PELATIHAN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                                Text("Dengan ini diberikan kepada", fontSize = 11.sp)
                                Text(item.optString("name").ifBlank { "Nama peserta belum tersedia" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f), color = Color(0xFFD6A34D))
                                Text("Atas keberhasilan menyelesaikan program pelatihan", fontSize = 11.sp, textAlign = TextAlign.Center)
                                Text(item.optString("course"), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("Nilai akhir: ${item.optInt("score")}/100  •  Status: LULUS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("Diterbitkan oleh KarirMate  •  ${item.optString("date")}", fontSize = 11.sp)
                                Text("Nomor sertifikat", fontSize = 10.sp)
                                Text(item.optString("id"), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                HorizontalDivider(color = Color(0xFFD6A34D))
                                Text(
                                    if (item.optBoolean("serverRegistered")) "Verifikasi keaslian tersedia melalui registri KarirMate."
                                    else "Identitas sertifikat dapat diperiksa pada riwayat perangkat ini.",
                                    fontSize = 10.sp, textAlign = TextAlign.Center
                                )
                                Text("Sertifikat pelatihan internal KarirMate • Bukan sertifikasi kompetensi BNSP.", fontSize = 9.sp, textAlign = TextAlign.Center, color = Color(0xFF526374))
                            }
                        }
                        val pdfText = "SERTIFIKAT PENYELESAIAN PELATIHAN\\n\\nKarirMate\\n\\nDiberikan kepada: ${item.optString("name")}\\n\\nTelah menyelesaikan pelatihan: ${item.optString("course")}\\n\\nHasil akhir: ${item.optInt("score")}/100\\nTanggal: ${item.optString("date")}\\nID: ${item.optString("id")}\\n\\nSertifikat internal KarirMate, bukan sertifikasi profesi/BNSP."
                        TrainingCertificateExport(item)
                    }
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Riwayat Sertifikat") }
                }
                "history" -> {
                    Text("Riwayat Sertifikat", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    val history = JSONArray(prefs.getString("certificates", "[]"))
                    if (history.length() == 0) Text("Belum ada sertifikat.")
                    var qrInput by rememberSaveable { mutableStateOf("") }
                    var qrFeedback by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = qrInput,
                        onValueChange = { qrInput = it; qrFeedback = "" },
                        label = { Text("Isi QR sertifikat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(colors = trainingButtonColors, onClick = {
                        val known = (0 until history.length()).mapNotNull { index ->
                            val record = history.optJSONObject(index) ?: return@mapNotNull null
                            runCatching {
                                TrainingCertificateIdentity.payload(
                                    record.optString("id"), record.optString("courseId"),
                                    record.optString("date"), record.optInt("score")
                                )
                            }.getOrNull()
                        }
                        qrFeedback = if (TrainingCertificateIdentity.verifyLocal(qrInput.trim(), known))
                            "Cocok dengan riwayat pada perangkat ini."
                        else "Tidak ditemukan di perangkat ini; belum ada registri online."
                    }) { Text("Periksa Identitas Sertifikat") }
                    if (qrFeedback.isNotBlank()) Text(qrFeedback)
                    (history.length() - 1 downTo 0).forEach { i ->
                        val item = history.getJSONObject(i)
                        TrainingCard {
                            Text(item.optString("course"), fontWeight = FontWeight.Bold)
                            Text("${item.optString("name")} • ${item.optString("date")}")
                            TextButton(onClick = { certificate = item; stage = "certificate" }) { Text("Buka Sertifikat") }
                        }
                    }
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "catalog" }) { Text("Kembali ke Katalog") }
                }
            }
        }
    }
    }
}
