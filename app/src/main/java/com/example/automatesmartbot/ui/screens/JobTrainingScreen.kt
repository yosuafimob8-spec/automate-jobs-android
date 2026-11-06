package com.example.automatesmartbot.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.graphics.nativeCanvas
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

import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenPageGradient
import com.example.automatesmartbot.ui.theme.OnKarirOrange
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirInputColors

private val orangeTraining = Color(0xFF2563A6)
private val darkTraining @Composable get() = ScreenText
private val tealTraining = Color(0xFF16A34A)
private val paleTraining @Composable get() = screenCanvas(Color(0xFFF5FAFC))
private val trainingDisabledText @Composable get() = ScreenMuted
private val trainingButtonColors @Composable get() = ButtonDefaults.outlinedButtonColors(
    containerColor = ScreenSurface,
    contentColor = darkTraining,
    disabledContentColor = ScreenMuted,
    disabledContainerColor = screenCanvas(Color(0xFFF8FAFC))
)

@Composable
private fun TrainingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface, contentColor = darkTraining),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
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
            contentColor = OnKarirOrange,
            disabledContainerColor = screenCanvas(Color(0xFFE2E8F0)),
            disabledContentColor = darkTraining
        )
    ) { Text(label, fontWeight = FontWeight.Bold, color = if (enabled) OnKarirOrange else darkTraining) }
}

