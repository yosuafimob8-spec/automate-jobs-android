package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private val orange = Color(0xFFFF7033)
private val teal = Color(0xFF11B9BE)
private val blue = Color(0xFF219CEC)
private val ink = Color(0xFF172744)
private fun JsonObject.s(k: String, fallback: String = "") = get(k)?.takeUnless { it.isJsonNull }?.asString ?: fallback
private fun JsonObject.n(k: String) = runCatching { get(k)?.asInt ?: 0 }.getOrDefault(0)

@Composable
fun LatihanPsikotesScreen(onExit: () -> Unit = {}) {
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
    var page by remember { mutableStateOf("home") }
    var family by remember { mutableStateOf("all") }
    var proNotice by remember { mutableStateOf(false) }
    var freeNotice by remember { mutableStateOf(false) }
    // Until subscription verification is integrated, never unlock PRO by default.
    val isPro = false
    val basicCategories = listOf("logika", "numerik", "matematika", "verbal", "bahasa", "ketelitian", "deret", "pola", "spasial", "gambar", "kepribadian", "twk", "tiu", "tkp")
    fun basicMode(mode: JsonObject): Boolean {
        val name = mode.s("title").lowercase()
        val code = mode.s("test_id").lowercase()
        return basicCategories.any { it in name || it in code } &&
            !listOf("simulasi", "cat", "skb", "premium", "pro", "full").any { it in name || it in code }
    }
    fun freeEligible(mode: JsonObject): Boolean = basicMode(mode) && mode.n("question_count") in 10..20

    LaunchedEffect(Unit) { vm.loadCatalog(); vm.loadExtras() }
    LaunchedEffect(session, result) {
        if (page == "exam" && result != null) page = "result"
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
        text = { Text("Simulasi lengkap, statistik, dan peringkat memerlukan langganan PRO yang aktif. Aktivasi belum terhubung.") },
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
                                Text("Psikotes", color = ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                        Shortcut("🕘 Riwayat", Modifier.weight(1f)) { vm.loadExtras(); page = "history" }
                        Shortcut("📊 Statistik 👑", Modifier.weight(1f)) { if (isPro) { vm.loadExtras(); page = "stats" } else proNotice = true }
                        Shortcut("🏆 Peringkat 👑", Modifier.weight(1f)) { if (isPro) { vm.loadExtras(); page = "leaderboard" } else proNotice = true }
                    }
                }
                item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Latihan Gratis", color = ink, fontWeight = FontWeight.Bold); Text("Semua kategori dasar • 10–20 soal per sesi • Riwayat latihan", color = ink, style = MaterialTheme.typography.bodySmall) } } }
                item { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("👑 Fitur PRO", color = ink, fontWeight = FontWeight.Bold); Text("Simulasi lengkap, statistik dan peringkat • Segera hadir", color = ink, style = MaterialTheme.typography.bodySmall) } } }
            }
            "catalog" -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                TextButton(onClick = { page = "home" }, contentPadding = PaddingValues(0.dp)) { Text("← Kembali ke Psikotes", color = teal) }
                Text("Pilih Jenis Tes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
                Text("Pilih latihan dan mulai kapan saja.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF59667A))
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
                            if (isPro || freeEligible(mode)) { vm.start(mode.s("test_id")); page = "exam" }
                            else if (basicMode(mode)) freeNotice = true
                            else proNotice = true
                        }, modifier = Modifier.height(164.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.background(icon.second.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).size(42.dp), contentAlignment = Alignment.Center) {
                                    Text(icon.first, style = MaterialTheme.typography.titleLarge)
                                }
                                Text(title, fontWeight = FontWeight.Bold, color = ink, maxLines = 2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${mode.n("question_count")} soal  •  ${mode.n("duration_seconds") / 60} menit", color = Color(0xFF58657A), style = MaterialTheme.typography.labelSmall)
                                Text(if (freeEligible(mode)) "FREE • Mulai →" else if (basicMode(mode)) "FREE • 10–20 soal saja" else "👑 PRO", color = if (freeEligible(mode)) teal else orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            "exam" -> session?.let { ExamPanel(it, questions, vm::answer, vm::submit) }
            "result" -> result?.let { r ->
                val maxScore = r.n("max_score").coerceAtLeast(1)
                val actualScore = (r.n("score") * 100f / maxScore).toInt().coerceIn(0, 100)
                val total = r.getAsJsonArray("questions")?.size() ?: questions.size
                val encouragements = when (actualScore) {
                    in 0..39 -> listOf("🌱 Tetap semangat! Setiap latihan adalah langkah maju.", "💪 Jangan menyerah, kemampuan tumbuh lewat latihan.", "✨ Awal yang baik untuk belajar lebih jauh!")
                    in 40..59 -> listOf("💪 Terus berlatih, kamu semakin berkembang!", "🌟 Progresmu sudah terlihat. Ayo tingkatkan lagi!", "🚀 Sedikit demi sedikit, hasilmu akan meningkat!")
                    in 60..79 -> listOf("⭐ Bagus! Pertahankan semangat belajarmu.", "🔥 Hasil yang baik! Masih ada ruang untuk bersinar.", "👏 Kamu berada di jalur yang tepat!")
                    in 80..94 -> listOf("🏅 Hebat! Kerja kerasmu membuahkan hasil.", "🔥 Luar biasa, terus pertahankan performamu!", "🌟 Kemampuanmu sangat menjanjikan!")
                    else -> listOf("🏆 Fantastis! Penguasaanmu sangat kuat.", "🎉 Luar biasa! Kamu tampil sangat baik.", "👑 Hasil istimewa, pertahankan prestasimu!")
                }
                val encouragement = remember(r, actualScore) { encouragements.random() }
                LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                    item { Banner("🏆  Tes Selesai", "Hasil latihan kamu sudah tersedia.", listOf(Color(0xFFE4F8F7), Color(0xFFFFE8D7)), true) {} }
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(encouragement, color = ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                                Text("NILAI LATIHAN", color = Color(0xFF526176), fontWeight = FontWeight.Bold)
                                Text("$actualScore", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.ExtraBold, color = ink)
                                Text("dari 100", color = Color(0xFF526176))
                                LinearProgressIndicator(progress = { actualScore / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp), color = orange, trackColor = Color(0xFFE8EFF4))
                                Text("Jawaban benar: ${r.n("score")} / $maxScore", color = ink, fontWeight = FontWeight.SemiBold)
                                Text("Nilai berdasarkan jawaban benar yang dinilai server.", color = Color(0xFF526176), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Info("📈  Analisis Kemampuan Mendalam  👑 PRO", "Kekuatan, kelemahan, dan rekomendasi latihan personal. Ketuk untuk informasi akses PRO.") }
                    item { Info("📚  Pembahasan Detail  👑 PRO", "Kunci jawaban dan penjelasan setiap soal tersedia untuk pelanggan PRO.") }
                    item { Text("Fitur analisis mendalam dan pembahasan detail memerlukan PRO aktif.", color = orange, style = MaterialTheme.typography.bodySmall) }
                    item {
                        Button(onClick = { page = "home"; vm.loadExtras() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Kembali ke Psikotes") }
                    }
                }
            }
            "history" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) } }
                if (history.isEmpty()) item { Text("Belum ada riwayat.") }
                items(history) { Info(it.s("title", "Tes"), "Skor: ${it.s("score", "-")}") }
            }
            "stats" -> if (isPro) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) }
                stats?.let {
                    Info("Total Tes", it.s("total_tests", "0"))
                    Info("Rata-rata", it.s("average_percent", "-"))
                    Info("Peningkatan", it.s("improvement_percent", "-"))
                } ?: Text("Belum ada statistik.")
            }
            "leaderboard" -> if (isPro) LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) } }
                if (leaderboard.isEmpty()) item { Text("Belum ada peringkat.") }
                items(leaderboard) { Info("🏆 ${it.s("alias", "Peserta")}", "Skor: ${it.s("score", "-")}") }
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
private fun ExamPanel(session: JsonObject, questions: List<JsonObject>, onAnswer: (String, String) -> Unit, onSubmit: () -> Unit) {
    val id = session.s("id")
    var index by remember(id) { mutableIntStateOf(0) }
    var remaining by remember(id) { mutableIntStateOf(session.n("remaining_seconds")) }
    var confirm by remember(id) { mutableStateOf(false) }
    var submitted by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        while (remaining > 0) { delay(1000); remaining-- }
        if (!submitted) { submitted = true; onSubmit() }
    }
    val answered = questions.count { it.get("selected_option_id")?.takeUnless { v -> v.isJsonNull } != null }
    val allAnswered = questions.isNotEmpty() && answered == questions.size
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Selesaikan tes?") },
        text = { Text("Semua $answered soal sudah dijawab. Kirim untuk melihat nilai?") },
        confirmButton = { TextButton(onClick = { confirm = false; if (!submitted) { submitted = true; onSubmit() } }) { Text("Selesaikan") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Periksa Lagi") } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("⏱  SISA WAKTU", color = Color(0xFF58657A), style = MaterialTheme.typography.labelMedium); Text("%02d:%02d".format(remaining / 60, remaining % 60), color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text("TERJAWAB", color = Color(0xFF58657A), style = MaterialTheme.typography.labelMedium); Text("$answered/${questions.size}", color = ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
        }
        Text("Soal ${index + 1} dari ${questions.size}", fontWeight = FontWeight.Bold, color = ink, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth().height(7.dp), color = orange, trackColor = Color(0xFFE3EBF1))
        questions.getOrNull(index)?.let { q ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(q.s("prompt"), fontWeight = FontWeight.Bold, color = ink, style = MaterialTheme.typography.titleMedium)
                    (q.getAsJsonArray("options")?.map { it.asJsonObject } ?: emptyList()).forEach { option ->
                        val selected = q.s("selected_option_id") == option.s("id")
                        Card(onClick = { onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFE8DC) else Color(0xFFF7FAFD)), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) orange else Color(0xFFD9E3ED))) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RadioButton(selected = selected, onClick = { onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.size(36.dp), colors = RadioButtonDefaults.colors(selectedColor = orange))
                                Text(option.s("text"), color = ink, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { index-- }, enabled = index > 0, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, disabledContainerColor = orange, disabledContentColor = Color.White)) { Text("Sebelumnya", color = Color.White) }
            if (index < questions.lastIndex) {
                Button(onClick = { index++ }, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Selanjutnya", color = Color.White) }
            } else {
                Button(onClick = { confirm = true }, enabled = allAnswered && !submitted, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Selesaikan Tes", color = Color.White) }
            }
        }
        if (index == questions.lastIndex && !allAnswered) Text("Jawab semua soal untuk menyelesaikan tes. Gunakan Sebelumnya untuk memeriksa jawaban.", color = Color(0xFF58657A), style = MaterialTheme.typography.bodySmall)
    }
}
