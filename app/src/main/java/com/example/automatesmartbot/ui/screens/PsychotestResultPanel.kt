package com.example.automatesmartbot.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.psychotest.PsychotestPolicy
import com.example.automatesmartbot.ui.components.JourneyHeader
import com.google.gson.JsonObject

private fun JsonObject.text(key: String) = get(key)?.takeUnless { it.isJsonNull }?.asString.orEmpty()

@Composable
internal fun PsychotestResultPanel(result: JsonObject, isPro: Boolean, onDone: () -> Unit, onPro: () -> Unit) {
    val score = PsychotestPolicy.normalizedScore(result.get("score")?.takeUnless { it.isJsonNull }?.asDouble, result.get("max_score")?.takeUnless { it.isJsonNull }?.asDouble)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val progress by animateFloatAsState(if (entered) (score ?: 0) / 100f else 0f, animationSpec = tween(800), label = "Hasil psikotes")
    val motivation = when {
        score == null -> "Kenali kecenderungan Anda sebagai bahan refleksi. Hasil ini bukan diagnosis psikologis."
        score < 40 -> "Mulai dari konsep dasar. Satu sesi latihan teratur setiap hari membantu membangun kemampuan."
        score < 60 -> "Pemahaman mulai berkembang. Tinjau jawaban yang sulit dan coba strategi baru."
        score < 80 -> "Hasil baik! Tingkatkan ketelitian dan pengelolaan waktu agar makin konsisten."
        else -> "Penguasaan sangat baik. Pertahankan kebiasaan belajar dan tantang diri dengan variasi soal."
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { JourneyHeader("Tes Selesai", "${result.text("title")} · ${if (isPro) "PRO" else "FREE"}") }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (score != null) Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(140.dp), strokeWidth = 10.dp)
                Text("$score/100", style = MaterialTheme.typography.headlineMedium)
            } else Text("Profil Kepribadian", style = MaterialTheme.typography.headlineSmall)
            Text(motivation, style = MaterialTheme.typography.bodyLarge)
        } } }
        item { Text("Ringkasan Kemampuan", style = MaterialTheme.typography.titleLarge)
            Text("Benar: ${result.text("correct_count").ifBlank { "—" }} · Salah: ${result.text("wrong_count").ifBlank { "—" }} · Kosong: ${result.text("blank_count").ifBlank { "—" }}")
            Text("${result.text("result_label")} · Hasil dinilai server dan tersimpan pada riwayat. Skor latihan dinormalisasi 1–100.")
        }
        if (isPro) {
            item { Text("Analisis Mendalam · PRO", style = MaterialTheme.typography.titleLarge) }
            items(result.getAsJsonArray("categories")?.map { it.asJsonObject }.orEmpty()) { c ->
                val percent = c.get("percent")?.takeUnless { it.isJsonNull }?.asFloat
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.text("label"), style = MaterialTheme.typography.titleMedium)
                    if (percent != null) {
                        LinearProgressIndicator(progress = { (percent/100).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text("Capaian materi: $percent%")
                        Text(if (percent < 60) "Pengembangan: pelajari konsep materi ini, bahas soal salah atau kosong, lalu ulangi latihan." else "Pengembangan: pertahankan akurasi sambil melatih kecepatan dan variasi soal.")
                    } else Text("Materi ini menggunakan evaluasi kecenderungan, tanpa nilai benar/salah.")
                } }
            }
            result.getAsJsonObject("personality")?.getAsJsonArray("dimensions")?.let { dimensions -> items(dimensions.map { it.asJsonObject }) { d -> Text("${d.text("label")}: ${d.text("description")}") } }
            item { Text("Pembahasan Jawaban · PRO", style = MaterialTheme.typography.titleMedium) }
            items(result.getAsJsonArray("questions")?.map { it.asJsonObject }.orEmpty()) { q -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(q.text("prompt"), style = MaterialTheme.typography.titleMedium)
                Text(q.text("explanation").ifBlank { "Pembahasan belum tersedia dari server." })
            } } }
        } else item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("Analisis Mendalam & Rekomendasi · PRO", style = MaterialTheme.typography.titleMedium)
            Text("Evaluasi per materi dan pembahasan terperinci tersedia untuk langganan PRO aktif.")
            OutlinedButton(onClick = onPro) { Text("Lihat Akses PRO") }
        } } }
        item { Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Selesai") } }
    }
}
