package com.example.automatesmartbot.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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

    LaunchedEffect(Unit) { vm.loadCatalog(); vm.loadExtras() }
    LaunchedEffect(session, result) {
        if (result != null) page = "result"
        else if (session != null) page = "exam"
    }
    fun back() {
        page = when (page) {
            "home" -> { onExit(); "home" }
            "exam" -> "exam"
            else -> "home"
        }
    }
    BackHandler { back() }
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F9FC))) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { back() }) { Text("‹", color = ink, style = MaterialTheme.typography.headlineMedium) }
            Text(when (page) {
                "home" -> "Karir Mate • Psikotes"
                "catalog" -> "Pilih Jenis Tes"
                "exam" -> "Kerjakan Soal"
                "result" -> "Hasil Psikotes"
                "history" -> "Riwayat Ujian"
                "stats" -> "Statistik"
                else -> "Peringkat"
            }, fontWeight = FontWeight.Bold, color = ink)
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = teal)
        error?.let { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error) }
        when (page) {
            "home" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Banner("Psikotes", "Latihan psikotes, tingkatkan kemampuan, raih karir impianmu!", listOf(Color(0xFFD8F8FA), Color(0xFFFFE9D8)), true) {} }
                item { Banner("💼  Tes Kerja Swasta", "Latihan psikotes untuk seleksi perusahaan swasta", listOf(orange, Color(0xFFFF9B65))) { family = "private"; page = "catalog" } }
                item { Banner("🏛  Persiapan CPNS", "Latihan SKD dan SKB sesuai kebutuhan", listOf(blue, teal)) { family = "cpns"; page = "catalog" } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Shortcut("🕘 Riwayat", Modifier.weight(1f)) { vm.loadExtras(); page = "history" }
                        Shortcut("📊 Statistik", Modifier.weight(1f)) { vm.loadExtras(); page = "stats" }
                        Shortcut("🏆 Peringkat", Modifier.weight(1f)) { vm.loadExtras(); page = "leaderboard" }
                    }
                }
            }
            "catalog" -> Column(Modifier.padding(16.dp)) {
                Text("Pilih Jenis Tes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ink)
                Text("Latih kemampuan sesuai kebutuhan.", color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Semua" to "all", "Swasta" to "private", "CPNS" to "cpns").forEach { (label, value) ->
                        FilterChip(selected = family == value, onClick = { family = value }, label = { Text(label) })
                    }
                }
                val visible = modes.filter {
                    val f = it.s("family").lowercase()
                    family == "all" || f == family || (family == "cpns" && f == "skb")
                }
                if (visible.isEmpty() && !loading) Text("Belum ada tes tersedia dari server.")
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visible) { mode ->
                        Card(onClick = { vm.start(mode.s("test_id")) }, modifier = Modifier.height(166.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🧠", style = MaterialTheme.typography.headlineMedium)
                                Text(mode.s("title", "Tes"), fontWeight = FontWeight.Bold, color = ink, maxLines = 2)
                                Text("${mode.n("question_count")} soal • ${mode.n("duration_seconds") / 60} menit", style = MaterialTheme.typography.bodySmall)
                                Text("Mulai  ›", color = teal)
                            }
                        }
                    }
                }
            }
            "exam" -> session?.let { ExamPanel(it, questions, vm::answer, vm::submit) }
            "result" -> result?.let { r ->
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Banner("🏆  Kerja bagus!", "Kamu telah menyelesaikan tes psikotes.", listOf(Color(0xFFD8F8FA), Color(0xFFFFE9D8)), true) {} }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${r.n("percent")}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = teal)
                                Text("Skor ${r.n("score")} dari ${r.n("max_score")}")
                            }
                        }
                    }
                    item { Text("Pembahasan Jawaban", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(r.getAsJsonArray("questions")?.map { it.asJsonObject } ?: emptyList()) { q ->
                        Info(q.s("prompt"), q.s("explanation", "Pembahasan belum tersedia"))
                    }
                    item {
                        Button(onClick = vm::retryWrong, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Ulangi Soal Salah") }
                        OutlinedButton(onClick = { page = "home"; vm.loadExtras() }, modifier = Modifier.fillMaxWidth()) { Text("Kembali ke Beranda") }
                    }
                }
            }
            "history" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (history.isEmpty()) item { Text("Belum ada riwayat.") }
                items(history) { Info(it.s("title", "Tes"), "Skor: ${it.s("score", "-")}") }
            }
            "stats" -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats?.let {
                    Info("Total Tes", it.s("total_tests", "0"))
                    Info("Rata-rata", it.s("average_percent", "-"))
                    Info("Peningkatan", it.s("improvement_percent", "-"))
                } ?: Text("Belum ada statistik.")
            }
            "leaderboard" -> LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (leaderboard.isEmpty()) item { Text("Belum ada peringkat.") }
                items(leaderboard) { Info("🏆 ${it.s("alias", "Peserta")}", "Skor: ${it.s("score", "-")}") }
            }
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
    Card(onClick = onClick, modifier = modifier.height(90.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { Text(label, color = ink) }
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
    var confirm by remember { mutableStateOf(false) }
    var submitted by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        while (remaining > 0) { delay(1000); remaining-- }
        if (!submitted) { submitted = true; onSubmit() }
    }
    val answered = questions.count { it.get("selected_option_id")?.takeUnless { v -> v.isJsonNull } != null }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Kirim ujian?") },
        text = { Text("Semua soal sudah dijawab. Kirim jawaban ke server?") },
        confirmButton = { TextButton(onClick = { confirm = false; if (!submitted) { submitted = true; onSubmit() } }) { Text("Kirim") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } }
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Info("⏱  Sisa Waktu", "%02d:%02d".format(remaining / 60, remaining % 60))
        Text("Soal ${index + 1} dari ${questions.size}", fontWeight = FontWeight.Bold, color = ink)
        LinearProgressIndicator(progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth(), color = blue)
        questions.getOrNull(index)?.let { q ->
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Text(q.s("prompt"), fontWeight = FontWeight.SemiBold, color = ink) }
                    items(q.getAsJsonArray("options")?.map { it.asJsonObject } ?: emptyList()) { option ->
                        val selected = q.s("selected_option_id") == option.s("id")
                        OutlinedButton(
                            onClick = { onAnswer(q.s("question_id"), option.s("id")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) Color(0xFFDDF5FA) else Color.White)
                        ) { Text(option.s("text"), color = ink) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("← Sebelumnya") }
            Button(onClick = { index++ }, enabled = index < questions.lastIndex, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = orange)) { Text("Berikutnya →") }
        }
        Text("Terjawab $answered/${questions.size}", color = Color.Gray)
        Button(onClick = { confirm = true }, enabled = questions.isNotEmpty() && answered == questions.size && !submitted, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = teal)) { Text("Kirim Ujian") }
    }
}
