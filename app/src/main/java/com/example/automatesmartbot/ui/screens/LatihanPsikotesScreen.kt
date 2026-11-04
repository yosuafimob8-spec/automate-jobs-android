package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.automatesmartbot.ui.viewmodel.PsychotestViewModel
import com.google.gson.JsonObject
import kotlinx.coroutines.delay

private val orange = com.example.automatesmartbot.ui.theme.KarirOrange
private val teal = Color(0xFF16A34A)
private val blue = Color(0xFFEA580C)
private val ink = Color(0xFF0F172A)
private fun JsonObject.s(k: String, fallback: String = "") = get(k)?.takeUnless { it.isJsonNull }?.asString ?: fallback
private fun JsonObject.n(k: String) = runCatching { get(k)?.asInt ?: 0 }.getOrDefault(0)

@Composable
fun LatihanPsikotesScreen(isPro: Boolean, onExit: () -> Unit = {}) {
    MaterialTheme(colorScheme = com.example.automatesmartbot.ui.theme.KarirMateLightColors) {
    Surface(color = Color.Transparent, contentColor = ink) {
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
    var resultAnalysisPage by rememberSaveable { mutableStateOf(false) }
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
        if (proNotice) { proNotice = false; return }
        if (freeNotice) { freeNotice = false; return }
        page = when (page) {
            "home" -> { onExit(); "home" }
            "catalog" -> "home"
            "exam" -> "catalog"
            "result" -> if (resultAnalysisPage) { resultAnalysisPage = false; "result" } else "catalog"
            "history", "stats", "leaderboard" -> "home"
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
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The parent dashboard already draws the app toolbar; do not duplicate it.
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = orange)
        error?.let { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }
        when (page) {
            "home" -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().height(122.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🧠 Psikotes", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Latihan hari ini, siap menghadapi tes kerja besok!", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
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
                item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("Latihan Gratis", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text("Semua kategori dasar • 10–20 soal per sesi • Riwayat latihan", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall) } } }
                item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Text("👑 Fitur PRO", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Text("Simulasi lengkap, statistik dan peringkat untuk langganan aktif", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall) } } }
            }
            "catalog" -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                TextButton(onClick = { back() }, contentPadding = PaddingValues(0.dp)) { Text("← Kembali ke Psikotes", color = teal) }
                Text(if (family == "private") "💼 Tes Kerja Swasta" else if (family == "cpns") "🏛️ Persiapan CPNS" else "Pilih Jenis Tes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Pilih latihan sesuai tujuanmu. Kerjakan soal, pantau progres, dan tingkatkan kemampuan.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF59667A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Semua" to "all", "Swasta" to "private", "CPNS" to "cpns").forEach { (label, value) ->
                        FilterChip(selected = family == value, onClick = { family = value }, label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = orange, selectedLabelColor = Color.White, containerColor = MaterialTheme.colorScheme.surface, labelColor = MaterialTheme.colorScheme.onSurface))
                    }
                }
                val visible = modes.filter {
                    val f = it.s("family").lowercase()
                    family == "all" || f == family || (family == "cpns" && f == "skb")
                }
                if (visible.isEmpty() && !loading) Text("Belum ada tes tersedia dari server.", color = MaterialTheme.colorScheme.onSurface)
                Text("FREE: kategori dasar 10–20 soal. Kategori lain memerlukan PRO.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(visible) { mode ->
                        val title = mode.s("title", "Tes")
                        val icon = testIcon(title)
                        Card(onClick = {
                            if (isPro || freeEligible(mode)) { val requestedCount = if (isPro && !freeEligible(mode)) mode.n("question_count").coerceIn(10, 110) else (if (mode.has("training_question_count")) mode.n("training_question_count") else mode.n("question_count")).coerceIn(10, 20); vm.start(mode.s("test_id"), requestedCount); page = "exam" }
                            else if (basicMode(mode)) freeNotice = true
                            else proNotice = true
                        }, enabled = !loading, modifier = Modifier.height(164.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.background(icon.second.copy(alpha = 0.13f), RoundedCornerShape(12.dp)).size(42.dp), contentAlignment = Alignment.Center) {
                                    Text(icon.first, style = MaterialTheme.typography.titleLarge)
                                }
                                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("${(if (isPro && !freeEligible(mode)) mode.n("question_count").coerceIn(10, 110) else (if (mode.has("training_question_count")) mode.n("training_question_count") else mode.n("question_count")).coerceIn(10, 20))} soal  •  ${mode.n("duration_seconds") / 60} menit", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                Text(if (freeEligible(mode)) "FREE • Mulai →" else if (basicMode(mode)) "FREE • 10–20 soal saja" else "👑 PRO", color = if (freeEligible(mode)) teal else orange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            "exam" -> if (session != null && questions.isNotEmpty() && questions.size <= 110) {
                ExamPanel(session!!, questions, submitting, pendingCount, vm::answer, vm::submit, { vm.syncAnswers() })
            } else Column(Modifier.padding(16.dp)) {
                Text(if (loading) "Memuat sesi dan soal..." else "Sesi belum dapat dibuka. Periksa koneksi atau konfigurasi bank soal.", color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = { vm.loadCatalog(); page = "catalog" }) { Text("Pilih Tes Lagi") }
            }
            "result" -> result?.let { r ->
                val actualScore = PsychotestPolicy.normalizedScore(
                    r.get("score")?.takeUnless { it.isJsonNull }?.asDouble,
                    r.get("max_score")?.takeUnless { it.isJsonNull }?.asDouble
                )
                val correct = r.n("correct_count")
                val wrong = r.n("wrong_count")
                val blank = r.n("blank_count")
                val total = correct + wrong + blank
                val accuracy = if (total > 0) correct * 100 / total else 0
                var analysisTab by rememberSaveable { mutableStateOf(0) }
                var showReview by rememberSaveable { mutableStateOf(false) }
                var selectedCategory by rememberSaveable { mutableStateOf("") }
                var animateResults by remember { mutableStateOf(false) }
                LaunchedEffect(r) { animateResults = true }
                val animatedScore by animateIntAsState(targetValue = if (animateResults) (actualScore ?: 0).coerceIn(0, 100) else 0, animationSpec = tween(1000), label = "score")
                val animatedAccuracy by animateFloatAsState(targetValue = if (animateResults) accuracy.coerceIn(0, 100) / 100f else 0f, animationSpec = tween(1100), label = "accuracy")
                val rating = when {
                    actualScore == null -> "Asesmen Kepribadian"
                    actualScore >= 80 -> "Sangat Baik"
                    actualScore >= 60 -> "Baik"
                    actualScore >= 40 -> "Terus Berkembang"
                    else -> "Tahap Pengembangan"
                }
                val encouragement = when {
                    actualScore == null -> "Kenali potensi dan pola kerjamu melalui hasil latihan ini."
                    actualScore >= 80 -> "Kemampuan kognitifmu berada di atas rata-rata. Pertahankan dan terus tingkatkan performamu! 🚀"
                    actualScore >= 60 -> "Hasil yang baik! Terus asah kemampuan dan tingkatkan ketelitianmu."
                    else -> "Tetap semangat! Mulai dari dasar dan tingkatkan kemampuanmu sedikit demi sedikit."
                }
                LazyColumn(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { back() }) {
                                Text("←", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
                            }
                            Text("🟧 Karir", color = orange, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Text("Mate", color = Color(0xFF008A81), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    if (!resultAnalysisPage) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("LAPORAN LATIHAN", color = teal, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Hasil Psikotes", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Kenali pencapaianmu, tinjau jawaban, lalu pilih langkah berikutnya.", color = Color(0xFF59667A))
                            }
                        }
                        item {
                            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF003A2B)), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF003C2E), Color(0xFF00583A), Color(0xFF002F28)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                                         Text("♛  HASIL PSIKOTES  ♛", color = Color(0xFFFFD978), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                         Box(Modifier.size(194.dp), contentAlignment = Alignment.Center) {
                                             Canvas(Modifier.fillMaxSize()) {
                                                 drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, style = Stroke(17.dp.toPx(), cap = StrokeCap.Round))
                                                 drawArc(Color(0xFFFFC45D), -90f, 360f * animatedScore / 100f, false, style = Stroke(17.dp.toPx(), cap = StrokeCap.Round))
                                             }
                                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                 Text("Skor Akhir", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                                 Text(if (actualScore == null) "—" else "$animatedScore", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold)
                                                 Text("/ 100", color = Color(0xFFB9F2E9), style = MaterialTheme.typography.titleMedium)
                                             }
                                         }
                                         Surface(color = Color(0xFFFFC45D), shape = RoundedCornerShape(3.dp)) {
                                             Text("🏆  $rating  🏆", color = Color(0xFF102C38), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                                         }
                                         Text("$correct dari $total soal benar", color = Color(0xFFE4F5F2))
                                     }
                                     HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                                    Text("✦  $encouragement", color = Color.White, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        item {
                            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    ScoreStat("✓", "$correct", "Benar", Color(0xFF0EAE4D), Modifier.weight(1f))
                                    Box(Modifier.width(1.dp).height(65.dp).background(Color(0xFFE2E6E9)))
                                    ScoreStat("✕", "$wrong", "Salah", Color(0xFFE52935), Modifier.weight(1f))
                                    Box(Modifier.width(1.dp).height(65.dp).background(Color(0xFFE2E6E9)))
                                    ScoreStat("◎", "$accuracy%", "Akurasi", Color(0xFFB319C7), Modifier.weight(1f))
                                }
                            }
                        }
                        item {
                            Text("Analisis Hasil Latihan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Text("Buka jawaban dan ketuk kategori untuk rekomendasi latihan.", color = Color(0xFF59667A), style = MaterialTheme.typography.bodySmall)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { showReview = !showReview }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = teal)) {
                                    Text(if (showReview) "Tutup Jawaban ↑" else "Lihat Jawaban ↓", maxLines = 1)
                                }
                                Button(onClick = { if (isPro) { resultAnalysisPage = true; analysisTab = 1 } else proNotice = true }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = orange)) {
                                    Text("Analisis PRO →", maxLines = 1)
                                }
                            }
                        }
                        if (showReview) {
                            val details = r.getAsJsonArray("questions")?.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }.orEmpty()
                            if (details.isEmpty()) item {
                                MockupCard { Text("Rincian jawaban belum dikirim server untuk sesi ini.", color = MaterialTheme.colorScheme.onSurface) }
                            }
                            items(details) { q ->
                                MockupCard {
                                    Text(q.s("prompt", q.s("question", "Soal")), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text("Jawabanmu: " + q.s("selected_answer", q.s("user_answer", "Tidak tersedia")), color = MaterialTheme.colorScheme.onSurface)
                                    Text("Jawaban benar: " + q.s("correct_answer", "Tidak tersedia"), color = teal)
                                }
                            }
                        }
                        item {
                            MockupCard(tint = Color(0xFFEAFBF0), borderColor = Color(0xFFC8F1D7)) {
                                Text("▥  Analisis Kemampuan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                Text("Ketuk kategori untuk melihat rekomendasi latihan.", color = Color(0xFF59667A), style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                val categories = r.getAsJsonArray("categories") ?: r.getAsJsonArray("category_scores")
                                val categoryMap = mutableMapOf<String, Int>()
                                categories?.forEach { element ->
                                    if (element.isJsonObject) {
                                        val cat = element.asJsonObject
                                        val label = cat.s("name", cat.s("category", ""))
                                        val value = if (cat.has("percent")) cat.n("percent") else cat.n("score")
                                        categoryMap[label.lowercase()] = value.coerceIn(0, 100)
                                    }
                                }
                                if (categoryMap.isEmpty()) {
                                    Text("Data hasil kategori belum tersedia.", color = Color.Gray)
                                }
                                categoryMap.forEach { (label, score) ->
                                    val expanded = selectedCategory == label
                                    Column(Modifier.fillMaxWidth().clickable { selectedCategory = if (expanded) "" else label }.padding(vertical = 8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(label.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.weight(1f).height(9.dp), color = Color(0xFF16B85B), trackColor = Color(0xFFD8F3E2), strokeCap = StrokeCap.Round)
                                        Text("$score%  ${if (expanded) "⌃" else "⌄"}", modifier = Modifier.width(68.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    AnimatedVisibility(visible = expanded) {
                                        Text(if (score >= 80) "Kekuatanmu di kategori ini sudah sangat baik. Pertahankan latihan rutin." else if (score >= 60) "Kemampuan cukup baik. Latih kembali soal yang masih keliru." else "Prioritaskan latihan kategori ini, mulai dari soal dasar lalu tingkatkan kesulitan.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    }
                                    }
                                }
                            }
                        }
                        item {
                            MockupCard(tint = Color(0xFFFFF4E4), borderColor = Color(0xFFFFB966), onClick = { if (isPro) resultAnalysisPage = true else proNotice = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔐", style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Analisis & Pengembangan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                                        Text("Lihat analisis jawaban per soal, evaluasi kemampuan, dan saran pengembangan.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(color = orange, shape = RoundedCornerShape(50)) { Text("PRO", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
                                }
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { showReview = !showReview }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFFF6B00))) {
                                    Text(if (showReview) "Tutup Jawaban" else "Lihat Jawaban", color = Color(0xFFFF6B00), maxLines = 1)
                                }
                                Button(onClick = { page = "catalog"; vm.loadExtras(isPro) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00))) {
                                    Text("↻ Latihan Lagi", color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    } else {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Analisis Lengkap", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                                Surface(color = orange, shape = RoundedCornerShape(50)) { Text("👑 PRO", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) }
                            }
                            Text("Evaluasi detail hasil psikotes, analisis jawaban, dan saran pengembangan khusus untukmu.", color = MaterialTheme.colorScheme.onSurface)
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("Analisis", "Pembahasan", "Pengembangan").forEachIndexed { index, label ->
                                    Button(
                                        onClick = { analysisTab = index },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (analysisTab == index) orange else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (analysisTab == index) Color.White else MaterialTheme.colorScheme.onSurface)
                                    ) { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                                }
                            }
                        }
                        when (analysisTab) {
                            0 -> {
                                item {
                                    MockupCard {
                                        Text("📊  Profil Kemampuan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                        Text("Dari $total soal, kamu menjawab $correct soal dengan benar dan $wrong salah. Tingkat ketepatanmu $accuracy%. Evaluasi ini menunjukkan hasil latihan berdasarkan jawaban yang tersedia.", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                item {
                                    MockupCard(tint = Color(0xFFEAFBF3)) {
                                        Text("👍  Kekuatan Utama", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                        Text("🟢  $correct jawaban berhasil dikerjakan dengan tepat.\n🟢  Pertahankan konsep yang sudah kamu kuasai.\n🟢  Latih konsistensi dan ketelitian dalam menjawab.", color = Color(0xFF202020), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                item {
                                    MockupCard(tint = Color(0xFFFFEDF0)) {
                                        Text("🎯  Area Pengembangan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                        Text("🔴  Tinjau kembali $wrong jawaban yang belum tepat.\n🔴  Tingkatkan kecepatan dan pemahaman konsep.\n🔴  Latih strategi manajemen waktu saat mengerjakan soal.", color = Color(0xFF202020), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                item {
                                    MockupCard(tint = Color(0xFFFFF5D5)) {
                                        Text("💡  Saran Pengembangan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                                        Text("🟡  Rutin latihan soal sesuai kategori yang perlu ditingkatkan.\n🟡  Gunakan simulasi psikotes dengan timer.\n🟡  Pelajari pola soal yang sering muncul pada seleksi kerja.\n🟡  Jaga fokus dan kondisi fisik sebelum tes.", color = Color(0xFF202020), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            1 -> {
                                val details = r.getAsJsonArray("questions")?.mapNotNull { if (it.isJsonObject) it.asJsonObject else null }.orEmpty()
                                if (details.isEmpty()) item { MockupCard { Text("Pembahasan belum tersedia dari server.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                                items(details) { q -> MockupCard { Text(q.s("prompt", "Soal"), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(q.s("explanation", "Pembahasan tidak tersedia dari server."), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            }
                            else -> {
                                item { MockupCard(tint = MaterialTheme.colorScheme.surfaceVariant) { Text("💡 Saran Pengembangan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold); Text("✓ Rutin latihan soal sesuai kategori yang sulit.\n✓ Gunakan simulasi dengan timer.\n✓ Pelajari pembahasan setiap jawaban.\n✓ Jaga fokus dan kondisi fisik sebelum tes.", color = MaterialTheme.colorScheme.onSurface) } }
                                item { Button(onClick = { page = "home"; vm.loadExtras(isPro) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Mulai Latihan Baru") } }
                            }
                        }
                    }
                }
            }
            "history" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { back() }) { Text("← Kembali", color = teal) } }
                if (history.isEmpty()) item { Text("Belum ada riwayat.") }
                items(history) {
                    val score = PsychotestPolicy.normalizedScore(it.get("score")?.takeUnless { x -> x.isJsonNull }?.asDouble, it.get("max_score")?.takeUnless { x -> x.isJsonNull }?.asDouble)
                    HistoryScoreCard(it.s("title", "Tes"), score)
                }
            }
            "stats" -> if (isPro) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) }
                stats?.let {
                    Text("📊 Statistik Latihan", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Info("📝 Total Tes", it.s("total_tests", "0"))
                    Info("📈 Rata-rata Nilai", it.s("average_percent", "-"))
                    Info("🚀 Peningkatan", it.s("improvement_percent", "-"))
                } ?: Text("Belum ada statistik.")
            }
            "leaderboard" -> if (isPro) LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { TextButton(onClick = { page = "home" }) { Text("← Kembali", color = teal) } }
                item { Text("🏆 Peringkat Peserta", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                if (leaderboard.isEmpty()) item { Text("Belum ada peringkat.") }
                items(leaderboard) { Info("🏆 ${it.s("alias", "Peserta")}", "Skor: ${it.s("score", "-")}") }
            }
        }
    }
}
    }
    }


@Composable
private fun MedalIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.40f
        val radius = size.minDimension * 0.30f
        val left = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx - radius * 0.85f, cy + radius * 0.5f)
            lineTo(cx - radius * 1.10f, size.height * 0.97f)
            lineTo(cx - radius * 0.45f, size.height * 0.80f)
            lineTo(cx, size.height * 0.97f)
            close()
        }
        drawPath(left, Color(0xFFFF6227))
        val right = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx + radius * 0.15f, cy + radius * 0.5f)
            lineTo(cx + radius * 0.80f, size.height * 0.97f)
            lineTo(cx + radius * 0.95f, size.height * 0.80f)
            lineTo(cx + radius * 1.25f, size.height * 0.88f)
            lineTo(cx + radius * 0.80f, cy + radius * 0.30f)
            close()
        }
        drawPath(right, Color(0xFFFF3C24))
        drawCircle(Color(0xFFFFB000), radius * 1.12f, androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(Color(0xFFFFE06B), radius * 0.96f, androidx.compose.ui.geometry.Offset(cx, cy))
        drawCircle(Color(0xFFFFA400), radius * 0.78f, androidx.compose.ui.geometry.Offset(cx, cy))
        val star = androidx.compose.ui.graphics.Path()
        for (i in 0 until 10) {
            val angle = Math.PI * (i.toDouble() / 5.0 - 0.5)
            val rr = if (i % 2 == 0) radius * 0.64f else radius * 0.27f
            val x = cx + (kotlin.math.cos(angle) * rr).toFloat()
            val y = cy + (kotlin.math.sin(angle) * rr).toFloat()
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, Color(0xFFFFF2A6))
    }
}

@Composable
private fun MockupCard(
    tint: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (tint == Color.Unspecified) MaterialTheme.colorScheme.surface else tint),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (borderColor == Color.Unspecified) MaterialTheme.colorScheme.outlineVariant else borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
    Card(
        onClick = onClick,
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
    }
}
@Composable
private fun Info(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
        containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = { confirm = false },
        title = { Text("Selesaikan tes?", color = MaterialTheme.colorScheme.onSurface) },
        text = { Text("Semua $answered soal sudah dijawab. Kirim untuk melihat nilai?", color = MaterialTheme.colorScheme.onSurface) },
        confirmButton = { TextButton(onClick = { confirm = false; if (!submitting) onSubmit() }) { Text("Selesaikan", color = orange, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Periksa Lagi", color = MaterialTheme.colorScheme.onSurface) } }
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("⏱  SISA WAKTU", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium); Text("%02d:%02d".format(remaining / 60, remaining % 60), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Column(horizontalAlignment = Alignment.End) { Text("TERJAWAB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium); Text("$answered/${questions.size}", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
        }
        Text("📝 Soal ${index + 1} dari ${questions.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth().height(7.dp), color = orange, trackColor = Color(0xFFE3EBF1))
        questions.getOrNull(index)?.let { q ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(q.s("prompt", q.s("question", "Soal tidak tersedia")), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                    (q.getAsJsonArray("options")?.map { it.asJsonObject } ?: emptyList()).forEach { option ->
                        val selected = q.s("selected_option_id") == option.s("id")
                        Card(onClick = { if (!submitting && remaining > 0) onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFE8DC) else MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, if (selected) orange else MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RadioButton(selected = selected, onClick = { if (!submitting && remaining > 0) onAnswer(q.s("question_id"), option.s("id")) }, modifier = Modifier.size(36.dp), colors = RadioButtonDefaults.colors(selectedColor = orange))
                                Text(option.s("text"), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { index = PsychotestPolicy.previous(index) }, enabled = index > 0, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, disabledContainerColor = Color(0xFFE5E7EB), disabledContentColor = Color(0xFF475569))) { Text("Sebelumnya") }
            if (index < questions.lastIndex) {
                Button(onClick = { index = PsychotestPolicy.next(index, questions.size) }, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = Color.White)) { Text("Selanjutnya", color = Color.White) }
            } else {
                Button(onClick = { confirm = true }, enabled = (allAnswered || remaining == 0) && !submitting, modifier = Modifier.weight(1f).heightIn(min = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = orange, contentColor = Color.White)) { Text(if (submitting) "Mengirim..." else "Selesaikan Tes", color = Color.White) }
            }
        }
        if (pendingCount > 0) TextButton(onClick = onSync, enabled = !submitting) { Text("Sinkronkan Jawaban ($pendingCount)") }
        if (index == questions.lastIndex && !allAnswered) Text("Jawab semua soal untuk menyelesaikan tes. Gunakan Sebelumnya untuk memeriksa jawaban.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HistoryScoreCard(title: String, score: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(62.dp).background(Color(0xFFFFF0E3), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Text(score?.toString() ?: "🧠", color = orange, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(if (score == null) "Asesmen kepribadian" else "Skor: $score / 100", color = teal)
                if (score != null) LinearProgressIndicator(progress = { score.coerceIn(0, 100) / 100f },
                    color = teal, trackColor = Color(0xFFE8F0F3), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ScoreStat(icon: String, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(icon, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text(value, color = Color(0xFF14212C), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Color(0xFF14212C), style = MaterialTheme.typography.bodySmall)
    }
}
