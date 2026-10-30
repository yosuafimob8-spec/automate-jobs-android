package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import com.example.automatesmartbot.ui.viewmodel.PsychotestViewModel

@Composable
fun LatihanPsikotesScreen(onExit: () -> Unit = {}) {
    val vm: PsychotestViewModel = viewModel()
    val modes by vm.catalog.collectAsState(); val questions by vm.questions.collectAsState()
    val session by vm.session.collectAsState(); val result by vm.result.collectAsState()
    val loading by vm.loading.collectAsState(); val error by vm.error.collectAsState()
    val history by vm.history.collectAsState(); val stats by vm.stats.collectAsState(); val leaderboard by vm.leaderboard.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var catalogLevel by remember { mutableStateOf<String?>(null) }
    fun navigateBack() {
        catalogLevel = when (catalogLevel) {
            "private", "cpnsMenu" -> null
            "skd", "skb" -> "cpnsMenu"
            else -> { onExit(); null }
        }
    }
    BackHandler(enabled = session == null && result == null) { navigateBack() }
    LaunchedEffect(Unit) { vm.loadCatalog(); vm.loadExtras() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            result != null -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("Hasil: ${result!!.get("score")} / ${result!!.get("max_score")}", style = MaterialTheme.typography.titleLarge); Text("${result!!.get("percent")}%"); if (result!!.getAsJsonObject("analysis")?.get("retry_wrong")?.isJsonNull == false) Button(onClick = vm::retryWrong) { Text("Ulangi Soal Salah") } }
                items(result!!.getAsJsonArray("questions")?.map { it.asJsonObject } ?: emptyList()) { q -> Card { Column(Modifier.padding(12.dp)) { Text(q.get("prompt").asString); Text(q.get("explanation").asString) } } }
            }
            session != null -> ExamSessionContent(
                sessionId = session!!.get("id").asString,
                serverRemainingSeconds = session!!.get("remaining_seconds").asInt,
                questions = questions,
                onAnswer = vm::answer,
                onSubmit = vm::submit
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TabRow(selectedTabIndex = tab) { listOf("Latihan", "Riwayat", "Statistik", "Peringkat").forEachIndexed { i, label -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) }) } }
                when (tab) {
                    0 -> {
                        val visible = when (catalogLevel) { "private" -> modes.filter { it.get("family")?.asString == "private" }; "skd" -> modes.filter { it.get("family")?.asString == "cpns" }; "skb" -> modes.filter { it.get("family")?.asString == "skb" }; else -> emptyList() }
                        if (catalogLevel == null) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pusat Latihan Psikotes", style = MaterialTheme.typography.headlineSmall)
                            Text("Pilih jalur latihan sesuai tujuan kariermu.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CatalogEntry("Tes Kerja Swasta", "Latihan psikotes untuk seleksi perusahaan", Color(0xFFFF6B3D), Color(0xFFFF9A54)) { catalogLevel = "private" }
                            CatalogEntry("Persiapan CPNS", "Latihan SKD dan SKB berdasarkan formasi", Color(0xFF146FEA), Color(0xFF12CBE8)) { catalogLevel = "cpnsMenu" }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { SecondaryEntry("Riwayat") { tab = 1 }; SecondaryEntry("Statistik") { tab = 2 }; SecondaryEntry("Peringkat") { tab = 3 } }
                        } else Column {
                            TextButton(onClick = { navigateBack() }) { Text("Kembali") }
                            if (catalogLevel == "cpnsMenu") LazyVerticalGrid(columns = GridCells.Adaptive(156.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { item { CatalogEntry("SKD", "TWK, TIU, TKP, simulasi CAT", Color(0xFF146FEA), Color(0xFF12CBE8)) { catalogLevel = "skd" } }; item { CatalogEntry("SKB", "Pilih formasi yang tersedia", Color(0xFF146FEA), Color(0xFF12CBE8)) { catalogLevel = "skb" } } } else LazyVerticalGrid(columns = GridCells.Adaptive(156.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(visible) { m -> Card(onClick = { vm.start(m.get("test_id").asString) }) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(m.get("formation_label")?.takeUnless { it.isJsonNull }?.asString ?: m.get("family_label")?.asString ?: "Latihan", style = MaterialTheme.typography.labelSmall); Text(m.get("title").asString, style = MaterialTheme.typography.titleSmall, maxLines = 2); Text("${m.get("question_count").asInt} soal | ${m.get("duration_seconds").asInt / 60} menit") } } } }
                        }
                    }
                    1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (history.isEmpty()) item { Text("Belum ada riwayat ujian") }; items(history) { h -> Card { Column(Modifier.padding(12.dp)) { Text(h.get("title")?.asString ?: "Tes"); Text("Skor: ${h.get("score") ?: "Belum ada data"}") } } } }
                    2 -> stats?.let { Card { Column(Modifier.padding(16.dp)) { Text("Statistik", style = MaterialTheme.typography.titleMedium); Text("Total tes: ${it.get("total_tests")}"); Text("Rata-rata: ${it.get("average_percent")?.takeUnless { e -> e.isJsonNull }?.asString ?: "Belum ada data"}"); Text("Perkembangan: ${it.get("improvement_percent")?.takeUnless { e -> e.isJsonNull }?.asString ?: "Belum ada data"}") } } } ?: Text("Belum ada data")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (leaderboard.isEmpty()) item { Text("Belum ada peringkat") }; items(leaderboard) { e -> Card { Row(Modifier.padding(12.dp)) { Text(e.get("alias")?.asString ?: "Peserta anonim", Modifier.weight(1f)); Text(e.get("score")?.toString() ?: "-") } } } }
                }
            }
        }
    }
}

