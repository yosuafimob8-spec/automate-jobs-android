package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import com.example.automatesmartbot.ui.theme.karirButtonColors
import com.example.automatesmartbot.ui.theme.ScreenAccent as KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.ScreenPageGradient as KarirPageGradient
import com.example.automatesmartbot.ui.theme.ScreenText as KarirTextDark
import com.example.automatesmartbot.ui.theme.ScreenMuted as KarirTextMuted
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

// Revision #9: UI-only payroll calculator. Financial estimates use the existing PayrollCalculator.
private val salaryBlue = Color(0xFF0D6EBB)

@Composable
private fun SalaryCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFD0E2F0))),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SalaryNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondary = screenCanvas(Color(0xFF58718B), Color(0xFFB5CCDE))
    val fieldBg = screenCanvas(Color(0xFFFFFFFF), Color(0xFF263950))
    val cardBorder = screenCanvas(Color(0xFFBFDAEC), Color(0xFF496176))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        OutlinedTextField(
            value = value,
            onValueChange = { entered ->
                // Monetary amounts may be written as 5000000 or 5.000.000; never accept a sign.
                if (entered.length <= 17 && entered.all { it.isDigit() || it == '.' || it == ' ' }) onChange(entered)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            placeholder = { Text("0", color = secondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = textColor,
                unfocusedLabelColor = secondary,
                cursorColor = salaryBlue,
                focusedBorderColor = salaryBlue,
                unfocusedBorderColor = cardBorder,
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg
            )
        )
        if (hint.isNotBlank()) Text(hint, color = secondary, fontSize = 11.sp)
    }
}

