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
import com.example.automatesmartbot.data.career.TrainingCourses
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

@Composable
private fun TrainingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
        colors = ButtonDefaults.buttonColors(containerColor = orangeTraining)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
fun JobTrainingScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("career_training", 0) }
    var courseId by rememberSaveable { mutableStateOf(TrainingCourses.all.first().id) }
    val course = TrainingCourses.all.first { it.id == courseId }
    var stage by rememberSaveable { mutableStateOf("catalog") }
    var module by rememberSaveable { mutableIntStateOf(0) }
    var questionIndex by rememberSaveable { mutableIntStateOf(0) }
    var version by remember { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var message by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<JSONObject?>(null) }
    val completed = remember(courseId, version) { prefs.getStringSet("done_$courseId", emptySet()).orEmpty().toSet() }
    val answers = remember(courseId, version) { JSONArray(prefs.getString("answers_$courseId", "[]")) }
    val allDone = course.lessons.indices.all { it.toString() in completed }
    fun choose(id: String) { courseId = id; module = 0; stage = "modules"; message = ""; certificate = null }
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
    Column(Modifier.fillMaxSize().background(paleTraining)) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stage != "catalog") TextButton(onClick = {
                stage = when (stage) {
                    "modules" -> "catalog"
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
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEDB)), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Siapkan Karier Impianmu Lewat Pelatihan Terarah", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                            listOf("✓ Materi praktis", "✓ Latihan & evaluasi", "✓ Sertifikat penyelesaian KarirMate").forEach {
                                Text(it, color = tealTraining, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("Pilih Kategori Pelatihan", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    TrainingCourses.all.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                Card(
                                    modifier = Modifier.weight(1f).height(120.dp).clickable { choose(item.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("▣", color = orangeTraining, fontSize = 28.sp)
                                        Text(item.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 13.sp)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Sertifikat penyelesaian adalah bukti latihan internal, bukan sertifikasi kompetensi resmi BNSP.", fontSize = 12.sp)
                    OutlinedButton(onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Riwayat Sertifikat") }
                }
                "modules" -> {
                    Text(course.title, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                    TrainingCard {
                        Text("Pelajari materi dan selesaikan seluruh modul", fontWeight = FontWeight.Bold)
                        Text("${course.lessons.size} Modul  •  Evaluasi Akhir  •  Sertifikat")
                        LinearProgressIndicator(
                            progress = { completed.size.toFloat() / course.lessons.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth(),
                            color = orangeTraining
                        )
                    }
                    Text("Daftar Modul", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    course.lessons.forEachIndexed { i, lesson ->
                        val done = i.toString() in completed
                        val unlocked = i == 0 || (i - 1).toString() in completed
                        Card(
                            onClick = { if (unlocked) { module = i; stage = "lesson" } },
                            enabled = unlocked,
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (done) "✓" else if (unlocked) "▶" else "🔒", color = if (done) tealTraining else orangeTraining)
                                Spacer(Modifier.width(12.dp))
                                Text(lesson.first, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                Text(if (done) "Selesai" else if (unlocked) "Mulai" else "Terkunci", fontSize = 11.sp)
                            }
                        }
                    }
                    TrainingAction("Evaluasi Akhir →", enabled = allDone) { stage = "exam"; questionIndex = 0 }
                }
                "lesson" -> {
                    Text("Modul ${module + 1}", color = orangeTraining, fontWeight = FontWeight.Bold)
                    Text(course.lessons[module].first, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                    Text("Latihan Praktik", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Modul ${module + 1} – ${course.lessons[module].first}")
                    TrainingCard {
                        Text("🏠  Studi Kasus", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Berikan contoh nyata bagaimana materi ini diterapkan dalam pekerjaan atau saat melamar kerja.")
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
                        label = { Text("Jawaban latihan praktik") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    TrainingAction("Kumpulkan Jawaban", enabled = practice.trim().length >= 20) { completeModule() }
                }
                "exam" -> {
                    Text("Evaluasi Akhir", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(course.title)
                    Text("Soal ${questionIndex + 1} dari ${course.questions.size}", fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { (questionIndex + 1f) / course.questions.size.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        color = orangeTraining
                    )
                    val q = course.questions[questionIndex]
                    TrainingCard {
                        Text("${questionIndex + 1}. ${q.prompt}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        q.options.forEachIndexed { index, option ->
                            val selected = answers.optInt(questionIndex, -1) == index
                            Card(
                                onClick = { saveAnswer(questionIndex, index) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFEAD7) else Color.White),
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
                        OutlinedButton(
                            onClick = { questionIndex-- },
                            enabled = questionIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("‹ Sebelumnya") }
                        Button(
                            onClick = {
                                if (questionIndex < course.questions.lastIndex) questionIndex++
                                else stage = "submit"
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = orangeTraining)
                        ) { Text(if (questionIndex == course.questions.lastIndex) "Selesai" else "Berikutnya ›") }
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
                    OutlinedButton(onClick = { stage = "exam"; questionIndex = 0 }) { Text("Ulangi Soal") }
                }
                "certificate" -> {
                    Text("Sertifikat Penyelesaian", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("✓ Selamat! Kamu telah menyelesaikan pelatihan.", color = tealTraining, fontWeight = FontWeight.Bold)
                    certificate?.let { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(2.dp, Color(0xFFFFB447)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF5)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("▣ KarirMate", color = orangeTraining, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("🏅", fontSize = 48.sp)
                                Text("SERTIFIKAT", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = darkTraining)
                                Text("PENYELESAIAN PELATIHAN", fontWeight = FontWeight.Bold)
                                Text("Diberikan kepada")
                                Text(item.optString("name"), fontSize = 23.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                HorizontalDivider()
                                Text("Telah menyelesaikan pelatihan")
                                Text(item.optString("course"), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("Dengan hasil akhir ${item.optInt("score")}/100")
                                Text("Tanggal: ${item.optString("date")}", fontSize = 12.sp)
                                Text("ID: ${item.optString("id")}", fontSize = 10.sp)
                                Text("Sertifikat Pelatihan Internal KarirMate. Bukan sertifikasi kompetensi resmi (BNSP).", fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                        val pdfText = "SERTIFIKAT PENYELESAIAN PELATIHAN\\n\\nKarirMate\\n\\nDiberikan kepada: ${item.optString("name")}\\n\\nTelah menyelesaikan pelatihan: ${item.optString("course")}\\n\\nHasil akhir: ${item.optInt("score")}/100\\nTanggal: ${item.optString("date")}\\nID: ${item.optString("id")}\\n\\nSertifikat internal KarirMate, bukan sertifikasi profesi/BNSP."
                        TrainingCertificateExport(item)
                    }
                    OutlinedButton(onClick = { stage = "history" }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Riwayat Sertifikat") }
                }
                "history" -> {
                    Text("Riwayat Sertifikat", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    val history = JSONArray(prefs.getString("certificates", "[]"))
                    if (history.length() == 0) Text("Belum ada sertifikat.")
                    (history.length() - 1 downTo 0).forEach { i ->
                        val item = history.getJSONObject(i)
                        TrainingCard {
                            Text(item.optString("course"), fontWeight = FontWeight.Bold)
                            Text("${item.optString("name")} • ${item.optString("date")}")
                            TextButton(onClick = { certificate = item; stage = "certificate" }) { Text("Buka Sertifikat") }
                        }
                    }
                    OutlinedButton(onClick = { stage = "catalog" }) { Text("Kembali ke Katalog") }
                }
            }
        }
    }
}