@Composable
private fun CatalogEntry(title: String, subtitle: String, start: Color, end: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(132.dp), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(start, end))).padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White) } }
}

@Composable private fun SecondaryEntry(label: String, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = Modifier.height(54.dp), contentPadding = PaddingValues(4.dp)) { Text(label, maxLines = 1) } }

@Composable
private fun ExamSessionContent(
    sessionId: String,
    serverRemainingSeconds: Int,
    questions: List<com.google.gson.JsonObject>,
    onAnswer: (String, String) -> Unit,
    onSubmit: () -> Unit
) {
    var index by remember(sessionId) { mutableIntStateOf(0) }
    var remaining by remember(sessionId) { mutableIntStateOf(serverRemainingSeconds) }
    LaunchedEffect(sessionId, serverRemainingSeconds) {
        remaining = serverRemainingSeconds
    }
    LaunchedEffect(sessionId) {
        while (remaining > 0) {
            kotlinx.coroutines.delay(1_000)
            remaining -= 1
        }
    }
    LaunchedEffect(remaining) {
        if (remaining == 0 && questions.isNotEmpty()) onSubmit()
    }
    val question = questions.getOrNull(index)
    val answered = questions.count { it.get("selected_option_id")?.isJsonNull == false }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Soal ${index + 1}/${questions.size} | Terjawab $answered/${questions.size}")
        Text("Sisa waktu: ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}")
        LinearProgressIndicator(
            progress = { if (questions.isEmpty()) 0f else answered.toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        question?.let { q ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(q.get("prompt").asString)
                    q.getAsJsonArray("options").forEach { raw ->
                        val option = raw.asJsonObject
                        val selected = q.get("selected_option_id")?.takeUnless { it.isJsonNull }?.asString == option.get("id").asString
                        OutlinedButton(
                            onClick = { onAnswer(q.get("question_id").asString, option.get("id").asString) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        ) { Text(option.get("text").asString) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (index > 0) index-- }, enabled = index > 0, modifier = Modifier.weight(1f)) { Text("Sebelumnya") }
            OutlinedButton(onClick = { if (index < questions.lastIndex) index++ }, enabled = index < questions.lastIndex, modifier = Modifier.weight(1f)) { Text("Berikutnya") }
        }
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("Kirim Ujian") }
    }
}
