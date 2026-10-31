package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.autoapply.*
import com.example.automatesmartbot.data.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun RiwayatScreen() {
    val repo = remember { AutoApplyReportRepository(ApiClient.apiService) }
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf("today") }
    var report by remember { mutableStateOf<DeliveryReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    suspend fun load(more: Boolean = false) {
        busy = true; error = null
        try {
            val next = repo.report(period, if (more) report?.nextOffset ?: 0 else 0)
            report = if (more) next.copy(items = report?.items.orEmpty() + next.items) else next
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Laporan gagal disinkronkan." }
        finally { busy = false }
    }
    LaunchedEffect(period) { report = null; load() }
    detail?.let { d -> AlertDialog(onDismissRequest = { detail = null }, title = { Text(d.text("company")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(d.text("job_title")); Text(d.text("email")); Text(DeliveryStatus.label(d))
            Text("Waktu server: ${d.text("timestamp").ifBlank { d.text("sent_at") }.ifBlank { "Belum tersedia" }}")
            Text(if (d.text("kind") == "spontaneous") "Lamaran spontan; tidak menyatakan ada lowongan aktif." else "Jenis: ${d.text("kind")}")
            if (d.text("gmail_message_id").isNotBlank()) Text("ID Gmail: ${d.text("gmail_message_id")}")
            if (d.text("error").isNotBlank()) Text("Kesalahan: ${d.text("error")}")
            Text("Penerima yang sudah diklaim tidak dikirim ulang otomatis, termasuk hasil kirim yang belum pasti.")
        }
    }, confirmButton = { TextButton(onClick = { detail = null }) { Text("Tutup") } }) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Riwayat & Laporan Lamaran", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Target maksimal 5 email per hari · rekap hari mengikuti WIB. Status berasal dari database backend.") }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("today" to "Hari ini", "7d" to "7 hari", "30d" to "30 hari", "all" to "Semua").forEach { (id,label) -> FilterChip(selected = period == id, onClick = { if (!busy) period = id }, label = { Text(label) }) }
        } }
        item { Button(onClick = { scope.launch { load() } }, enabled = !busy) { Text("Sinkronkan Laporan") }; if (busy) LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error); Text("Data sebelumnya belum diperbarui.") } }
        report?.let { r ->
            item { DeliverySummary(r.summary, r.quota) }
            if (r.items.isEmpty()) item { Card { Column(Modifier.padding(20.dp)) { Text("Belum ada pengiriman pada periode ini", style = MaterialTheme.typography.titleMedium); Text("Aktivasi dan simulasi tidak dihitung sebagai email terkirim. Periksa status Auto Apply jika pengiriman diblokir.") } } }
            items(r.items, key = { it.optLong("id") }) { d -> Card(onClick = { detail = d }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(d.text("company"), style = MaterialTheme.typography.titleMedium); Text(d.text("job_title")); Text(d.text("email")); Text(DeliveryStatus.label(d), color = MaterialTheme.colorScheme.primary)
                    Text(d.text("timestamp"), style = MaterialTheme.typography.bodySmall)
                    if (d.text("error").isNotBlank()) Text(d.text("error"), color = MaterialTheme.colorScheme.error)
                }
            } }
            if (r.nextOffset != null) item { OutlinedButton(onClick = { scope.launch { load(true) } }, enabled = !busy) { Text("Muat Riwayat Berikutnya") } }
        }
    }
}

@Composable
internal fun DeliverySummary(summary: JSONObject, quota: JSONObject) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pengiriman Hari Ini", style = MaterialTheme.typography.titleLarge)
            Text("${summary.optInt("sent_today")} terkirim / target maksimal 5", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { (summary.optInt("sent_today") / 5f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Gagal hari ini: ${summary.optInt("failed_today")} · Tertunda seluruh antrean: ${summary.optInt("pending")}")
            Text("Total terkirim: ${summary.optInt("total_sent")} · Belum pasti: ${summary.optInt("uncertain")}")
            Text("Kuota terpakai/direservasi: ${quota.optInt("quota_used_today")} · Tersisa hari ini: ${quota.optInt("remaining_today")}")
            Text("Status belum pasti tetap memakai kuota untuk mencegah kirim ganda.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
