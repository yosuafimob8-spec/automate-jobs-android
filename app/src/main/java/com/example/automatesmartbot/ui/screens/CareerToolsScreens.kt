package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.career.*
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@Composable
fun SalaryCalculatorScreen() {
    var gross by rememberSaveable { mutableStateOf("") }
    var base by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var health by rememberSaveable { mutableStateOf("") }
    var jht by rememberSaveable { mutableStateOf("") }
    var pension by rememberSaveable { mutableStateOf("0") }
    var ptkp by rememberSaveable { mutableStateOf("54000000") }
    var months by rememberSaveable { mutableStateOf("12") }
    var day by rememberSaveable { mutableStateOf(PayrollCalculator.OvertimeDay.WORKDAY.name) }
    var result by remember { mutableStateOf<PayrollCalculator.Result?>(null) }
    var error by remember { mutableStateOf("") }
    val rupiah = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Kalkulator Gaji & Hak Kerja", style = MaterialTheme.typography.titleLarge)
        Text("Estimasi PPh 21 tahunan dengan lapisan tarif UU HPP; angka per bulan adalah rata-rata tahunan, bukan potongan TER pada slip gaji. Periksa rekonsiliasi aktual dengan pemberi kerja.")
        AppInput("Penghasilan bruto pajak per bulan (Rp)", gross, { gross = it; result = null })
        AppInput("Upah dasar lembur / THR (Rp)", base, { base = it; result = null })
        AppInput("Jam lembur dalam satu hari", hours, { hours = it; result = null })
        listOf(PayrollCalculator.OvertimeDay.WORKDAY to "Hari kerja", PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK to "Istirahat, sistem 5 hari", PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK to "Istirahat, sistem 6 hari", PayrollCalculator.OvertimeDay.SHORT_DAY_REST to "Istirahat jatuh pada hari terpendek (6 hari)").forEach { (value,label) ->
            Row { RadioButton(day == value.name, { day = value.name; result = null }); Text(label, Modifier.padding(top = 12.dp)) }
        }
        Text("Lembur memakai dasar 1/173 dan faktor PP 35/2021. Input untuk satu hari; batas mingguan dan ketentuan khusus perlu diperiksa terpisah.")
        AppInput("Dasar iuran BPJS Kesehatan (0 jika tidak ikut)", health, { health = it; result = null })
        AppInput("Dasar iuran JHT (0 jika tidak ikut)", jht, { jht = it; result = null })
        AppInput("Dasar iuran JP setelah plafon yang berlaku (0 jika tidak ikut)", pension, { pension = it; result = null })
        Text("Porsi pekerja: kesehatan 1% (dasar maksimum Rp12 juta), JHT 2%, JP 1%. Masukkan dasar JP aktual pada slip; plafon tahunan tidak ditebak aplikasi.")
        AppInput("PTKP tahunan sesuai status pajak (Rp)", ptkp, { ptkp = it; result = null })
        AppInput("Masa kerja untuk THR (bulan penuh)", months, { months = it; result = null })
        Button(onClick = {
            try {
                fun number(s: String) = s.trim().toLongOrNull() ?: kotlin.error("Isi angka rupiah tanpa titik/koma.")
                result = PayrollCalculator.calculate(PayrollCalculator.Input(number(gross),number(base),hours.toIntOrNull() ?: kotlin.error("Jam tidak valid."),PayrollCalculator.OvertimeDay.valueOf(day),number(health),number(jht),number(pension),number(ptkp),months.toIntOrNull() ?: kotlin.error("Masa kerja tidak valid."),number(base)))
                error = ""
            } catch (e: Exception) { error = e.message ?: "Input tidak valid."; result = null }
        }) { Text("Hitung") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        result?.let { r ->
            listOf("Upah lembur" to r.overtime,"BPJS Kesehatan pekerja" to r.health,"JHT pekerja" to r.jht,"JP pekerja" to r.pension,"THR bruto" to r.thr,"Estimasi PPh 21 setahun (termasuk THR)" to r.annualTax,"Rata-rata PPh 21 per bulan" to r.monthlyTaxEstimate,"Estimasi diterima bulan ini (tanpa THR)" to r.monthlyTakeHomeEstimate).forEach { (label,value) -> Text("$label: ${rupiah.format(value)}") }
            Text("THR: masa kerja ≥12 bulan satu bulan upah; 1–<12 bulan proporsional. Estimasi ini memakai pendapatan bulanan tetap, lembur input satu kali, dan THR satu kali; pendapatan lain perlu dimasukkan dalam perhitungan pajak aktual.")
        }
    }
}