@Composable
private fun SalaryAmountRow(label: String, value: String, emphasize: Boolean = false) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = textColor, fontSize = 12.sp)
        Text(
            value, color = if (emphasize) screenCanvas(salaryBlue, Color(0xFF78CDFF)) else textColor,
            fontSize = 12.sp, fontWeight = if (emphasize) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
fun SalaryCalculatorScreen() {
    var gross by rememberSaveable { mutableStateOf("") }
    var base by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var health by rememberSaveable { mutableStateOf("") }
    var jht by rememberSaveable { mutableStateOf("") }
    var pension by rememberSaveable { mutableStateOf("") }
    var ptkp by rememberSaveable { mutableStateOf("54000000") }
    var months by rememberSaveable { mutableStateOf("12") }
    var day by rememberSaveable { mutableStateOf(PayrollCalculator.OvertimeDay.WORKDAY.name) }
    var advancedVisible by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<PayrollCalculator.Result?>(null) }
    var error by remember { mutableStateOf("") }
    val rupiah = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = screenCanvas(Color(0xFF58718B), Color(0xFFB5CCDE))

    fun amount(raw: String, required: Boolean = false): Long {
        val clean = raw.replace(".", "").replace(" ", "")
        require(clean.isNotEmpty() || !required) { "Isi nominal penghasilan dan dasar lembur terlebih dahulu." }
        return if (clean.isEmpty()) 0L else clean.toLongOrNull() ?: kotlin.error("Nominal terlalu besar atau tidak valid.")
    }
    fun calculateEstimate() {
        try {
            result = PayrollCalculator.calculate(
                PayrollCalculator.Input(
                    monthlyGross = amount(gross, required = true),
                    overtimeBase = amount(base, required = true),
                    overtimeHours = hours.replace(" ", "").toIntOrNull() ?: kotlin.error("Jam lembur harus berupa angka."),
                    overtimeDay = PayrollCalculator.OvertimeDay.valueOf(day),
                    bpjsHealthBase = amount(health),
                    jhtBase = amount(jht),
                    pensionBase = amount(pension),
                    ptkp = amount(ptkp),
                    serviceMonths = months.toIntOrNull() ?: kotlin.error("Masa kerja harus berupa angka."),
                    thrBase = amount(base, required = true)
                )
            )
            error = ""
        } catch (ex: Exception) {
            error = ex.message ?: "Input tidak valid. Periksa nominal dan jenis hari lembur."
            result = null
        }
    }
    fun example() {
        gross = "5000000"
        base = "5000000"
        hours = "2"
        health = "5000000"
        jht = "5000000"
        pension = "5000000"
        ptkp = "54000000"
        months = "12"
        day = PayrollCalculator.OvertimeDay.WORKDAY.name
        advancedVisible = true
        calculateEstimate()
    }
    fun reset() {
        gross = ""
        base = ""
        hours = "0"
        health = ""
        jht = ""
        pension = ""
        ptkp = "54000000"
        months = "12"
        day = PayrollCalculator.OvertimeDay.WORKDAY.name
        advancedVisible = false
        result = null
        error = ""
    }

    Column(
        Modifier.fillMaxSize().background(KarirPageGradient).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Parent app shell provides the only back button and bottom navigation.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = salaryBlue)
        ) {
            Column(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color(0xFF075596), Color(0xFF0A79CD), Color(0xFF22BFB5))
                    )
                ).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🧮 Kalkulator Gaji + Lembur", fontWeight = FontWeight.ExtraBold,
                    fontSize = 23.sp, color = Color.White)
                Text("Hitung estimasi gaji diterima, upah lembur, BPJS, PPh 21 dan THR.",
                    color = Color.White, fontSize = 13.sp)
                Text("Perhitungan simulasi, bukan slip gaji resmi perusahaan.",
                    color = Color(0xFFE1F8FF), fontSize = 11.sp)
            }
        }

        SalaryCard {
            Text("⚡ Simulasi Cepat", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("Coba contoh gaji Rp5 juta dengan lembur 2 jam pada hari kerja.",
                color = muted, fontSize = 12.sp)
            Button(
                onClick = ::example,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = salaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Coba Simulasi Rp5.000.000", color = Color.White, fontWeight = FontWeight.Bold) }
        }

        SalaryCard {
            Text("💼 Komponen Penghasilan", color = ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            SalaryNumberField("Gaji bruto per bulan (Rp)", gross, { gross = it; result = null })
            SalaryNumberField("Dasar perhitungan lembur dan THR (Rp)", base, { base = it; result = null },
                "Gunakan dasar upah sesuai komponen gaji yang berlaku.")
        }

        SalaryCard {
            Text("⏱️ Perhitungan Lembur", color = ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            SalaryNumberField("Total jam lembur dalam satu hari", hours, { hours = it; result = null })
            Text("Jenis hari lembur", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                PayrollCalculator.OvertimeDay.WORKDAY to "Hari kerja biasa",
                PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK to "Hari istirahat (5 hari kerja/minggu)",
                PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK to "Hari istirahat (6 hari kerja/minggu)",
                PayrollCalculator.OvertimeDay.SHORT_DAY_REST to "Hari istirahat terpendek (6 hari kerja)"
            ).forEach { (option, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = day == option.name,
                        onClick = { day = option.name; result = null },
                        role = Role.RadioButton
                    ).padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = day == option.name, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = salaryBlue)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = ink, fontSize = 12.sp)
                }
            }
        }

        SalaryCard {
            OutlinedButton(
                onClick = { advancedVisible = !advancedVisible },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, salaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (advancedVisible) "Sembunyikan BPJS, Pajak & THR  ▲" else "Atur BPJS, Pajak & THR  ▼",
                    color = ink, fontWeight = FontWeight.Bold)
            }
            if (advancedVisible) {
                Text("Potongan BPJS & komponen pajak", color = ink, fontWeight = FontWeight.Bold)
                SalaryNumberField("Dasar iuran BPJS Kesehatan (Rp)", health, { health = it; result = null }, "Kosong atau 0 = tidak dihitung")
                SalaryNumberField("Dasar iuran JHT (Rp)", jht, { jht = it; result = null }, "Kosong atau 0 = tidak dihitung")
                SalaryNumberField("Dasar iuran Jaminan Pensiun (Rp)", pension, { pension = it; result = null }, "Isi dasar yang sudah memperhitungkan batas upah")
                SalaryNumberField("PTKP tahunan (Rp)", ptkp, { ptkp = it; result = null })
                SalaryNumberField("Masa kerja untuk THR (bulan)", months, { months = it; result = null })
            } else {
                Text("Buka untuk mengisi dasar BPJS, PTKP, dan masa kerja THR. Nilai kosong BPJS dianggap nol.",
                    color = muted, fontSize = 12.sp)
            }
        }

        Button(
            onClick = ::calculateEstimate,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = salaryBlue)
        ) { Text("Hitung Estimasi Gaji", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        if (error.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = screenCanvas(Color(0xFFFFEDED), Color(0xFF4A252A))
            ) { Text(error, Modifier.padding(12.dp), color = screenCanvas(Color(0xFF9B1C1C), Color(0xFFFFCBCB)), fontSize = 12.sp) }
        }

        result?.let { r ->
            SalaryCard {
                Text("✅ Hasil Estimasi", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = screenCanvas(Color(0xFFE7F3FD), Color(0xFF1B4264))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("PERKIRAAN GAJI DITERIMA", color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(rupiah.format(r.monthlyTakeHomeEstimate), color = ink,
                            fontWeight = FontWeight.ExtraBold, fontSize = 27.sp)
                        Text("Perhitungan estimasi bulanan, tidak termasuk pencairan THR.",
                            fontSize = 11.sp, color = muted)
                    }
                }
                SalaryAmountRow("Upah lembur", rupiah.format(r.overtime))
                SalaryAmountRow("Potongan BPJS Kesehatan", rupiah.format(r.health))
                SalaryAmountRow("Potongan JHT", rupiah.format(r.jht))
                SalaryAmountRow("Potongan JP", rupiah.format(r.pension))
                SalaryAmountRow("Estimasi PPh 21 bulanan (rata-rata)", rupiah.format(r.monthlyTaxEstimate))
                HorizontalDivider(color = screenCanvas(Color(0xFFD0E2F0)))
                SalaryAmountRow("Estimasi PPh 21 tahunan", rupiah.format(r.annualTax))
                SalaryAmountRow("Estimasi THR bruto", rupiah.format(r.thr))
                HorizontalDivider(color = screenCanvas(Color(0xFFD0E2F0)))
                SalaryAmountRow("Gaji diterima per bulan", rupiah.format(r.monthlyTakeHomeEstimate), emphasize = true)
                Text("PPh 21 menggunakan estimasi tahunan yang dirata-ratakan; potongan aktual bulanan dapat berbeda karena metode TER dan kebijakan penggajian.",
                    color = muted, fontSize = 11.sp)
            }
        }
        OutlinedButton(onClick = ::reset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Atur Ulang", color = ink)
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
            .background(KarirPageGradient)
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
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Surat Lamaran Otomatis", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)
                    Text("Gunakan profil dan CV tersimpan untuk menyusun surat lamaran otomatis.", fontSize = 12.sp, color = KarirTextMuted)
                    Button(
                        onClick = onApplicationLetter,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = karirButtonColors()
                    ) {
                        Text("Buat Surat Lamaran Sekarang", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Surat Pengunduran Diri (Resign)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KarirTextDark)

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
                        colors = karirButtonColors()
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
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
            .background(KarirPageGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Akademi KarirMate", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = KarirTextDark)
            Text("Belajar bertahap • latihan praktik • evaluasi • sertifikat penyelesaian", color = KarirTextMuted, fontSize = 12.sp)

            if (page == -1) {
                Text("Katalog Pelatihan", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = KarirTextDark)
                TrainingCourses.all.forEach { item ->
                    val completed = prefs.getStringSet("done_${item.id}", emptySet()).orEmpty().size
                    Card(
                        onClick = { courseId = item.id; page = 0; message = ""; certificate = null },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KarirTextDark)
                            Text("$completed/${item.lessons.size} modul selesai", fontSize = 12.sp, color = KarirTextMuted)
                            LinearProgressIndicator(
                                progress = { (completed.toFloat() / item.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = KarirOrange,
                                trackColor = Color(0xFFC6DFE8)
                            )
                        }
                    }
                }
            } else {
                TextButton(onClick = { page = -1; certificate = null }) {
                    Text("← Kembali ke Katalog Pelatihan", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                }
                Text(course.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KarirTextDark)
                Text("${done.size} dari ${course.lessons.size} modul diselesaikan", fontSize = 12.sp, color = KarirTextMuted)
                LinearProgressIndicator(
                    progress = { (done.size.toFloat() / course.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = KarirOrange,
                    trackColor = Color(0xFFC6DFE8)
                )

                course.lessons.forEachIndexed { index, lesson ->
                    OutlinedButton(
                        onClick = { page = index; message = "" },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${if (index.toString() in done) "✓" else "○"} Modul ${index + 1}: ${lesson.first}",
                            color = KarirTextDark,
                            fontSize = 12.5.sp
                        )
                    }
                }

                if (page in course.lessons.indices) {
                    val lesson = course.lessons[page]
                    HorizontalDivider(color = Color(0xFFC6DFE8))
                    Text(lesson.first, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = KarirTextDark)
                    Text(lesson.second, fontSize = 13.sp, color = KarirTextDark)
                    Text("Latihan praktik", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KarirTextDark)
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
                            unfocusedBorderColor = Color(0xFFC6DFE8),
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
                    HorizontalDivider(color = Color(0xFFC6DFE8))
                    Text("Evaluasi Akhir • Minimal 80/100", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = KarirTextDark)
                    if (!allDone) {
                        Text("Selesaikan seluruh modul dan latihan praktik sebelum mengikuti evaluasi.", fontSize = 12.sp, color = KarirTextMuted)
                    }
                    if (allDone) {
                        AppInput("Nama pada sertifikat", name, {
                            name = it
                            prefs.edit().putString("name", it).apply()
                        })
                        course.questions.forEachIndexed { index, question ->
                            Text("${index + 1}. ${question.prompt}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KarirTextDark)
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
                                    Text(option, fontSize = 12.sp, color = KarirTextDark)
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
