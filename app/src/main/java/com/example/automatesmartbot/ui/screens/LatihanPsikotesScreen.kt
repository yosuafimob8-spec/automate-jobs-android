package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.automatesmartbot.data.psychotest.PsychotestPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.viewmodel.PsychotestViewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.delay

private val orange = com.example.automatesmartbot.ui.theme.ActionOrange
private val teal = Color(0xFF11B9BE)
private val blue = Color(0xFF219CEC)
private val ink = Color(0xFF172744)
private fun JsonObject.s(k: String, fallback: String = "") = get(k)?.takeUnless { it.isJsonNull }?.asString ?: fallback
private fun JsonObject.n(k: String) = runCatching { get(k)?.asInt ?: 0 }.getOrDefault(0)

@Composable
fun LatihanPsikotesScreen(isPro: Boolean, onExit: () -> Unit = {}) {
    MaterialTheme(colorScheme = com.example.automatesmartbot.ui.theme.KarirMateLightColors) {
    Surface(color = Color(0xFFF5F9FC), contentColor = ink) {
    val vm: PsychotestViewModel = viewModel()
    val modes by vm.catalog.collectAsState()
    val questions by vm.questions.collectAsState()
    val session by vm.session.collectAsState()
    val result by vm.result.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val history by vm.history.collectAsState()
    val stats by vm.stats.collectAsState()
    val leaderboard by vm.leaderboard.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val pendingCount by vm.pendingCount.collectAsState()
    var page by rememberSaveable { mutableStateOf("home") }
    var family by remember { mutableStateOf("all") }
    var proNotice by remember { mutableStateOf(false) }
    var freeNotice by remember { mutableStateOf(false) }
    var proResultTab by rememberSaveable { mutableStateOf(0) }
    val basicCategories = listOf("logika", "numerik", "matematika", "verbal", "bahasa", "ketelitian", "deret", "pola", "spasial", "gambar", "kepribadian", "twk", "tiu", "tkp")
    fun basicMode(mode: JsonObject): Boolean {
        val name = mode.s("title").lowercase()
        val code = mode.s("test_id").lowercase()
        return basicCategories.any { it in name || it in code } &&
            !listOf("simulasi", "cat", "skb", "premium", "pro", "full").any { it in name || it in code }
    }
    fun freeEligible(mode: JsonObject): Boolean = mode.s("access_tier") == "free" || (mode.s("access_tier").isBlank() && basicMode(mode))

    LaunchedEffect(isPro) { vm.loadCatalog(); vm.loadExtras(isPro) }
    LaunchedEffect(session, result) {
        if (result != null && (page == "exam" || page == "home")) page = "result"
        else if (session != null && result == null && page == "home") page = "exam"
    }
    fun back() {
        page = when (page) {
            "home" -> { onExit(); "home" }
            "exam" -> "exam"
            else -> "home"
        }
    }
    BackHandler { back() }
    if (proNotice) AlertDialog(onDismissRequest = { proNotice = false },
        title = { Text("Fitur PRO") },
        text = { Text("Simulasi lengkap, statistik, dan peringkat memerlukan langganan PRO yang aktif. Status akses mengikuti langganan yang diverifikasi server.") },
        confirmButton = { TextButton(onClick = { proNotice = false }) { Text("Mengerti") } })
    if (freeNotice) AlertDialog(onDismissRequest = { freeNotice = false },
        title = { Text("Latihan FREE") },
        text = { Text("Paket FREE hanya mencakup kategori dasar dengan 10–20 soal per sesi. Paket ini belum tersedia di server; silakan pilih kategori lain.") },
        confirmButton = { TextButton(onClick = { freeNotice = false }) { Text("Mengerti") } })
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F9FC))) {
        // The parent dashboard already draws the app toolbar; do not duplicate it.
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = teal)
        error?.let { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }
        when (page) {
            "home" -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F8F7))) {
                        Row(Modifier.fillMaxWidth().height(122.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🧠 Psikotes", color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Latihan hari ini, siap menghadapi tes kerja besok!", color = ink, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("🎯", style = MaterialTheme.typography.displayLarge)
                        }
                    }
                }
                item { CompactRoute("💼", "Tes Kerja Swasta", "Logika, numerik, verbal, dan kemampuan kerja", listOf(orange, Color(0xFFFFA168))) { family = "private"; page = "catalog" } }
                item { CompactRoute("🏛️", "Persiapan CPNS", "TWK, TIU, TKP, simulasi CAT, dan SKB", listOf(blue, teal)) { family = "cpns"; page = "catalog" } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Shortcut("🕘 Riwayat", Modifier.weight(1f)) { vm.loadExtras(isPro); page = "history" }
                        Shortcut("📊 Statistik 👑", Modifier.weight(1f)) { if (isPro) { vm.loadExtras(isPro); page = "stats" } else proNotice = true }
                        Shortcut("🏆 Peringkat 👑", Modifier.weight(1f)) { if (isPro) { vm.loadExtras(isPro); page = "leaderboard" } else proNotice = true }
                    }
                }
                item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Latihan Gratis", color = ink, fontWeight = FontWeight.Bold); Text("Semua kategori dasar • 10–20 soal per sesi • Riwayat latihan", color = ink, style = MaterialTheme.typography.bodySmall) } } }
                item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("👑 Fitur PRO", color = ink, fontWeight = FontWeight.Bold); Text("Simulasi lengkap, statistik dan peringkat untuk langganan aktif", color = ink, style = MaterialTheme.typography.bodySmall) } } }
            }
            "catalog" -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                TextButton(onClick = { page = "home" }, contentPadding = PaddingValues(0.dp)) { Text("← Kembali ke Psikotes", color = teal) }
                Text(if (family == "private") "💼 Tes Kerja Swasta" else if (family == "cpns") "🏛️ Persiapan CPNS" else "Pilih Jenis Tes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
                Text("Pilih latihan sesuai tujuanmu. Kerjakan soal, pantau progres, dan tingkatkan kemampuan.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF59667A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Semua" to "all", "Swasta" to "private", "CPNS" to "cpns").forEach { (label, value) ->
                        FilterChip(selected = family == value, onClick = { family = value }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = orange, selectedLabelColor = Color.White))
                    }
                }
                val visible = modes.filter {
                    val f = it.s("family").lowercase()
                    family == "all" || f == family || (family == "cpns" && f == "skb")
                }
                if (visible.isEmpty() && !loading) Text("Belum ada tes tersedia dari server.", color = ink)
                Text("FREE: kategori dasar 10–20 soal. Kategori lain memerlukan PRO.", color = ink, style = MaterialTheme.typography.bodySmall)
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(visible) { mode ->
                        val title = mode.s("title", "Tes")
                        val icon = testIcon(title)
                        Card(onClick = {
                            if (isPro || freeEligible(mode)) { vm.start(mode.s("test_id"), (if (mode.has("training_question_count")) mode.n("training_question_count") else mode.n("question_count")).coerceIn(10, 20)); page = "exam" }
                            else if (basicMode(mode)) freeNotice = true
                            else proNotice = true
                        }, enabled = !loading, modifier = Modifier.height(164.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.background(icon.second.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).size(42.dp), contentAlignment = Alignment.Center) {
                                    Text(icon.first, style = MaterialTheme.typography.titleLarge)
                                }
                                Text(title, fontWeight = FontWeight.Bold, color = ink, maxLines = 2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${(if (mode.has("training_question_count")) mode.n("training_question_count") else mode.n("question_count")).coerceIn(10, 20)} soal  •  ${mode.n("duration_seconds") / 60} menit", color = Color(0xFF58657A), style = MaterialTheme.typography.labelSmall)
                                Text(if (freeEligible(mode)) "FREE • Mulai →" else if (basicMode(mode)) "FREE • 10–20 soal saja" else "👑 PRO", color = if (freeEligible(mode)) teal else orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            "exam" -> if (session != null && questions.size in 10..20) {
                ExamPanel(session!!, questions, submitting, pendingCount, vm::answer, vm::submit, { vm.syncAnswers() })
            } else Column(Modifier.padding(16.dp)) {
                Text(if (loading) "Memuat sesi dan soal..." else "Sesi belum dapat dibuka. Periksa koneksi atau konfigurasi bank soal.", color = ink)
                TextButton(onClick = { vm.loadCatalog(); page = "catalog" }) { Text("Pilih Tes Lagi") }
            }
            "result" -> result?.let { r ->
                val actualScore = PsychotestPolicy.normalizedScore(
                    r.get("score")?.takeUnless { it.isJsonNull }?.asDouble,
                    r.get("max_score")?.takeUnless { it.isJsonNull }?.asDouble
                )
                val encouragement = when {
                    actualScore == null -> "Terima kasih sudah mengenal dirimu lebih jauh! Setiap karakter punya kelebihan yang bisa dikembangkan."
                    actualScore >= 90 -> "Luar biasa! Kemampuanmu sangat baik. Pertahankan prestasimu!"
                    actualScore >= 75 -> "Kerja bagus! Kamu sudah berada di jalur yang tepat."
                    actualScore >= 60 -> "Usahamu bagus! Terus berlatih untuk meningkatkan kemampuan."
                    actualScore >= 40 -> "Jangan menyerah! Setiap latihan adalah kesempatan berkembang."
                    else -> "Tetap semangat! Mulai dari dasar dan tingkatkan kemampuanmu sedikit demi sedikit."
                }
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Text("🏆 Hasil & Insight", color = ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (actualScore != null && actualScore >= 75) "🏆" else "📊", style = MaterialTheme.typography.displayMedium)
                                Text("HASIL LATIHAN PSIKOTES", color = ink, fontWeight = FontWeight.Bold)
                                if (actualScore != null) {
                                    Text("$actualScore / 100", color = orange, fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.displayMedium)
                                    LinearProgressIndicator(progress = { actualScore.coerceIn(0, 100) / 100f },
                                        modifier = Modifier.fillMaxWidth().height(10.dp),
                                        color = orange, trackColor = Color(0xFFFFE5D6))
                                } else {
                                    Text("Asesmen Kepribadian", color = orange, style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text((if (actualScore != null) when { actualScore >= 90 -> "SANGAT BAIK"; actualScore >= 75 -> "BAIK"; actualScore >= 60 -> "CUKUP"; actualScore >= 40 -> "PERLU LATIHAN"; else -> "TAHAP PENGEMBANGAN" } else "PROFIL DIRI") + "\n" + encouragement, color = ink, textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    item {
                        Info("📋 Ringkasan Hasil",
                            "Benar: ${r.s("correct_count", "—")}  •  Salah: ${r.s("wrong_count", "—")}  •  Kosong: ${r.s("blank_count", "—")}" +
                            if (actualScore == null) "\nTes kepribadian tidak menggunakan nilai benar/salah." else
                                "\nSkor latihan dinormalisasi ke skala 0–100; bobot resmi mengikuti server.")
                    }
                    item {
                        val correct = r.n("correct_count")
                        val wrong = r.n("wrong_count")
                        val blank = r.n("blank_count")
                        val total = correct + wrong + blank
                        if (total > 0 && actualScore != null) {
                            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("📊 Tingkat Ketepatan", color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    val accuracy = (correct.toFloat() / total).coerceIn(0f, 1f)
                                    Text("${(accuracy * 100).toInt()}% jawaban benar", color = teal, fontWeight = FontWeight.Bold)
                                    LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth().height(12.dp), color = teal, trackColor = Color(0xFFE6F0F1))
                                    Text("✓ Benar $correct    ✕ Salah $wrong    ○ Kosong $blank", color = ink)
                                }
                            }
                        }
                    }
                    if (isPro) {
                        item {
                            Text("👑 Analisis Lengkap", color = ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Evaluasi", "Pembahasan", "Rencana").forEachIndexed { tab, label ->
                                    FilterChip(selected = proResultTab == tab, onClick = { proResultTab = tab },
                                        modifier = Modifier.weight(1f), label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = orange, selectedLabelColor = Color.White))
                                }
                            }
                        }
                        when (proResultTab) {
                            0 -> {
                                val correct = r.n("correct_count")
                                val wrong = r.n("wrong_count")
                                val blank = r.n("blank_count")
                                val total = correct + wrong + blank
                                val accuracy = if (total > 0) correct * 100 / total else 0
                                item { Info("📈 Evaluasi Kemampuan", if (actualScore == null) "Asesmen ini memetakan preferensi, bukan benar atau salah." else "Ketepatan $accuracy% dari $total soal. $correct jawaban tepat dan $wrong perlu ditinjau. Ini hasil latihan, bukan penilaian rekrutmen resmi.") }
                                if (actualScore != null) item { Info("🔍 Fokus Perbaikan", when {
                                    wrong > correct -> "Perkuat pemahaman konsep dan alasan jawaban sebelum berlatih dengan batas waktu."
                                    wrong > 0 -> "Tinjau jawaban yang keliru untuk menemukan pola kesalahan."
                                    else -> "Pertahankan ketepatan dengan variasi soal berikutnya."
                                }) }
                            }
                            1 -> {
                                val answeredQuestions = r.getAsJsonArray("questions")?.map { it.asJsonObject }.orEmpty()
                                if (answeredQuestions.isEmpty()) item { Info("📚 Pembahasan", "Pembahasan soal belum tersedia dari server.") }
                                else itemsIndexed(answeredQuestions) { index, q ->
                                    Info("Soal ${index + 1}: ${q.s("prompt", "Pertanyaan")}", q.s("explanation", "Pembahasan belum tersedia dari server."))
                                }
                            }
                            else -> item {
                                Info("🎯 Saran Pengembangan", when {
                                    actualScore == null -> "Kenali preferensi kerja dan kekuatanmu dari asesmen ini. Hasil ini bukan diagnosis psikologis."
                                    actualScore < 40 -> "Perkuat konsep dasar dan ulangi soal yang belum dipahami. Mulai dengan sesi pendek setiap hari."
                                    actualScore < 60 -> "Tinjau kembali jawaban yang salah dan latih ketelitian sebelum meningkatkan kecepatan."
                                    actualScore < 75 -> "Tingkatkan konsistensi dengan latihan bertimer dan telaah pembahasan soal."
                                    actualScore < 90 -> "Pertajam strategi menjawab soal sulit dan pertahankan latihan rutin."
                                    else -> "Pertahankan kemampuanmu dan coba variasi soal dengan tingkat kesulitan lebih tinggi."
                                })
                            }
                        }
                    } else {
                        item {
                            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1E2))) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🔒 Analisis & Pengembangan • PRO", color = ink, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium)
                                    Text("Lihat pembahasan jawaban per soal, evaluasi kemampuan, dan saran pengembangan.", color = ink)
                                    Text("👑 Buka Analisis Lengkap — PRO", color = orange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item {
                        Button(onClick = { page = "home"; vm.loadExtras(isPro) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = teal, contentColor = Color.White)) {
                            Text("Coba Tes Lainnya")
                        }
                    }
                }
            }
            "history" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) } }
                if (history.isEmpty()) item { Text("Belum ada riwayat.") }
                items(history) {
                    val score = PsychotestPolicy.normalizedScore(it.get("score")?.takeUnless { x -> x.isJsonNull }?.asDouble, it.get("max_score")?.takeUnless { x -> x.isJsonNull }?.asDouble)
                    HistoryScoreCard(it.s("title", "Tes"), score)
                }
            }
            "stats" -> if (isPro) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) }
                stats?.let {
                    Text("📊 Statistik Latihan", color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Info("📝 Total Tes", it.s("total_tests", "0"))
                    Info("📈 Rata-rata Nilai", it.s("average_percent", "-"))
                    Info("🚀 Peningkatan", it.s("improvement_percent", "-"))
                } ?: Text("Belum ada statistik.")
            }
            "leaderboard" -> if (isPro) LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) } }
                item { Text("🏆 Peringkat Peserta", color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                if (leaderboard.isEmpty()) item { Text("Belum ada peringkat.") }
                items(leaderboard) { Info("🏆 ${it.s("alias", "Peserta")}", "Skor: ${it.s("score", "-")}") }
            }
        }
    }
}
    }
    }


