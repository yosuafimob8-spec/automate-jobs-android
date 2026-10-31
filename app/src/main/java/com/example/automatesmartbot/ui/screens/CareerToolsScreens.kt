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
    var thr by rememberSaveable { mutableStateOf("") }
    var hours by rememberSaveable { mutableStateOf("0") }
    var health by rememberSaveable { mutableStateOf("") }
    var jht by rememberSaveable { mutableStateOf("") }
    var pension by rememberSaveable { mutableStateOf("0") }
    var ptkp by rememberSaveable { mutableStateOf("54000000") }
    var months by rememberSaveable { mutableStateOf("12") }
    var day by rememberSaveable { mutableStateOf(PayrollCalculator.OvertimeDay.WORKDAY.name) }
    var result by remember { mutableStateOf<PayrollCalculator.Result?>(null) }
    var error by remember { mutableStateOf("") }
    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val rupiah = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Kalkulator Gaji & Hak Kerja", style = MaterialTheme.typography.titleLarge)
        Text("Kolom bertanda * wajib diisi. Gunakan 0 untuk komponen yang tidak berlaku.")
        Text("Estimasi PPh 21 tahunan dengan lapisan tarif UU HPP; angka per bulan adalah rata-rata tahunan, bukan potongan TER pada slip gaji. Periksa rekonsiliasi aktual dengan pemberi kerja.")
        AppInput("Penghasilan bruto pajak per bulan (Rp) *", gross, { gross = it; result = null }, error = fieldErrors["gross"])
        AppInput("Upah dasar lembur (Rp) *", base, { base = it; result = null }, error = fieldErrors["base"])
        AppInput("Upah dasar THR (Rp) *", thr, { thr = it; result = null }, error = fieldErrors["thr"])
        Text("Dasar lembur dan THR dapat berbeda. Gunakan dasar yang sesuai komponen upah dan ketentuan pada dokumen kerja.")
        AppInput("Jam lembur dalam satu hari *", hours, { hours = it; result = null }, error = fieldErrors["hours"])
        listOf(PayrollCalculator.OvertimeDay.WORKDAY to "Hari kerja", PayrollCalculator.OvertimeDay.REST_FIVE_DAY_WEEK to "Istirahat, sistem 5 hari", PayrollCalculator.OvertimeDay.REST_SIX_DAY_WEEK to "Istirahat, sistem 6 hari", PayrollCalculator.OvertimeDay.SHORT_DAY_REST to "Istirahat jatuh pada hari terpendek (6 hari)").forEach { (value,label) ->
            Row { RadioButton(day == value.name, { day = value.name; result = null }); Text(label, Modifier.padding(top = 12.dp)) }
        }
        Text("Lembur memakai dasar 1/173 dan faktor PP 35/2021. Input untuk satu hari; batas mingguan dan ketentuan khusus perlu diperiksa terpisah.")
        AppInput("Dasar iuran BPJS Kesehatan (0 jika tidak ikut) *", health, { health = it; result = null }, error = fieldErrors["health"])
        AppInput("Dasar iuran JHT (0 jika tidak ikut) *", jht, { jht = it; result = null }, error = fieldErrors["jht"])
        AppInput("Dasar iuran JP setelah plafon yang berlaku (0 jika tidak ikut) *", pension, { pension = it; result = null }, error = fieldErrors["pension"])
        Text("Porsi pekerja: kesehatan 1% (dasar maksimum Rp12 juta), JHT 2%, JP 1%. Masukkan dasar JP aktual pada slip; plafon tahunan tidak ditebak aplikasi.")
        AppInput("PTKP tahunan sesuai status pajak (Rp) *", ptkp, { ptkp = it; result = null }, error = fieldErrors["ptkp"])
        AppInput("Masa kerja untuk THR (bulan penuh) *", months, { months = it; result = null }, error = fieldErrors["months"])
        Button(onClick = {
            val validated = PayrollForm.validate(mapOf("gross" to gross,"base" to base,"thr" to thr,"hours" to hours,"health" to health,"jht" to jht,"pension" to pension,"ptkp" to ptkp,"months" to months), PayrollCalculator.OvertimeDay.valueOf(day))
            fieldErrors = validated.errors
            if (validated.input == null) { error = "Periksa kolom yang ditandai. Input Anda tetap tersimpan."; result = null }
            else try { result = PayrollCalculator.calculate(validated.input); error = "" } catch (e: Exception) { error = e.message ?: "Perhitungan belum dapat dilakukan."; result = null }
        }) { Text("Hitung") }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        PayrollSources()
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
fun LegacyJobTrainingScreen() {
    val context=LocalContext.current
    val prefs=remember{context.getSharedPreferences("career_training",0)}
    var courseId by rememberSaveable { mutableStateOf(TrainingCourses.all.first().id) }
    val course=TrainingCourses.all.first{it.id==courseId}
    var name by rememberSaveable { mutableStateOf(prefs.getString("name","").orEmpty()) }
    var version by remember{mutableIntStateOf(0)}
    var message by remember{mutableStateOf("")}
    var certificate by remember{mutableStateOf<JSONObject?>(null)}
    val answers=remember(courseId,version){JSONArray(prefs.getString("answers_$courseId","[]"))}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Pelatihan Kerja",style=MaterialTheme.typography.titleLarge)
        Text("Modul belajar mandiri KarirMate. Sertifikat adalah bukti penyelesaian latihan internal, bukan sertifikasi profesi/BNSP atau jaminan kerja.")
        TrainingCourses.all.forEach{c->OutlinedButton(onClick={courseId=c.id;certificate=null}){Text(c.title)}}
        AppInput("Nama peserta untuk sertifikat",name,{name=it;prefs.edit().putString("name",it).apply();certificate=null})
        Text(course.title,style=MaterialTheme.typography.titleMedium)
        course.lessons.forEach{(title,body)->Text(title,style=MaterialTheme.typography.titleSmall);Text(body)}
        Text("Evaluasi pemahaman — minimal 80/100",style=MaterialTheme.typography.titleMedium)
        course.questions.forEachIndexed{i,q->
            Text("${i+1}. ${q.prompt}")
            q.options.forEachIndexed{j,option->Row(Modifier.fillMaxWidth().selectable(selected=answers.optInt(i,-1)==j,role=Role.RadioButton,onClick={answers.put(i,j);prefs.edit().putString("answers_$courseId",answers.toString()).apply();version++;certificate=null})){RadioButton(answers.optInt(i,-1)==j,null);Text(option,Modifier.padding(top=12.dp))}}
        }
        Button(onClick={
            val values=course.questions.indices.map{answers.optInt(it,-1).takeIf{n->n>=0}}
            if(values.any{it==null}) message="Jawab semua pertanyaan terlebih dahulu."
            else if(name.isBlank()) message="Isi nama peserta terlebih dahulu."
            else{
                val score=TrainingCourses.score(values,course.questions)
                message="Hasil: $score/100. "+if(score>=80)"Latihan selesai." else "Baca kembali materi dan coba lagi."
                if(score>=80){
                    val item=JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.trim()).put("course",course.title).put("date",LocalDate.now().toString()).put("score",score)
                    val history=JSONArray(prefs.getString("certificates","[]"));history.put(item);prefs.edit().putString("certificates",history.toString()).apply();certificate=item
                }
            }
        }){Text("Nilai & Simpan Penyelesaian")}
        if(message.isNotBlank())Text(message)
        certificate?.let{c->
            val text="SERTIFIKAT PENYELESAIAN LATIHAN\n\nKarirMate\n\nDiberikan kepada:\n${c.getString("name")}\n\nTelah menyelesaikan modul belajar mandiri:\n${c.getString("course")}\n\nNilai evaluasi: ${c.getInt("score")}/100\nTanggal: ${c.getString("date")}\nID: ${c.getString("id")}\n\nBukti latihan internal KarirMate. Bukan sertifikasi profesi/BNSP dan bukan bukti akreditasi."
            PdfExportButton(CvRenderer.letterPages(text),"KarirMate-Sertifikat-Latihan.pdf","Unduh Sertifikat PDF")
        }
        Text("Riwayat penyelesaian",style=MaterialTheme.typography.titleMedium)
        val history=JSONArray(prefs.getString("certificates","[]"))
        (history.length()-1 downTo 0).forEach{i->val c=history.getJSONObject(i);TextButton(onClick={certificate=c}){Text("${c.getString("course")} • ${c.getString("name")} • ${c.getString("date")}")}}
    }
}
