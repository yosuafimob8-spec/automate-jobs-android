package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.automatesmartbot.data.autoapply.AutoApplyConfirmation
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.network.AutoApplyEnableRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun KirimLamaranScreen(isPro: Boolean, isOnline: Boolean, onNavigateProfil: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("auto_apply_consent", 0) }
    val scope = rememberCoroutineScope()
    var accountId by remember { mutableStateOf<String?>(null) }
    var consentSaved by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }
    var gmailConnected by remember { mutableStateOf(false) }
    var statusLoaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var deliveries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    suspend fun refresh() {
        statusLoaded = false
        val response = ApiClient.apiService.getAutoApplyStatus()
        if (!response.isSuccessful) { response.errorBody()?.close(); error("Status Auto Apply gagal (${response.code()}).") }
        val body = JSONObject(response.body()?.use { it.string() } ?: error("Status kosong."))
        require(body.has("gmail_connected") && body.has("auto_apply")) { "Status Auto Apply dari server belum lengkap." }
        enabled = body.optJSONObject("auto_apply")?.optBoolean("enabled") == true
        gmailConnected = body.optBoolean("gmail_connected")
        val a = body.optJSONObject("auto_apply")
        consentSaved = AutoApplyConfirmation.consent(a?.optBoolean("consent_granted") == true, a?.optString("consent_at"), accountId?.let { prefs.getBoolean(it, false) } == true)
        statusLoaded = true
    }
    suspend fun load() {
        busy = true
        try {
            val me = ApiClient.apiService.getMe()
            accountId = me.takeIf { it.isSuccessful }?.body()?.id
            me.errorBody()?.close()
            refresh()
            if (isPro) {
                val response = ApiClient.apiService.getKarirMateCandidates()
                if (response.isSuccessful) {
                    val array = JSONObject(response.body()?.use { it.string() } ?: "{}").optJSONArray("items")
                    candidates = if (array == null) emptyList() else (0 until array.length()).map { array.getJSONObject(it) }
                } else { response.errorBody()?.close(); message = "Kandidat gagal dimuat (${response.code()})." }
            }
            val report = ApiClient.apiService.getKarirMateReport()
            if (report.isSuccessful) {
                val array = JSONObject(report.body()?.use { it.string() } ?: "{}").optJSONArray("items")
                deliveries = if (array == null) emptyList() else (0 until array.length()).map { array.getJSONObject(it) }
            } else report.errorBody()?.close()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = e.message ?: "Koneksi terputus. Coba muat ulang." }
        finally { busy = false }
    }
    LaunchedEffect(isPro) { load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Kirim Lamaran Otomatis", style = MaterialTheme.typography.headlineSmall)
            Text("Lowongan aktif yang sesuai CV dan lokasi didahulukan. Lamaran spontan harus menggunakan kontak perusahaan yang tersedia; tidak dianggap lowongan terverifikasi.")
        }
        item {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { scope.launch { load() } }, enabled = !busy && isOnline) { Text("Muat Ulang Status") }
            Text(if (!statusLoaded) "Status belum tersedia" else "Gmail: ${if (gmailConnected) "Terhubung" else "Belum terhubung"} • Auto Apply: ${if (enabled) "Aktif" else "Jeda"}")
            TextButton(onClick = onNavigateProfil) { Text("Periksa Profil & CV") }
        }
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Persetujuan Auto Apply", style = MaterialTheme.typography.titleMedium)
                    Text("Saya mengizinkan penggunaan CV dan profil untuk mengirim lamaran melalui Gmail, dengan batas yang diminta maksimal 5 email per hari. Persetujuan berlaku sampai Auto Apply dijeda. Pengiriman yang hasilnya belum pasti harus diperiksa sebelum dicoba ulang.")
                    if (!consentSaved) Row {
                        Checkbox(agreed, { agreed = it })
                        Text("Saya menyetujui pengiriman otomatis", Modifier.padding(top = 12.dp))
                    } else Text("Persetujuan akun telah tersimpan.")
                    if (!gmailConnected) Text("Akun Gmail perlu terhubung sebelum pengiriman dapat diaktifkan.")
                    if (!isPro) Text("Auto Apply memerlukan langganan PRO aktif.")
                    Button(onClick = { scope.launch {
                        busy = true
                        try {
                            val response = ApiClient.apiService.enableAutoApply(AutoApplyEnableRequest(consent = agreed || consentSaved))
                            if (!response.isSuccessful) { response.errorBody()?.close(); error("Aktivasi ditolak (${response.code()}). Periksa izin Gmail dan langganan.") }
                            response.body()?.close()
                            refresh()
                            if (enabled) {
                                accountId?.let { prefs.edit().putBoolean(it, true).apply() }
                                consentSaved = true
                                message = "Auto Apply aktif menurut server."
                            } else message = "Server belum mengaktifkan Auto Apply."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = e.message ?: "Aktivasi gagal. Muat ulang status sebelum mencoba lagi." }
                        finally { busy = false }
                    } }, enabled = statusLoaded && gmailConnected && isPro && isOnline && !busy && !enabled && (agreed || consentSaved), modifier = Modifier.fillMaxWidth()) { Text(if (enabled) "Auto Apply Aktif" else "Aktifkan Auto Apply") }
                    OutlinedButton(onClick = { scope.launch {
                        busy = true
                        try {
                            val response = ApiClient.apiService.pauseAutoApply()
                            if (!response.isSuccessful) { response.errorBody()?.close(); error("Gagal menjeda (${response.code()}).") }
                            response.body()?.close(); refresh()
                            message = if (enabled) "Server belum menjeda pengiriman." else "Auto Apply dijeda."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = e.message ?: "Jeda gagal. Periksa status server." }
                        finally { busy = false }
                    } }, enabled = statusLoaded && enabled && !busy && isOnline, modifier = Modifier.fillMaxWidth()) { Text("Jeda Auto Apply") }
                }
            }
        }
        item {
            Text("Kandidat dari server", style = MaterialTheme.typography.titleMedium)
            if (candidates.isEmpty()) Text("Belum ada kandidat yang tersedia.")
            candidates.forEach { c ->
                Text("${c.optString("company")} • ${c.optString("position")}\n${c.optString("email")}")
                Text(if (c.optBoolean("vacancy_verified") && c.optBoolean("active")) "Lowongan aktif menurut server" else "Kontak / peluang spontan; lowongan belum terverifikasi", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("Riwayat dari server", style = MaterialTheme.typography.titleMedium)
            if (deliveries.isEmpty()) Text("Belum ada riwayat yang tersedia dari server.")
            deliveries.forEach { d ->
                val gmailId = d.optString("gmail_message_id").takeIf(AutoApplyConfirmation::hasReference) ?: d.optString("gmail_id")
                val confirmed = AutoApplyConfirmation.sent(d.optString("status"), gmailId)
                Text("${d.optString("company")} • ${if (confirmed) "Terkirim (Gmail)" else if (d.optString("status") == "sent") "Menunggu konfirmasi Gmail" else d.optString("status", "Belum dikonfirmasi")}")
            }
        }
    }
}