@Composable
fun CareerLettersScreen(onApplicationLetter: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("career_resignation", 0) }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var company by rememberSaveable { mutableStateOf(prefs.getString("company", "").orEmpty()) }
    var position by rememberSaveable { mutableStateOf(prefs.getString("position", "").orEmpty()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var lastDay by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf(prefs.getString("content", "").orEmpty()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(name,company,position,content) { prefs.edit().putString("name",name).putString("company",company).putString("position",position).putString("content",content).apply() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Surat Lamaran & Resign", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onApplicationLetter, Modifier.fillMaxWidth()) { Text("Buat Surat Lamaran dari CV") }
        HorizontalDivider()
        Text("Surat Pengunduran Diri", style = MaterialTheme.typography.titleMedium)
        AppInput("Nama lengkap",name,{name=it})
        AppInput("Perusahaan saat ini",company,{company=it})
        AppInput("Jabatan saat ini",position,{position=it})
        AppInput("Tanggal surat (YYYY-MM-DD)",date,{date=it})
        AppInput("Usulan hari kerja terakhir (YYYY-MM-DD)",lastDay,{lastDay=it})
        AppInput("Alasan (opsional)",reason,{reason=it})
        Button(onClick = { try { content=CareerLetters.resignation(name,company,position,LocalDate.parse(date),LocalDate.parse(lastDay),reason);error="";preview=false } catch(e:Exception){error=e.message ?: "Periksa isian."} }) { Text("Buat Draf Resign") }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
        if(content.isNotBlank()) {
            AppInput("Isi surat (tersimpan otomatis)",content,{content=it;preview=false},minLines=12)
            TextButton(onClick={preview=!preview}) { Text(if(preview) "Tutup Preview" else "Preview A4") }
            val pages=remember(content){CvRenderer.letterPages(content)}
            if(preview) CvDocumentPreview(pages,Modifier.height(560.dp).fillMaxWidth())
            PdfExportButton(pages,"KarirMate-Surat-Resign.pdf")
            Text("Tanggal merupakan usulan; sesuaikan masa pemberitahuan dengan perjanjian dan ketentuan yang berlaku.")
        }
    }
}

@Composable
fun JobTrainingScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("career_training", 0) }
    var courseId by rememberSaveable { mutableStateOf(TrainingCourses.all.first().id) }
    val course = TrainingCourses.all.first { it.id == courseId }
    var name by rememberSaveable { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var page by rememberSaveable(courseId) { mutableIntStateOf(-1) }
    var version by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    var certificate by remember { mutableStateOf<JSONObject?>(null) }
    val done = remember(courseId, version) {
        prefs.getStringSet("done_$courseId", emptySet()).orEmpty().toSet()
    }
    val answers = remember(courseId, version) {
        JSONArray(prefs.getString("answers_$courseId", "[]"))
    }
    val allDone = course.lessons.indices.all { it.toString() in done }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Akademi KarirMate", style = MaterialTheme.typography.headlineSmall)
        Text("Belajar bertahap • latihan praktik • evaluasi • sertifikat penyelesaian")
        Text("Sertifikat hanya membuktikan penyelesaian latihan internal KarirMate, bukan sertifikasi profesi/BNSP.")
        if (page == -1) {
            Text("Katalog Pelatihan", style = MaterialTheme.typography.titleLarge)
            TrainingCourses.all.forEach { item ->
                val completed = prefs.getStringSet("done_${item.id}", emptySet()).orEmpty().size
                Card(onClick = { courseId = item.id; page = 0; message = ""; certificate = null },
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text("$completed/${item.lessons.size} modul selesai")
                        LinearProgressIndicator(
                            progress = { (completed.toFloat() / item.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = { page = -1; certificate = null }) { Text("← Katalog Pelatihan") }
            Text(course.title, style = MaterialTheme.typography.titleLarge)
            Text("${done.size} dari ${course.lessons.size} modul diselesaikan")
            LinearProgressIndicator(
                progress = { (done.size.toFloat() / course.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            course.lessons.forEachIndexed { index, lesson ->
                OutlinedButton(
                    onClick = { page = index; message = "" },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("${if (index.toString() in done) "✓" else "○"} Modul ${index + 1}: ${lesson.first}") }
            }
            if (page in course.lessons.indices) {
                val lesson = course.lessons[page]
                HorizontalDivider()
                Text(lesson.first, style = MaterialTheme.typography.titleLarge)
                Text(lesson.second)
                Text("Latihan praktik", style = MaterialTheme.typography.titleMedium)
                Text("Tuliskan satu contoh penerapan materi ini dalam pekerjaan atau proses melamar kerja.")
                var practice by rememberSaveable(courseId, page) {
                    mutableStateOf(prefs.getString("practice_${courseId}_$page", "").orEmpty())
                }
                OutlinedTextField(
                    value = practice,
                    onValueChange = {
                        practice = it
                        prefs.edit().putString("practice_${courseId}_$page", it).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Jawaban latihan praktik") }
                )
                Button(
                    enabled = practice.trim().length >= 20,
                    onClick = {
                        prefs.edit().putStringSet("done_$courseId", done + page.toString()).apply()
                        version++
                        message = "Modul selesai. Lanjutkan ke modul berikutnya."
                        if (page < course.lessons.lastIndex) page++
                        else page = course.lessons.size
                    }
                ) { Text("Selesaikan Modul") }
            }
            if (page == course.lessons.size) {
                HorizontalDivider()
                Text("Evaluasi Akhir • Minimal 80/100", style = MaterialTheme.typography.titleLarge)
                if (!allDone) Text("Selesaikan seluruh modul dan latihan praktik sebelum mengikuti evaluasi.")
                if (allDone) {
                    AppInput("Nama pada sertifikat", name, {
                        name = it
                        prefs.edit().putString("name", it).apply()
                    })
                    course.questions.forEachIndexed { index, question ->
                        Text("${index + 1}. ${question.prompt}")
                        question.options.forEachIndexed { optionIndex, option ->
                            Row(
                                Modifier.fillMaxWidth().selectable(
                                    selected = answers.optInt(index, -1) == optionIndex,
                                    role = Role.RadioButton,
                                    onClick = {
                                        answers.put(index, optionIndex)
                                        prefs.edit().putString("answers_$courseId", answers.toString()).apply()
                                        version++
                                    }
                                )
                            ) {
                                RadioButton(answers.optInt(index, -1) == optionIndex, null)
                                Text(option, Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                    Button(onClick = {
                        val selected = course.questions.indices.map { answers.optInt(it, -1) }
                        when {
                            name.isBlank() -> message = "Isi nama peserta."
                            selected.any { it < 0 } -> message = "Jawab semua soal."
                            else -> {
                                val score = TrainingCourses.score(selected, course.questions)
                                message = "Nilai evaluasi: $score/100. " +
                                    if (score >= 80) "Lulus." else "Belum lulus. Ulangi materi."
                                if (score >= 80) {
                                    val existing = JSONArray(prefs.getString("certificates", "[]"))
                                    val item = JSONObject()
                                        .put("id", UUID.randomUUID().toString())
                                        .put("courseId", courseId)
                                        .put("name", name.trim())
                                        .put("course", course.title)
                                        .put("date", LocalDate.now().toString())
                                        .put("score", score)
                                    existing.put(item)
                                    prefs.edit().putString("certificates", existing.toString()).apply()
                                    certificate = item
                                }
                            }
                        }
                    }) { Text("Periksa Hasil & Sertifikat") }
                }
            }
        }
        if (message.isNotBlank()) Text(message)
        certificate?.let { item ->
            val certificateText = "SERTIFIKAT PENYELESAIAN PELATIHAN\\n\\n" +
                "KarirMate\\n\\nDiberikan kepada: ${item.optString("name")}\\n" +
                "Pelatihan: ${item.optString("course")}\\n" +
                "Nilai: ${item.optInt("score")}/100\\nTanggal: ${item.optString("date")}\\n" +
                "ID: ${item.optString("id")}\\n\\n" +
                "Bukti pelatihan internal KarirMate, bukan sertifikasi profesi/BNSP."
            PdfExportButton(
                CvRenderer.letterPages(certificateText),
                "KarirMate-Sertifikat-Pelatihan.pdf",
                "Ekspor Sertifikat PDF"
            )
        }
        HorizontalDivider()
        Text("Riwayat Sertifikat", style = MaterialTheme.typography.titleMedium)
        val history = JSONArray(prefs.getString("certificates", "[]"))
        (history.length() - 1 downTo 0).forEach { index ->
            val item = history.getJSONObject(index)
            TextButton(onClick = { certificate = item }) {
                Text("${item.optString("course")} • ${item.optString("name")} • ${item.optString("date")}")
            }
        }
    }
}
