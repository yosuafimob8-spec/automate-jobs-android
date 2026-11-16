package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.R
import com.example.automatesmartbot.data.interview.GuidedTts
import com.example.automatesmartbot.data.interview.analyzeCv
import com.example.automatesmartbot.data.interview.buildExperienceQuestion
import com.example.automatesmartbot.data.interview.buildRandomBankSession
import com.example.automatesmartbot.data.interview.extractCvText
import com.example.automatesmartbot.data.interview.loadHrdScript
import com.example.automatesmartbot.data.interview.loadUniversalBank
import com.example.automatesmartbot.data.interview.redactSensitive
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alur Latihan Terpandu (offline, tanpa server):
 * Setup (CV + posisi + pilih mode) -> Lobby video-call -> Sesi 10 soal HRD
 * atau 8 soal acak dari bank -> Penutup -> Hasil STAR.
 * Dipanggil dari AiInterviewScreen; alur server yang sudah ada tidak diubah.
 */
@Composable
fun GuidedInterviewFlow(onExit: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by rememberSaveable { mutableStateOf("setup") } // setup | lobby | session
    var mode by rememberSaveable { mutableStateOf("hrd") } // hrd | bank
    var role by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var cvText by rememberSaveable { mutableStateOf("") }
    var cvFileName by rememberSaveable { mutableStateOf("") }
    var cvStatus by remember { mutableStateOf("") }
    var cvReading by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var showScript by remember { mutableStateOf(false) }
    var showBank by remember { mutableStateOf(false) }
    var expandedCat by remember { mutableStateOf<Int?>(null) }

    val script = remember { runCatching { loadHrdScript(context) }.getOrNull() }
    val bank = remember { runCatching { loadUniversalBank(context) }.getOrNull() }
    val profile = remember(cvText) {
        if (cvText.isBlank()) null else runCatching { analyzeCv(cvText) }.getOrNull()
    }
    val experienceQuestion = remember(profile) {
        profile?.let { buildExperienceQuestion(it) }
    }

    val cvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            cvReading = true
            error = ""
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) { extractCvText(context, uri) }
                    cvText = redactSensitive(text).take(6000)
                    cvFileName = context.contentResolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null, null, null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: uri.lastPathSegment.orEmpty()
                    cvStatus = "CV berhasil dibaca: ${text.length} karakter."
                } catch (ex: Exception) {
                    error = ex.message ?: "Gagal membaca CV."
                } finally {
                    cvReading = false
                }
            }
        }
    }

    fun buildItems(): List<GuidedItem> {
        return if (mode == "bank" && bank != null) {
            val logistics = (role + " " + company).lowercase().let {
                "logistik" in it || "gudang" in it || "operasional" in it || "warehouse" in it
            }
            buildRandomBankSession(bank, includeLogistics = logistics).map { (title, q) ->
                GuidedItem(title = title, question = q, response = "")
            }
        } else {
            val s = script ?: return emptyList()
            s.questions.map { q ->
                val questionText = if (q.no == 2 && experienceQuestion != null) experienceQuestion else q.question
                GuidedItem(title = q.title, question = questionText, response = q.response)
            }
        }
    }

    val closingText = if (mode == "bank") {
        "Terima kasih sudah mengikuti latihan interview hari ini. Terus berlatih dengan jawaban yang spesifik dan terukur, dan semoga sukses di interview sesungguhnya."
    } else {
        script?.closing
    }

    val ink = KarirTextDark
    val muted = KarirTextMuted

    // The parent AiInterviewScreen already renders InterviewBrandedHeader.
    // A second header here duplicates navigation and consumes the small phone screen.
    BackHandler {
        when (stage) {
            "session" -> stage = "lobby"
            "lobby" -> stage = "setup"
            else -> onExit()
        }
    }
    Column(Modifier.fillMaxSize()) {
        when (stage) {
            "setup" -> {
                Column(
                    Modifier.fillMaxSize().background(com.example.automatesmartbot.ui.theme.KarirPageGradient)
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Latihan Terpandu — Bu Nadia", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                    Text(
                        "Sesi utama: 10 pertanyaan HRD tetap berurutan, tanpa server. " +
                            "Bu Nadia membacakan soal dan respons HRD dengan suara, lalu Anda menjawab dengan suara atau ketikan.",
                        color = muted, fontSize = 13.sp,
                    )

                    // ---- Pilih mode ----
                    Text("Mode Latihan", color = ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = mode == "hrd",
                            onClick = { mode = "hrd" },
                            label = { Text("10 Soal HRD", color = ink) },
                        )
                        FilterChip(
                            selected = mode == "bank",
                            onClick = { mode = "bank" },
                            label = { Text("Acak dari Bank (8 soal)", color = ink) },
                        )
                    }
                    if (mode == "hrd") {
                        Text("Sesi utama 10 soal HRD — Perkenalan, Pengalaman, Alasan Melamar, Alasan Berhenti, Kelebihan/Kekurangan, Tekanan, Kesalahan, Konflik, Gaji, Kesiapan Bergabung.", color = muted, fontSize = 12.sp)
                    } else {
                        Text("Latihan tambahan: 8 soal acak dari bank 128 pertanyaan universal (13 kategori).", color = muted, fontSize = 12.sp)
                    }

                    // ---- Naskah 10 soal (bisa dibuka-tutup) ----
                    if (script != null) {
                        Card(
                            Modifier.fillMaxWidth().clickable { showScript = !showScript },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (showScript) "▾ 10 Pertanyaan Interview & Respons HRD" else "▸ 10 Pertanyaan Interview & Respons HRD",
                                    color = ink, fontWeight = FontWeight.Bold,
                                )
                                Text("Ketuk untuk melihat pertanyaan dan respons HRD persis seperti yang akan dijalankan.", color = muted, fontSize = 12.sp)
                                if (showScript) {
                                    script.questions.forEach { q ->
                                        Text("${q.no}. ${q.title}", color = KarirOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("HRD: ${q.question}", color = ink, fontSize = 13.sp)
                                        Text("Respons: ${q.response}", color = muted, fontSize = 13.sp)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text("Penutup:", color = KarirOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(script.closing, color = ink, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        Text("Naskah 10 soal HRD gagal dimuat. Periksa file assets/interview/hrd_script.json.", color = MaterialTheme.colorScheme.error)
                    }

                    // ---- Posisi & perusahaan ----
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Posisi yang dilamar", color = muted) },
                        placeholder = { Text("cth: Staff Logistik", color = muted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink),
                    )
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Perusahaan target (opsional)", color = muted) },
                        placeholder = { Text("cth: Shopee", color = muted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink),
                    )

                    // ---- Input CV ----
                    Text("CV Anda (opsional)", color = ink, fontWeight = FontWeight.Bold)
                    Text(
                        "Dipakai hanya untuk personalisasi singkat (nama, skill, pengalaman terakhir). " +
                            "Email, nomor HP, alamat lengkap, dan tanggal lahir tidak pernah ditampilkan atau dibacakan.",
                        color = muted, fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !cvReading,
                            onClick = {
                                cvPicker.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "text/plain",
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (cvReading) "Membaca CV…" else "📄 Upload File CV (PDF/DOCX/TXT)") }
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val pasted = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                if (pasted.length >= 30) {
                                    cvText = redactSensitive(pasted).take(6000)
                                    cvFileName = "Tempelan teks"
                                    cvStatus = "CV ditempel dari clipboard: ${cvText.length} karakter."
                                    error = ""
                                } else {
                                    error = "Clipboard kosong atau teks terlalu pendek (minimal 30 karakter)."
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("📋 Tempel dari Clipboard") }
                    }
                    OutlinedTextField(
                        value = cvText,
                        onValueChange = { cvText = it.take(6000) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("Atau tempel teks CV di sini…", color = muted) },
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ink, unfocusedTextColor = ink),
                    )
                    if (cvFileName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ CV: $cvFileName", color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { cvText = ""; cvFileName = ""; cvStatus = "" }) { Text("Hapus CV", color = KarirOrange) }
                        }
                    }
                    if (cvStatus.isNotBlank()) Text(cvStatus, color = ink, fontSize = 13.sp)
                    if (profile != null) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Hasil Analisis CV", color = ink, fontWeight = FontWeight.Bold)
                                if (!profile.name.isNullOrBlank()) Text("Nama: ${profile.name}", color = ink)
                                if (profile.skills.isNotEmpty()) {
                                    Text("Skill terdeteksi (${profile.skills.size}):", color = ink, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        profile.skills.take(4).forEach { skill ->
                                            FilterChip(selected = false, onClick = {}, label = { Text(skill, fontSize = 12.sp) })
                                        }
                                    }
                                }
                                if (experienceQuestion != null) {
                                    Text(
                                        "Soal 2 akan disesuaikan dari pengalaman terakhir Anda (aktivitas + perusahaan saja, tanpa data pribadi).",
                                        color = muted, fontSize = 12.sp,
                                    )
                                }
                                Text("Data sensitif (email, nomor HP, alamat, tanggal lahir) tidak ditampilkan.", color = muted, fontSize = 12.sp)
                            }
                        }
                    }

                    // ---- Bank pertanyaan (bisa dibuka-tutup per kategori) ----
                    if (bank != null) {
                        Card(
                            Modifier.fillMaxWidth().clickable { showBank = !showBank },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (showBank) "▾ Bank Pertanyaan — ${bank.totalQuestions} pertanyaan universal"
                                    else "▸ Bank Pertanyaan — ${bank.totalQuestions} pertanyaan universal",
                                    color = ink, fontWeight = FontWeight.Bold,
                                )
                                Text("Ketuk nama kategorinya untuk melihat semua pertanyaannya.", color = muted, fontSize = 12.sp)
                                if (showBank) {
                                    bank.categories.forEach { cat ->
                                        Text(
                                            "${if (expandedCat == cat.id) "▾" else "▸"} ${cat.id}. ${cat.title} (${cat.questions.size} soal)",
                                            color = ink,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                expandedCat = if (expandedCat == cat.id) null else cat.id
                                            }.padding(vertical = 4.dp),
                                        )
                                        if (expandedCat == cat.id) {
                                            cat.questions.forEachIndexed { i, q ->
                                                Text("${i + 1}. $q", color = ink, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
                                            }
                                            if (cat.followUp.isNotBlank()) {
                                                Text("Follow-up: ${cat.followUp}", color = muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
                                            }
                                            if (cat.sources.isNotEmpty()) {
                                                Text(
                                                    "Sumber: " + cat.sources.joinToString(", ") { it.name },
                                                    color = muted, fontSize = 11.sp,
                                                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "Catatan: bukan daftar resmi Shopee/BCA; wording dinormalisasi agar mudah dibacakan suara.",
                                        color = muted, fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }

                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                    Button(
                        enabled = (mode == "bank" && bank != null) || (mode == "hrd" && script != null),
                        onClick = { stage = "lobby" },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("Masuk Lobby Video Call ➜", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            "lobby" -> {
                GuidedLobby(
                    mode = mode,
                    onEnter = { stage = "session" },
                    onBack = { stage = "setup" },
                )
            }
            "session" -> {
                val items = remember(mode, script, bank, experienceQuestion) { buildItems() }
                if (items.isEmpty()) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Soal tidak dapat dimuat.", color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { stage = "setup" }) { Text("Kembali ke Setup") }
                    }
                } else {
                    GuidedInterviewSession(
                        items = items,
                        closingText = closingText,
                        candidateName = profile?.name,
                        onRestart = { stage = "setup" },
                        onExit = onExit,
                    )
                }
            }
        }
    }
}

/** Lobby video-call: pratinjau Bu Nadia + checklist + tes suara sebelum masuk sesi. */
@Composable
private fun GuidedLobby(mode: String, onEnter: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val tts = remember { GuidedTts(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }
    var testStatus by remember { mutableStateOf("") }
    val ink = KarirTextDark
    val muted = KarirTextMuted
    Column(
        Modifier.fillMaxSize().background(com.example.automatesmartbot.ui.theme.KarirPageGradient)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Lobby Video Call Interview", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
        Text("Periksa kamera, mic, dan suara interviewer sebelum masuk ruang interview.", color = muted, fontSize = 13.sp)
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2231)),
        ) {
            Column(
                Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ai_interview_hr_portrait),
                    contentDescription = "Bu Nadia, HR Recruiter",
                    modifier = Modifier.size(132.dp),
                    contentScale = ContentScale.Crop,
                )
                Text("Bu Nadia", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                Text("HR Recruiter • Siap mewawancarai Anda", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text("🔴 REC • SIMULASI LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        GuidedLobbyRow("📷", "Kamera", "Posisikan wajah di tengah frame, cari cahaya dari depan. Di ruang interview, kamera Anda tampil di pojok layar seperti video call.")
        GuidedLobbyRow("🎙", "Mic & Jawaban Suara", "Anda akan menjawab dengan suara — ucapan diubah jadi teks otomatis untuk dinilai. Headset membuat suara lebih jernih. Jawaban ketik selalu tersedia sebagai cadangan.")
        GuidedLobbyRow("🔊", "Suara Interviewer", "Bu Nadia membacakan pertanyaannya dengan suara. Tes dulu di sini — pastikan volume media HP naik dan mode senyap (silent) mati, karena suara keluar lewat volume media.")
        OutlinedButton(
            onClick = {
                testStatus = "Memutar suara tes…"
                tts.speak("Halo, saya Bu Nadia. Tes suara berhasil. Saya siap mewawancarai Anda hari ini.") {
                    testStatus = "✅ Suara terdengar — Bu Nadia siap membacakan pertanyaan di ruang interview."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("🔊 Putar Suara Tes") }
        if (testStatus.isNotBlank()) Text(testStatus, color = ink, fontSize = 13.sp)
        GuidedLobbyRow(
            "📋",
            "Sesi Anda",
            if (mode == "bank") "Latihan acak dari bank • 8 pertanyaan • Bahasa Indonesia"
            else "10 pertanyaan HRD tetap • Bahasa Indonesia",
        )
        Button(
            onClick = onEnter,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
            shape = RoundedCornerShape(16.dp),
        ) { Text("▶ Masuk Ruang Interview", color = Color.White, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Kembali ke Setup", color = ink)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GuidedLobbyRow(icon: String, title: String, body: String) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = KarirTextDark, fontWeight = FontWeight.Bold)
                Text(body, color = KarirTextMuted, fontSize = 13.sp)
            }
        }
    }
}
