package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.automatesmartbot.data.cv.readBytesLimited
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
    var operatorSendAllowed by remember { mutableStateOf(false) }
    var cvUploaded by remember { mutableStateOf(false) }
    var gmailConnected by remember { mutableStateOf(false) }
    var statusLoaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var deliveries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    suspend fun refresh() {
        statusLoaded = false
        val response = ApiClient.apiService.getAutoApplyStatus()
        if (!response.isSuccessful) { if (response.code() in listOf(401,403)) gmailConnected = false; response.errorBody()?.close(); error("Status Auto Apply gagal (${response.code()}). Periksa sesi dan otorisasi Gmail.") }
        val body = JSONObject(response.body()?.use { it.string() } ?: error("Status kosong."))
        require(body.has("gmail_connected") && body.has("auto_apply")) { "Status Auto Apply dari server belum lengkap." }
        enabled = body.optJSONObject("auto_apply")?.optBoolean("enabled") == true
        gmailConnected = body.optBoolean("gmail_connected")
        cvUploaded = body.optBoolean("cv_uploaded")
        operatorSendAllowed = body.optBoolean("operator_send_allowed")
        val a = body.optJSONObject("auto_apply")
        consentSaved = AutoApplyConfirmation.consentState(a?.opt("consent_granted") as? Boolean, a?.optString("consent_at"), accountId?.let { prefs.getBoolean(it, false) } == true)
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
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(2_200_000) } ?: error("PDF tidak dapat dibaca.")
                }
                require(bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-") { "Pilih file PDF valid." }
                val response = ApiClient.apiService.uploadAutoApplyCv(bytes.toRequestBody("application/pdf".toMediaType()))
                if (!response.isSuccessful) { response.errorBody()?.close(); error("Upload CV ditolak (${response.code()}).") }
                response.body()?.close(); refresh(); message = "CV tersimpan di backend untuk akun ini."
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = e.message ?: "Upload CV gagal." }
            finally { busy = false }
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) scope.launch { load() } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(isPro) { load() }
    LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF5F8FC)), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KARIRMATE  •  PRO", color = Color(0xFFFF762C), fontWeight = FontWeight.Bold)
                    Text("Kirim Lamaran Otomatis", color = Color(0xFF242424), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("Kelola CV, persetujuan, kandidat, dan riwayat pengiriman dari satu halaman.", color = Color(0xFF525B66))
                    Text("Maksimal 5 email per hari • Tanpa duplikasi", color = Color(0xFF00A99D), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Status & Kesiapan", color = Color(0xFF242424), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { scope.launch { load() } }, enabled = !busy && isOnline) { Text("Muat Ulang Status") }
            Text(if (!statusLoaded) "Status belum tersedia" else "Gmail: ${if (gmailConnected) "Terhubung" else "Belum terhubung"} • Izin Auto Apply: ${if (enabled) "Aktif" else "Jeda"}")
            Text(if (operatorSendAllowed) "Izin operasional tersedia; jadwal pengiriman belum diverifikasi dari aplikasi." else "Pengiriman produksi belum diizinkan operator; izin akun dapat disimpan tanpa memulai pengiriman.")
            TextButton(onClick = onNavigateProfil) { Text("Periksa Profil & CV") }
            Text(if (cvUploaded) "CV backend: tersimpan" else "CV backend: belum diunggah")
            OutlinedButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }, enabled = isOnline && !busy) { Text("Unggah / Ganti CV PDF") }
            if (!gmailConnected) Button(onClick = { scope.launch {
                busy = true
                try {
                    val response = ApiClient.apiService.startGmailOAuth()
                    if (!response.isSuccessful) { response.errorBody()?.close(); error("OAuth belum dapat dimulai (${response.code()}).") }
                    val url = JSONObject(response.body()?.use { it.string() } ?: "{}").getString("url")
                    val uri = android.net.Uri.parse(url)
                    require(uri.scheme == "https" && uri.host == "accounts.google.com" && uri.userInfo == null && uri.port in listOf(-1, 443)) { "URL otorisasi Google tidak valid." }
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = e.message ?: "OAuth gagal dimulai." }
                finally { busy = false }
            } }, enabled = isOnline && !busy) { Text("Hubungkan Gmail") }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Izin & Kontrol Auto Apply", color = Color(0xFF242424), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
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
                                message = "Izin Auto Apply tersimpan. Pengiriman mengikuti konfigurasi dan jadwal yang disetujui operator."
                            } else message = "Server belum mengaktifkan Auto Apply."
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { message = e.message ?: "Aktivasi gagal. Muat ulang status sebelum mencoba lagi." }
                        finally { busy = false }
                    } }, enabled = statusLoaded && gmailConnected && cvUploaded && isPro && isOnline && !busy && !enabled && (agreed || consentSaved), modifier = Modifier.fillMaxWidth()) { Text(if (enabled) "Izin Auto Apply Tersimpan" else "Simpan Izin Auto Apply") }
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
            Text("Kandidat Lamaran", color = Color(0xFF242424), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            if (candidates.isEmpty()) Text("Belum ada kandidat yang tersedia.")
            candidates.forEach { c ->
                Text("${c.optString("company")} • ${c.optString("position")}\n${c.optString("email")}")
                Text(if (c.optBoolean("vacancy_verified") && c.optBoolean("active")) "Lowongan aktif menurut server" else "Kontak / peluang spontan; lowongan belum terverifikasi", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text("Riwayat Pengiriman", color = Color(0xFF242424), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            if (deliveries.isEmpty()) Text("Belum ada riwayat yang tersedia dari server.")
            deliveries.forEach { d ->
                val gmailId = d.optString("gmail_message_id").takeIf(AutoApplyConfirmation::hasReference) ?: d.optString("gmail_id")
                val confirmed = AutoApplyConfirmation.sent(d.optString("status"), gmailId)
                Text("${d.optString("company")} • ${if (confirmed) "Terkirim (Gmail)" else if (d.optString("status") == "sent") "Menunggu konfirmasi Gmail" else d.optString("status", "Belum dikonfirmasi")}")
            }
        }
    }
}