@Composable
fun JobTrainingScreen(
    initialStage: String = "catalog",
    onExit: () -> Unit = {},
    onBackActionChanged: ((() -> Unit)?) -> Unit = {}
) {
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
    // Local, reviewed question bank is bundled for Android Studio/offline testing.
    // Online grading still uses the authenticated server's questions and answers.
    val offlineQuestions = remember(courseId) {
        if (!courseId.startsWith("role_")) course.questions else {
            (1..15).asSequence().mapNotNull { category ->
                runCatching {
                    val file = "training_questions/category_${category.toString().padStart(2, '0')}.json"
                    val bank = JSONObject(context.assets.open(file).bufferedReader().use { it.readText() })
                    bank.optJSONArray(courseId)?.let { array ->
                        (0 until array.length()).map { index ->
                            val q = array.getJSONObject(index)
                            val choices = q.getJSONArray("options")
                            com.example.automatesmartbot.data.career.TrainingQuestion(
                                q.getString("prompt"),
                                (0 until choices.length()).map { choices.getString(it) },
                                q.getInt("correct")
                            )
                        }
                    }
                }.getOrNull()
            }.firstOrNull() ?: emptyList()
        }
    }
    var stage by rememberSaveable(initialStage) { mutableStateOf(initialStage) }
    fun backOneStage() {
        when (stage) {
            "catalog" -> onExit()
            "history" -> if (initialStage == "history") onExit() else stage = "catalog"
            "certificate" -> stage = "history"
            "positions" -> stage = "catalog"
            "modules" -> stage = "positions"
            "lesson", "practice", "exam", "result" -> stage = "modules"
            else -> stage = "catalog"
        }
    }
    DisposableEffect(stage, initialStage) {
        onBackActionChanged({ backOneStage() })
        onDispose { onBackActionChanged(null) }
    }
    androidx.activity.compose.BackHandler(enabled = stage != "catalog") { backOneStage() }
    var selectedCategory by rememberSaveable { mutableStateOf(0) }
    var selectedPosition by rememberSaveable { mutableStateOf("") }
    var selectedLevel by rememberSaveable { mutableStateOf(TrainingLevel.BASIC.name) }
    var module by rememberSaveable { mutableIntStateOf(0) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var message by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<JSONObject?>(null) }
    var examScore by rememberSaveable { mutableStateOf<Int?>(null) }
    var examCorrect by rememberSaveable { mutableStateOf<Int?>(null) }
    val completed = remember(courseId, version) { prefs.getStringSet("done_$courseId", emptySet()).orEmpty().toSet() }
    val answers = remember(courseId, version) { JSONArray(prefs.getString("answers_$courseId", "[]")) }
    val allDone = course.lessons.indices.all { it.toString() in completed }
    val serverAllDone = course.lessons.indices.all { it in serverModules }
    fun choose(id: String) {
        courseId = id; module = 0; stage = "modules"; message = ""; certificate = null; examScore = null; examCorrect = null
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
    fun resetExamAnswers() {
        prefs.edit().remove("answers_$courseId").apply()
        version++
        message = ""
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
            if (questions.length() != 20 || values.any { it !in 0..2 }) {
                message = "Jawaban belum lengkap atau pilihan tidak valid. Pastikan semua 20 soal terjawab."
                return
            }
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
                    examScore = record.getInt("score").coerceIn(0, 100)
                    examCorrect = null
                    stage = "result"
                    message = "Sertifikat terverifikasi server."
                } catch (e: Exception) {
                    val detail = e.message.orEmpty()
                    message = when {
                        "exam_not_passed" in detail -> "Nilai belum mencapai 80/100. Silakan ulangi ujian."
                        "modules_incomplete" in detail -> "Progres modul belum lengkap di server. Kembali ke daftar modul dan selesaikan modul yang belum tersimpan."
                        "invalid_or_incomplete_assessment" in detail -> "Server menolak format ujian. Pastikan 20 jawaban valid; bila tetap gagal, laporkan HTTP 400."
                        "authenticated_name_required" in detail -> "Nama akun login belum tersedia di server. Lengkapi profil akun terlebih dahulu."
                        "invalid_answers" in detail -> "Server menerima jawaban yang tidak valid. Mulai ulang evaluasi."
                        else -> "Gagal memproses ujian: ${detail.take(280).ifBlank { "Kesalahan tidak diketahui" }}. Jangan ulangi soal sebelum penyebabnya diperiksa."
                    }
                }
                finally { trainingBusy = false }
            }
            return
        }
        val values = offlineQuestions.indices.map { answers.optInt(it, -1) }
        if (offlineQuestions.size != 20) { message = "Bank soal 20 pertanyaan belum tersedia. Periksa aset pelatihan."; return }
        if (values.any { it < 0 }) { message = "Jawab semua pertanyaan."; return }
        if (name.isBlank()) { message = "Isi nama peserta untuk sertifikat."; return }
        val score = TrainingCourses.score(values, offlineQuestions)
        examScore = score
        examCorrect = offlineQuestions.indices.count { values[it] == offlineQuestions[it].correct }
        stage = "result"
        message = "Nilai akhir: $score/100"
        if (score >= 80 && allDone) {
            val item = JSONObject().put("id", UUID.randomUUID().toString())
                .put("name", name.trim()).put("course", course.title)
                .put("courseId", courseId).put("date", LocalDate.now().toString()).put("score", score)
            val history = JSONArray(prefs.getString("certificates", "[]"))
            history.put(item)
            prefs.edit().putString("certificates", history.toString()).apply()
            certificate = item
        } else {
            message += ". Belum lulus; selesaikan modul dan ulangi evaluasi."
        }
    }
    CompositionLocalProvider(LocalContentColor provides darkTraining, LocalTextStyle provides LocalTextStyle.current.copy(color = darkTraining)) {
    Box(Modifier.fillMaxSize().background(ScreenPageGradient)) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(screenCanvas(Color(0xFFFFFDF9))).border(BorderStroke(0.75.dp, screenCanvas(Color(0xFFF1E5DC)))).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▣ KarirMate", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.LIGHT)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (stage) {
                "catalog" -> {
                    val navy = darkTraining
                    val blue = Color(0xFF0866B5)
                    val categoryColors = listOf(
                        Color(0xFFE5F4FF), Color(0xFFF2E9FF), Color(0xFFE5FAF0),
                        Color(0xFFFFF2DF), Color(0xFFFFE9F0), Color(0xFFE4F8FC)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("🎓", fontSize = 48.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Pelatihan Kerja", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = navy, modifier = Modifier.weight(1f))
                        Text("▶ 📚", fontSize = 27.sp, color = blue)
                    }
                    val shortcuts = listOf(
                        Triple("📖", "Katalog\nPelatihan", "catalog"),
                        Triple("▶️", "Lanjutkan\nBelajar", "modules"),
                        Triple("🏅", "Sertifikat\nSaya", "history"),
                        Triple("📊", "Riwayat\nPelatihan", "history")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        shortcuts.forEach { (emoji, title, destination) ->
                            Card(
                                modifier = Modifier.weight(1f).height(114.dp).clickable {
                                    if (destination == "modules") choose(courseId)
                                    else stage = destination
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8)))
                            ) {
                                Column(Modifier.fillMaxSize().padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(emoji, fontSize = 26.sp)
                                    Text(title, color = navy, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                    Text("▱  Pilih Kategori Pelatihan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = navy)
                    val groups = TrainingPositions.groups
                    groups.take(6).chunked(2).forEachIndexed { rowIndex, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            pair.forEachIndexed { colIndex, group ->
                                val index = rowIndex * 2 + colIndex
                                val icon = listOf("💼", "💬", "🏭", "💻", "📣", "🎧")[index]
                                Card(
                                    modifier = Modifier.weight(1f).heightIn(min = 86.dp).clickable { selectedCategory = index; stage = "positions" },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = screenCanvas(categoryColors[index])),
                                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8)))
                                ) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(icon, fontSize = 24.sp)
                                        Spacer(Modifier.width(7.dp))
                                        Text(group.category, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = navy, modifier = Modifier.weight(1f))
                                        Text("›", fontSize = 23.sp, color = navy)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    if (groups.size > 6) {
                        Text("Kategori lainnya", fontWeight = FontWeight.Bold, color = navy)
                        groups.drop(6).forEachIndexed { offset, group ->
                            OutlinedButton(onClick = { selectedCategory = offset + 6; stage = "positions" }, modifier = Modifier.fillMaxWidth()) {
                                Text(group.category, color = navy)
                            }
                        }
                    }
                    Text("🔥  Level Pelatihan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = navy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(Triple("🌱", "Basic", "Pemula"), Triple("📊", "Intermediate", "Menengah"), Triple("🏆", "Advance", "Mahir")).forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.weight(1f).height(110.dp).clickable {
                                    selectedLevel = TrainingLevel.entries[index].name
                                    stage = "positions"
                                },
                                colors = CardDefaults.cardColors(containerColor = screenCanvas(listOf(Color(0xFFE0F9EE), Color(0xFFE4F2FF), Color(0xFFF1E8FF))[index])),
                                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.fillMaxSize().padding(5.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(item.first, fontSize = 25.sp)
                                    Text(item.second, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = navy)
                                    Text("(${item.third})", fontSize = 11.sp, color = navy)
                                }
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))), color = ScreenSurface) {
                        Text("ⓘ  Sertifikat penyelesaian adalah bukti latihan internal, bukan sertifikasi kompetensi resmi BNSP.", modifier = Modifier.fillMaxWidth().padding(14.dp), color = navy, fontSize = 13.sp)
                    }
                    Button(onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = blue), shape = RoundedCornerShape(14.dp)) {
                        Text("▣  Lihat Riwayat Sertifikat  ›", fontWeight = FontWeight.Bold)
                    }
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
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = darkTraining, disabledContentColor = trainingDisabledText, disabledContainerColor = screenCanvas(Color(0xFFF1F5F9))),
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
                    if (selectedPosition.isNotBlank()) Text("Posisi pilihan: $selectedPosition", color = darkTraining)
                    TrainingCard {
                        Text("Pelajari materi dan selesaikan seluruh modul", fontWeight = FontWeight.Bold, color = darkTraining)
                        Text("${course.lessons.size} Modul  •  Evaluasi Akhir  •  Sertifikat", color = darkTraining)
                        LinearProgressIndicator(
                            progress = { completed.size.toFloat() / course.lessons.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
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
                            colors = CardDefaults.cardColors(containerColor = ScreenSurface, contentColor = darkTraining, disabledContainerColor = screenCanvas(Color(0xFFF1F5F9)), disabledContentColor = trainingDisabledText),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (done) Color(0xFFBBF7D0) else screenCanvas(Color(0xFFC6DFE8))),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        if (trainingApi == null) {
                            if (offlineQuestions.size != 20) message = "Bank soal 20 pertanyaan belum tersedia untuk kursus ini."
                            else { resetExamAnswers(); stage = "exam"; questionIndex = 0 }
                        }
                        else {
                            trainingBusy = true
                            trainingScope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) { trainingApi.questions(courseId) }
                                    val loaded = result.getJSONArray("questions")
                                    if (loaded.length() != 20) error("Bank soal server bukan 20 pertanyaan")
                                    resetExamAnswers()
                                    serverQuestions = loaded
                                    questionIndex = 0
                                    stage = "exam"
                                } catch (e: Exception) { message = "Gagal memuat soal server: ${e.message}" }
                                finally { trainingBusy = false }
                            }
                        }
                    }
                    if (message.isNotBlank()) Text(message, color = darkTraining)
                }
                "lesson" -> {
                    Text("Modul ${module + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                        colors = karirInputColors()
                    )
                    val practiceResult = TrainingPractice.assess(practice)
                    Text(practiceResult.feedback, color = if (practiceResult.accepted) screenCanvas(Color(0xFF166534), Color(0xFF86EFAC)) else MaterialTheme.colorScheme.primary)
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
                    Text("Soal ${questionIndex + 1} dari ${serverQuestions?.length() ?: offlineQuestions.size}", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (questionIndex + 1f) / (serverQuestions?.length() ?: offlineQuestions.size).coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    val remoteQuestion = serverQuestions?.optJSONObject(questionIndex)
                    val q = offlineQuestions.getOrNull(questionIndex)
                    val questionPrompt = remoteQuestion?.optString("prompt") ?: q?.prompt.orEmpty()
                    val questionOptions = remoteQuestion?.optJSONArray("options")?.let { array -> (0 until array.length()).map { array.getString(it) } } ?: q?.options.orEmpty()
                    TrainingCard {
                        Text("${questionIndex + 1}. ${questionPrompt}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        questionOptions.forEachIndexed { index, option ->
                            val selected = answers.optInt(questionIndex, -1) == index
                            Card(
                                onClick = { saveAnswer(questionIndex, index) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (selected) screenCanvas(Color(0xFFFFEAD7)) else ScreenSurface, contentColor = darkTraining),
                                border = BorderStroke(1.dp, if (selected) orangeTraining else screenCanvas(Color(0xFFC6DFE8)))
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
                                if (questionIndex < (serverQuestions?.length() ?: offlineQuestions.size) - 1) questionIndex++
                                else stage = "submit"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
            containerColor = orangeTraining,
            contentColor = OnKarirOrange,
            disabledContainerColor = screenCanvas(Color(0xFFE2E8F0)),
            disabledContentColor = darkTraining
        )
                        ) { Text(if (questionIndex == (serverQuestions?.length() ?: offlineQuestions.size) - 1) "Selesai" else "Berikutnya ›") }
                    }
                }
                "submit" -> {
                    Text("Selesaikan Evaluasi", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(name, {
                        name = it
                        prefs.edit().putString("name", it).apply()
                    }, label = { Text("Nama lengkap peserta") }, colors = karirInputColors(), modifier = Modifier.fillMaxWidth())
                    Text("Nilai minimal kelulusan: 80/100")
                    TrainingAction("Lihat Hasil", enabled = !trainingBusy) { evaluate() }
                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(colors = trainingButtonColors, onClick = { resetExamAnswers(); stage = "exam"; questionIndex = 0 }) { Text("Ulangi Soal") }
                }
                "result" -> {
                    val score = examScore ?: 0
                    var animateScore by remember { mutableStateOf(false) }
                    LaunchedEffect(score) { animateScore = true }
                    val animatedScore by animateIntAsState(if (animateScore) score else 0, animationSpec = tween(1000), label = "trainingScore")
                    val passed = score >= 80 && certificate != null
                    val rating = when { score >= 80 -> "Sangat Baik"; score >= 60 -> "Baik"; score >= 40 -> "Terus Berkembang"; else -> "Tahap Pengembangan" }
                    Text("LAPORAN LATIHAN", color = Color(0xFF008A81), fontWeight = FontWeight.Bold)
                    Text("Hasil Pelatihan Kerja", color = darkTraining, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(course.title, color = ScreenMuted)
                    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF003A2B)), elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF003C2E), Color(0xFF00583A), Color(0xFF002F28)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("♛  HASIL PELATIHAN  ♛", color = Color(0xFFFFD978), fontWeight = FontWeight.Bold)
                            Box(Modifier.size(194.dp), contentAlignment = Alignment.Center) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, style = Stroke(17.dp.toPx(), cap = StrokeCap.Round))
                                    drawArc(Color(0xFFFFC45D), -90f, 360f * animatedScore / 100f, false, style = Stroke(17.dp.toPx(), cap = StrokeCap.Round))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Skor Akhir", color = Color.White, fontSize = 13.sp)
                                    Text("$animatedScore", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("/ 100", color = Color(0xFFB9F2E9), fontSize = 18.sp)
                                }
                            }
                            Surface(color = Color(0xFFFFC45D), shape = RoundedCornerShape(3.dp)) {
                                Text("🏆  $rating  🏆", color = Color(0xFF102C38), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                            }
                            examCorrect?.let { Text("$it dari ${offlineQuestions.size} soal benar", color = Color(0xFFE4F5F2)) }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Text(if (passed) "✦  Selamat! Pertahankan hasil baikmu dan terus tingkatkan keterampilan." else "✦  Tetap semangat! Pelajari materi kembali dan tingkatkan hasil latihanmu.", color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                    examCorrect?.let { correct ->
                        val total = offlineQuestions.size.coerceAtLeast(1)
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = ScreenSurface), elevation = CardDefaults.cardElevation(5.dp)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                TrainingResultStat("✓", "$correct", "Benar", Color(0xFF0EAE4D), Modifier.weight(1f))
                                TrainingResultStat("✕", "${total - correct}", "Salah", Color(0xFFE52935), Modifier.weight(1f))
                                TrainingResultStat("◎", "${correct * 100 / total}%", "Akurasi", Color(0xFFB319C7), Modifier.weight(1f))
                            }
                        }
                    }
                    Text(if (passed) "LULUS • Nilai minimal 80/100" else "BELUM LULUS • Nilai minimal 80/100", color = if (passed) tealTraining else orangeTraining, fontWeight = FontWeight.Bold)
                    if (message.isNotBlank()) Text(message, color = darkTraining)
                    if (passed) TrainingAction("Selesai • Kembali ke Pelatihan") { stage = "modules" }
                    else TrainingAction("Ulangi Evaluasi") { resetExamAnswers(); questionIndex = 0; stage = "exam" }
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "modules" }) { Text("Kembali ke Modul") }
                }
                "certificate" -> {
                    Text("Sertifikat Penyelesaian", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("✓ Selamat! Kamu telah menyelesaikan pelatihan.", color = tealTraining, fontWeight = FontWeight.Bold)
                    certificate?.let { item ->
                        Text("Pratinjau sertifikat • geser horizontal untuk melihat ukuran landscape", fontSize = 12.sp, color = darkTraining)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            androidx.compose.foundation.Canvas(Modifier.width(620.dp).height(438.dp)) {
                                val androidCanvas = drawContext.canvas.nativeCanvas
                                androidCanvas.save()
                                androidCanvas.scale(size.width / 842f, size.height / 595f)
                                TrainingCertificateArtwork.draw(androidCanvas, item, context)
                                androidCanvas.restore()
                            }
                        }
                        TrainingCertificateExport(item)
                    }
                    OutlinedButton(colors = trainingButtonColors, onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Riwayat Sertifikat") }
                }
                "history" -> {
                    val history = JSONArray(prefs.getString("certificates", "[]"))
                    CertificateHistoryBody(
                        history = history,
                        onShowCertificate = { item -> certificate = item; stage = "certificate" },
                        onBackToCatalog = { stage = "catalog" }
                    )

                }
            }
        }
    }
    }
    }
}

@Composable
private fun TrainingResultStat(symbol: String, value: String, label: String, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(symbol, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(value, color = tint, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = ScreenMuted, fontSize = 12.sp)
    }
}
