package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.career.*
import com.example.automatesmartbot.data.cv.*
import com.example.automatesmartbot.ui.components.*
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KarirMateSectionHero("Kalkulator Gaji", "Simulasikan gaji, lembur, BPJS, pajak, dan THR secara transparan.")

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Simulasi Cepat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Coba angka simulasi standar, lalu sesuaikan dengan nominal slip gaji Anda.", fontSize = 12.sp, color = KarirTextMuted)
                    OutlinedButton(
                        onClick = {
                            gross = "5000000"
                            base = "5000000"
                            hours = "2"
                            health = "5000000"
                            jht = "5000000"
                            pension = "5000000"
                            months = "12"
                            result = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, KarirOrange),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFFF7F2),
                            contentColor = KarirOrange
                        )
                    ) {
                        Text("Isi Contoh Rp5 Juta", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Input Komponen Gaji",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Estimasi PPh 21 tahunan mengacu lapisan tarif UU HPP.",
                        fontSize = 11.5.sp,
                        color = KarirTextMuted
                    )

                    AppInput("Penghasilan bruto pajak per bulan (Rp)", gross, { gross = it; result = null })
                    AppInput("Upah dasar lembur / THR (Rp)", base, { base = it; result = null })
                    AppInput("Jam lembur dalam satu hari", hours, { hours = it; result = null })

                    Text("Jenis Hari Lembur:", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                    listOf(
                        PayrollCalculator.OvertimeDay.WORKDAY to "Hari kerja biasa",
                        PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK to "Hari istirahat (minggu 5 hari kerja)",
                        PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK to "Hari istirahat (minggu 6 hari kerja)",
                        PayrollCalculator.OvertimeDay.SHORT_DAY_REST to "Istirahat hari terpendek (minggu 6 hari)"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = day == value.name,
                                onClick = { day = value.name; result = null },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = KarirOrange,
                                    unselectedColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1E5DC))

                    AppInput("Dasar iuran BPJS Kesehatan (0 jika tidak ikut)", health, { health = it; result = null })
                    AppInput("Dasar iuran JHT (0 jika tidak ikut)", jht, { jht = it; result = null })
                    AppInput("Dasar iuran JP setelah plafon (0 jika tidak ikut)", pension, { pension = it; result = null })
                    AppInput("PTKP tahunan sesuai status pajak (Rp)", ptkp, { ptkp = it; result = null })
                    AppInput("Masa kerja untuk THR (bulan penuh)", months, { months = it; result = null })

                    Button(
                        onClick = {
                            try {
                                fun number(s: String) = s.trim().toLongOrNull() ?: kotlin.error("Isi angka rupiah tanpa titik/koma.")
                                result = PayrollCalculator.calculate(
                                    PayrollCalculator.Input(
                                        number(gross),
                                        number(base),
                                        hours.toIntOrNull() ?: kotlin.error("Jam tidak valid."),
                                        PayrollCalculator.OvertimeDay.valueOf(day),
                                        number(health),
                                        number(jht),
                                        number(pension),
                                        number(ptkp),
                                        months.toIntOrNull() ?: kotlin.error("Masa kerja tidak valid."),
                                        number(base)
                                    )
                                )
                                error = ""
                            } catch (e: Exception) {
                                error = e.message ?: "Input tidak valid."
                                result = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("Hitung Estimasi Gaji", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    if (error.isNotBlank()) {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }

            result?.let { r ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3E0),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                "ESTIMASI GAJI DITERIMA (TAKE HOME PAY)",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            rupiah.format(r.monthlyTakeHomeEstimate),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Estimasi per bulan, belum termasuk THR tahunan.", fontSize = 12.sp, color = KarirTextMuted)

                        HorizontalDivider(color = Color(0xFFF1E5DC))

                        listOf(
                            "Upah lembur hari ini" to r.overtime,
                            "BPJS Kesehatan pekerja" to r.health,
                            "JHT pekerja" to r.jht,
                            "JP pekerja" to r.pension,
                            "THR bruto" to r.thr,
                            "Estimasi PPh 21 setahun" to r.annualTax,
                            "Rata-rata PPh 21 bulanan" to r.monthlyTaxEstimate,
                            "Estimasi diterima bulan ini" to r.monthlyTakeHomeEstimate
                        ).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 12.sp, color = KarirTextMuted)
                                Text(rupiah.format(value), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
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

    LaunchedEffect(name, company, position, content) {
        prefs.edit().putString("name", name).putString("company", company).putString("position", position).putString("content", content).apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KarirMateSectionHero("Surat Karier", "Susun surat lamaran resmi dan dokumen pengunduran diri profesional.")

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Surat Lamaran Otomatis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Gunakan profil dan CV tersimpan untuk menyusun surat lamaran otomatis.", fontSize = 12.sp, color = KarirTextMuted)
                    Button(
                        onClick = onApplicationLetter,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("Buat Surat Lamaran Sekarang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Surat Pengunduran Diri (Resign)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                    AppInput("Nama lengkap", name, { name = it })
                    AppInput("Perusahaan saat ini", company, { company = it })
                    AppInput("Jabatan saat ini", position, { position = it })
                    AppInput("Tanggal surat (YYYY-MM-DD)", date, { date = it })
                    AppInput("Usulan hari kerja terakhir (YYYY-MM-DD)", lastDay, { lastDay = it })
                    AppInput("Alasan (opsional)", reason, { reason = it })

                    Button(
                        onClick = {
                            try {
                                content = CareerLetters.resignation(name, company, position, LocalDate.parse(date), LocalDate.parse(lastDay), reason)
                                error = ""
                                preview = false
                            } catch (e: Exception) {
                                error = e.message ?: "Periksa isian tanggal dan format."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text("Buat Draf Resign", fontWeight = FontWeight.Bold)
                    }

                    if (error.isNotBlank()) {
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    if (content.isNotBlank()) {
                        AppInput("Isi surat (tersimpan otomatis)", content, { content = it; preview = false }, minLines = 10)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { preview = !preview },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, KarirOrange),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KarirOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (preview) "Tutup Preview" else "Preview A4", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        val pages = remember(content) { CvRenderer.letterPages(content) }
                        if (preview) {
                            CvDocumentPreview(pages, Modifier.height(540.dp).fillMaxWidth())
                        }
                        PdfExportButton(pages, "KarirMate-Surat-Resign.pdf")
                    }
                }
            }
        }
    }
}

@Composable
fun LegacyJobTrainingScreen() {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Akademi KarirMate", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Belajar bertahap • latihan praktik • evaluasi • sertifikat penyelesaian", color = KarirTextMuted, fontSize = 12.sp)

            if (page == -1) {
                Text("Katalog Pelatihan", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                TrainingCourses.all.forEach { item ->
                    val completed = prefs.getStringSet("done_${item.id}", emptySet()).orEmpty().size
                    Card(
                        onClick = { courseId = item.id; page = 0; message = ""; certificate = null },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("$completed/${item.lessons.size} modul selesai", fontSize = 12.sp, color = KarirTextMuted)
                            LinearProgressIndicator(
                                progress = { (completed.toFloat() / item.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = KarirOrange,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            } else {
                TextButton(onClick = { page = -1; certificate = null }) {
                    Text("← Kembali ke Katalog Pelatihan", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                }
                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("${done.size} dari ${course.lessons.size} modul diselesaikan", fontSize = 12.sp, color = KarirTextMuted)
                LinearProgressIndicator(
                    progress = { (done.size.toFloat() / course.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = KarirOrange,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                course.lessons.forEachIndexed { index, lesson ->
                    OutlinedButton(
                        onClick = { page = index; message = "" },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${if (index.toString() in done) "✓" else "○"} Modul ${index + 1}: ${lesson.first}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp
                        )
                    }
                }

                if (page in course.lessons.indices) {
                    val lesson = course.lessons[page]
                    HorizontalDivider(color = Color(0xFFF1E5DC))
                    Text(lesson.first, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(lesson.second, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Latihan praktik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tuliskan satu contoh penerapan materi ini dalam pekerjaan atau proses melamar kerja.", fontSize = 12.sp, color = KarirTextMuted)

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
                        label = { Text("Jawaban latihan praktik") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KarirOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = KarirOrange
                        )
                    )
                    Button(
                        enabled = practice.trim().length >= 20,
                        onClick = {
                            prefs.edit().putStringSet("done_$courseId", done + page.toString()).apply()
                            version++
                            message = "Modul selesai. Lanjutkan ke modul berikutnya."
                            if (page < course.lessons.lastIndex) page++
                            else page = course.lessons.size
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Selesaikan Modul", fontWeight = FontWeight.Bold)
                    }
                }

                if (page == course.lessons.size) {
                    HorizontalDivider(color = Color(0xFFF1E5DC))
                    Text("Evaluasi Akhir • Minimal 80/100", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (!allDone) {
                        Text("Selesaikan seluruh modul dan latihan praktik sebelum mengikuti evaluasi.", fontSize = 12.sp, color = KarirTextMuted)
                    }
                    if (allDone) {
                        AppInput("Nama pada sertifikat", name, {
                            name = it
                            prefs.edit().putString("name", it).apply()
                        })
                        course.questions.forEachIndexed { index, question ->
                            Text("${index + 1}. ${question.prompt}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = answers.optInt(index, -1) == optionIndex,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = KarirOrange)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(option, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        Button(
                            onClick = {
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Periksa Hasil & Sertifikat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (message.isNotBlank()) Text(message, fontSize = 12.sp, color = KarirOrangeDeep)

            certificate?.let { item ->
                val certificateText = "SERTIFIKAT PENYELESAIAN PELATIHAN\n\n" +
                        "KarirMate\n\nDiberikan kepada: ${item.optString("name")}\n" +
                        "Pelatihan: ${item.optString("course")}\n" +
                        "Nilai: ${item.optInt("score")}/100\nTanggal: ${item.optString("date")}\n" +
                        "ID: ${item.optString("id")}\n\n" +
                        "Bukti pelatihan internal KarirMate, bukan sertifikasi profesi/BNSP."
                PdfExportButton(
                    CvRenderer.letterPages(certificateText),
                    "KarirMate-Sertifikat-Pelatihan.pdf",
                    "Ekspor Sertifikat PDF"
                )
            }
        }
    }
}