private fun testIcon(title: String): Pair<String, Color> {
    val t = title.lowercase()
    return when {
        "numer" in t || "matemat" in t || "hitung" in t -> "🔢" to blue
        "verbal" in t || "bahasa" in t -> "📚" to teal
        "deret" in t || "pola" in t -> "🧩" to orange
        "teliti" in t || "ketelitian" in t -> "🔍" to teal
        "spasial" in t || "gambar" in t -> "🧊" to blue
        "kepribadian" in t -> "👤" to Color(0xFF8467D8)
        "twk" in t || "konstitusi" in t -> "🇮🇩" to orange
        "tiu" in t -> "🧮" to blue
        "tkp" in t -> "🤝" to teal
        "cat" in t -> "💻" to blue
        "skb" in t || "administrasi" in t -> "📁" to orange
        "logika" in t -> "🧠" to Color(0xFFE87883)
        else -> "📝" to teal
    }
}

@Composable
private fun CompactRoute(icon: String, title: String, detail: String, colors: List<Color>, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().height(105.dp).background(Brush.horizontalGradient(colors)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).background(Color.White.copy(alpha = 0.23f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(detail, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Text("❯", color = Color.White)
        }
    }
}

@Composable
private fun Banner(title: String, subtitle: String, colors: List<Color>, dark: Boolean = false, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (dark) ink else Color.White)
            Text(subtitle, color = if (dark) ink else Color.White)
        }
    }
}
@Composable
private fun Shortcut(label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.height(78.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { Text(label, color = ink, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
    }
}
@Composable
private fun Info(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = ink, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray)
        }
    }
}
@Composable
internal fun ExamPanel(session: JsonObject, questions: List<JsonObject>, submitting: Boolean, pendingCount: Int, onAnswer: (String, String) -> Unit, onSubmit: () -> Unit, onSync: () -> Unit) {
    val id = session.s("id")
    var index by rememberSaveable(id) { mutableIntStateOf(0) }
    var remaining by remember(id) { mutableIntStateOf(session.n("remaining_seconds")) }
    var confirm by remember(id) { mutableStateOf(false) }
    var autoAttempted by rememberSaveable(id) { mutableStateOf(false) }
    LaunchedEffect(id, session.n("remaining_seconds")) {
        val deadline = android.os.SystemClock.elapsedRealtime() + session.n("remaining_seconds") * 1000L
        do {
            remaining = ((deadline - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L) / 1000).toInt()
            if (remaining > 0) delay(250)
        } while (remaining > 0)
        if (!autoAttempted) { autoAttempted = true; onSubmit() }
    }
    val answered = questions.count { it.get("selected_option_id")?.takeUnless { v -> v.isJsonNull } != null }
    val allAnswered = questions.isNotEmpty() && answered == questions.size
    if (confirm) AlertDialog(
        containerColor = Color.White, titleContentColor = ink, textContentColor = ink,
        onDismissRequest = { confirm = false },
        title = { Text("Selesaikan tes?") },
        text = { Text("Semua $answered soal sudah dijawab. Kirim untuk melihat nilai?") },
        confirmButton = { TextButton(onClick = { confirm = false; if (!submitting) onSubmit() }) { Text("Selesaikan", color = orange, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Periksa Lagi", color = ink) } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("⏱  SISA WAKTU", color = Color(0xFF58657A), style = MaterialTheme.typography.labelMedium); Text("%02d:%02d".format(remaining / 60, remaining % 60), color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text("TERJAWAB", color = Color(0xFF58657A), style = MaterialTheme.typography.labelMedium); Text("$answered/${questions.size}", color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
        }
        Text("📝 Soal ${index + 1} dari ${questions.size}", fontWeight = FontWeight.Bold, color = ink, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth().height(7.dp), color = orange, trackColor = Color(0xFFE3EBF1))
        questions.getOrNull(index)?.let { q ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(q.s("prompt"), fontWeight = FontWeight.Bold, color = ink, style = MaterialTheme.typography.titleMedium)
                    (q.getAsJsonArray("options")?.map { it.asJsonObject } ?: emptyList()).forEach { option ->
                        val selected = q.s("selected_option_id") == option.s("id")
                        Card(onClick = { if (!submitting && remaining > 0) onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFE8DC) else Color(0xFFF7FAFD)), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) orange else Color(0xFFD9E3ED))) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RadioButton(selected = selected, onClick = { if (!submitting && remaining > 0) onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.size(36.dp), colors = RadioButtonDefaults.colors(selectedColor = orange))
                                Text(option.s("text"), color = ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { index = PsychotestPolicy.previous(index) }, enabled = index > 0, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, disabledContainerColor = orange, disabledContentColor = Color.White)) { Text("Sebelumnya", color = Color.White) }
            if (index < questions.lastIndex) {
                Button(onClick = { index = PsychotestPolicy.next(index, questions.size) }, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = Color.White)) { Text("Selanjutnya", color = Color.White) }
            } else {
                Button(onClick = { confirm = true }, enabled = (allAnswered || remaining == 0) && !submitting, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = Color.White)) { Text(if (submitting) "Mengirim..." else "Selesaikan Tes", color = Color.White) }
            }
        }
        if (pendingCount > 0) TextButton(onClick = onSync, enabled = !submitting) { Text("Sinkronkan Jawaban ($pendingCount)") }
        if (index == questions.lastIndex && !allAnswered) Text("Jawab semua soal untuk menyelesaikan tes. Gunakan Sebelumnya untuk memeriksa jawaban.", color = Color(0xFF58657A), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryScoreCard(title: String, score: Int?) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(62.dp).background(Color(0xFFFFF0E3), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Text(score?.toString() ?: "🧠", color = orange, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ink, fontWeight = FontWeight.Bold)
                Text(if (score == null) "Asesmen kepribadian" else "Skor: $score / 100", color = teal)
                if (score != null) LinearProgressIndicator(progress = { score.coerceIn(0, 100) / 100f },
                    color = teal, trackColor = Color(0xFFE8F0F3), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
