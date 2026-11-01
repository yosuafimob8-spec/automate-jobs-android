package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.autoapply.*
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.ui.components.AppInput
import com.example.automatesmartbot.ui.components.JourneyHeader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDirectoryScreen() {
    val scope = rememberCoroutineScope(); val links = LocalUriHandler.current
    var provinces by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var province by rememberSaveable { mutableStateOf("") }; var city by rememberSaveable { mutableStateOf("") }; var industry by rememberSaveable { mutableStateOf("") }; var position by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) }; var stats by remember { mutableStateOf<JSONObject?>(null) }; var next by remember { mutableStateOf<Int?>(null) }; var total by remember { mutableStateOf<Int?>(null) }; var ownerStatus by remember { mutableStateOf("") }
    var appliedFilters by remember { mutableStateOf<List<String>?>(null) }
    suspend fun load(more: Boolean = false) {
        if (busy) return
        val filters = if (more) appliedFilters ?: return else listOf(province, city, industry, position)
        busy = true; error = null
        try {
            if (provinces.isEmpty()) provinces = ApiClient.apiService.companyProvinces().readJson().objects("provinces")
            val body = ApiClient.apiService.companies(filters[0].ifBlank { null }, filters[1].ifBlank { null }, filters[2].ifBlank { null }, filters[3].ifBlank { null }, if (more) next ?: 0 else 0).readJson()
            appliedFilters = filters
            rows = if (more) rows + body.objects("items") else body.objects("items"); stats = body.getJSONObject("stats"); total = body.getInt("total"); ownerStatus = body.text("owner_data_status"); next = if (body.isNull("next_offset")) null else body.getInt("next_offset")
        } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message ?: "Direktori belum dapat dimuat." } finally { busy = false }
    }
    LaunchedEffect(Unit) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { JourneyHeader("Database Perusahaan Indonesia", "Cari secara nasional. Wilayah tujuan Auto Apply tetap mengikuti preferensi profil Anda.") }
        item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { if (!busy) menu = it }) {
                OutlinedTextField(value = provinces.firstOrNull { it.text("code") == province }?.text("name") ?: "Semua provinsi", onValueChange = {}, readOnly = true, label = { Text("Provinsi") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Semua provinsi") }, onClick = { province = ""; menu = false })
                    provinces.forEach { p -> DropdownMenuItem(text = { Text(p.text("name")) }, onClick = { province = p.text("code"); menu = false }) }
                }
            }
            AppInput("Kota / kabupaten", city, { city = it }); AppInput("Industri", industry, { industry = it }); AppInput("Posisi yang dipublikasikan (opsional)", position, { position = it })
            Text("Kontak rekrutmen umum dapat tidak mencantumkan posisi. Alamat tersebut tetap tampil jika filter posisi dikosongkan.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { scope.launch { load() } }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Cari / Segarkan") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }
        stats?.let { s -> item { Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${s.optInt("companies")} perusahaan · ${s.optInt("emails")} email unik", style = MaterialTheme.typography.titleMedium)
            Text("Pemilik: ${s.optInt("owner_emails")} · Pencarian baru: ${s.optInt("new_emails")}")
            if (ownerStatus == "account_not_linked") Text("Daftar pemilik belum dapat dibaca: tautkan akun Telegram pada profil.", color = MaterialTheme.colorScheme.error)
            Text("Provinsi dengan kontak: ${s.optInt("provinces_with_contacts")} / katalog ${s.optInt("province_catalogue")}")
            Text("Data belum lengkap: ${s.optInt("incomplete_records")} · hasil filter: ${total ?: 0}")
            Text("Alamat yang dipublikasikan belum berarti ada lowongan aktif atau bahwa email akan diterima HR.")
        } } } }
        if (!busy && error == null && total == 0) item { Text("Belum ada kontak sesuai filter. Coba filter lain atau lengkapi lokasi/sumber pada data impor pemilik.") }
        items(rows, key = { it.text("id") }) { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.text("company"), style = MaterialTheme.typography.titleMedium); Text(r.text("email"))
            Text(r.text("location").ifBlank { "Lokasi belum tersedia" }); Text(r.text("industry").ifBlank { "Industri belum terverifikasi" })
            Text(if (r.text("origin") == "owner_import") "Sumber: database / file pemilik" else "Sumber: publikasi rekrutmen")
            Text("Pemeriksaan: ${r.text("checked_at")}", style = MaterialTheme.typography.bodySmall)
            Text("Lowongan: ${r.text("vacancy_status")} · Penggunaan email: ${r.text("email_policy")}")
            if ((r.optJSONArray("missing_fields")?.length() ?: 0) > 0) Text("Belum lengkap: ${r.optJSONArray("missing_fields")}", color = MaterialTheme.colorScheme.error)
            if (r.text("source_url").startsWith("https://")) TextButton(onClick = { runCatching { links.openUri(r.text("source_url")) }.onFailure { error = "Sumber belum dapat dibuka." } }) { Text("Periksa Sumber") }
            else Text(r.text("source_reference"), style = MaterialTheme.typography.bodySmall)
        } } }
        if (next != null) item { OutlinedButton(onClick = { scope.launch { load(true) } }, enabled = !busy && appliedFilters == listOf(province, city, industry, position)) { Text("Muat Berikutnya") } }
    }
}
