package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.autoapply.AutoApplyConfirmation
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.network.AutoApplyEnableRequest
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
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
        if (!response.isSuccessful) {
            if (response.code() in listOf(401, 403)) gmailConnected = false
            response.errorBody()?.close()
            error("Status Auto Apply gagal (${response.code()}). Periksa sesi dan otorisasi Gmail.")
        }
        val body = JSONObject(response.body()?.use { it.string() } ?: error("Status kosong."))
        require(body.has("gmail_connected") && body.has("auto_apply")) { "Status Auto Apply dari server belum lengkap." }
        enabled = body.optJSONObject("auto_apply")?.optBoolean("enabled") == true
        gmailConnected = body.optBoolean("gmail_connected")
        cvUploaded = body.optBoolean("cv_uploaded")
        operatorSendAllowed = body.optBoolean("operator_send_allowed")
        val a = body.optJSONObject("auto_apply")
        consentSaved = AutoApplyConfirmation.consentState(
            a?.opt("consent_granted") as? Boolean,
            a?.optString("consent_at"),
            accountId?.let { prefs.getBoolean(it, false) } == true
        )
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
                } else {
                    response.errorBody()?.close()
                    message = "Kandidat gagal dimuat (${response.code()})."
                }
            }
            val report = ApiClient.apiService.getKarirMateReport()
            if (report.isSuccessful) {
                val array = JSONObject(report.body()?.use { it.string() } ?: "{}").optJSONArray("items")
                deliveries = if (array == null) emptyList() else (0 until array.length()).map { array.getJSONObject(it) }
            } else report.errorBody()?.close()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message = e.message ?: "Koneksi terputus. Coba muat ulang."
        } finally {
            busy = false
        }
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
                if (!response.isSuccessful) {
                    response.errorBody()?.close()
                    error("Upload CV ditolak (${response.code()}).")
                }
                response.body()?.close()
                refresh()
                message = "CV tersimpan di backend untuk akun ini."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.message ?: "Upload CV gagal."
            } finally {
                busy = false
            }
        }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { load() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPro) { load() }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(KarirPageGradient),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5F7),
                            border = BorderStroke(1.dp, Color(0xFFC6DFE8))
                        ) {
                            Text(
                                "KARIRMATE • PRO",
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "Kirim Lamaran Otomatis",
                        color = KarirTextDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Kelola CV, persetujuan, kandidat perusahaan, dan riwayat pengiriman resmi dalam satu alur terpusat.",
                        color = KarirTextMuted,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            "Maksimal 5 email per hari • Aman & Tanpa Duplikasi",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Status & Kesiapan",
                            color = KarirTextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        TextButton(
                            onClick = { scope.launch { load() } },
                            enabled = !busy && isOnline
                        ) {
                            Text("Muat Ulang", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (busy) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth(),
                            color = KarirOrange,
                            trackColor = Color(0xFFC6DFE8)
                        )
                    }

                    if (message.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFFD8B2))
                        ) {
                            Text(
                                message,
                                color = Color(0xFF9A3412),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Gmail & Auto Apply Badge Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5FAFC),
                        border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (gmailConnected) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (gmailConnected) Color(0xFF16A34A) else KarirOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    if (!statusLoaded) "Status belum dimuat"
                                    else "Gmail: ${if (gmailConnected) "Terhubung" else "Belum terhubung"} • Status: ${if (enabled) "Aktif" else "Jeda"}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KarirTextDark
                                )
                            }
                            Text(
                                if (operatorSendAllowed) "Izin operasional tersedia; jadwal pengiriman diverifikasi oleh sistem."
                                else "Pengiriman produksi memerlukan izin operasional; Anda tetap dapat menyiapkan konfigurasi.",
                                fontSize = 11.5.sp,
                                color = KarirTextMuted
                            )
                            Text(
                                if (cvUploaded) "✓ CV backend: Tersimpan dan siap dikirim" else "⚠ CV backend: Belum diunggah",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (cvUploaded) Color(0xFF16A34A) else Color(0xFFD97706)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                            enabled = isOnline && !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, KarirOrange),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = KarirOrange
                            )
                        ) {
                            Text("Unggah CV PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = onNavigateProfil) {
                            Text("Profil & CV", color = KarirOrangeDeep, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (!gmailConnected) {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        val response = ApiClient.apiService.startGmailOAuth()
                                        if (!response.isSuccessful) {
                                            response.errorBody()?.close()
                                            error("OAuth belum dapat dimulai (${response.code()}).")
                                        }
                                        val url = JSONObject(response.body()?.use { it.string() } ?: "{}").getString("url")
                                        val uri = android.net.Uri.parse(url)
                                        require(uri.scheme == "https" && uri.host == "accounts.google.com" && uri.userInfo == null && uri.port in listOf(-1, 443)) { "URL otorisasi Google tidak valid." }
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        message = e.message ?: "OAuth gagal dimulai."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = isOnline && !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hubungkan Akun Gmail", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Controls Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Izin & Kontrol Auto Apply",
                        color = KarirTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Saya mengizinkan penggunaan CV dan profil untuk mengirim lamaran melalui Gmail, dengan batas maksimal 5 email per hari. Persetujuan berlaku sampai Anda menjeda Auto Apply.",
                        color = KarirTextMuted,
                        fontSize = 12.sp
                    )

                    if (!consentSaved) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = KarirOrange,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Saya menyetujui pengiriman otomatis",
                                color = KarirTextDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "✓ Persetujuan akun telah tersimpan secara resmi.",
                                color = Color(0xFF16A34A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (!gmailConnected) {
                        Text("• Akun Gmail perlu terhubung sebelum pengiriman dapat diaktifkan.", color = Color(0xFFD97706), fontSize = 11.5.sp)
                    }
                    if (!isPro) {
                        Text("• Auto Apply memerlukan paket langganan PRO aktif.", color = Color(0xFFD97706), fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val response = ApiClient.apiService.enableAutoApply(AutoApplyEnableRequest(consent = agreed || consentSaved))
                                    if (!response.isSuccessful) {
                                        response.errorBody()?.close()
                                        error("Aktivasi ditolak (${response.code()}). Periksa izin Gmail dan langganan.")
                                    }
                                    response.body()?.close()
                                    refresh()
                                    if (enabled) {
                                        accountId?.let { prefs.edit().putBoolean(it, true).apply() }
                                        consentSaved = true
                                        message = "Izin Auto Apply tersimpan. Pengiriman mengikuti konfigurasi operator."
                                    } else message = "Server belum mengaktifkan Auto Apply."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    message = e.message ?: "Aktivasi gagal. Muat ulang status sebelum mencoba lagi."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = statusLoaded && gmailConnected && cvUploaded && isPro && isOnline && !busy && !enabled && (agreed || consentSaved),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange)
                    ) {
                        Text(if (enabled) "Izin Auto Apply Aktif" else "Simpan Izin Auto Apply", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val response = ApiClient.apiService.pauseAutoApply()
                                    if (!response.isSuccessful) {
                                        response.errorBody()?.close()
                                        error("Gagal menjeda (${response.code()}).")
                                    }
                                    response.body()?.close()
                                    refresh()
                                    message = if (enabled) "Server belum menjeda pengiriman." else "Auto Apply dijeda."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    message = e.message ?: "Jeda gagal. Periksa status server."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = statusLoaded && enabled && !busy && isOnline,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = KarirTextDark
                        )
                    ) {
                        Text("Jeda Auto Apply", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Candidates List
        item {
            Text(
                "Kandidat Lamaran",
                color = KarirTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (candidates.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Belum ada kandidat lowongan yang dijadwalkan.",
                        color = KarirTextMuted,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(candidates.size) { index ->
                val c = candidates[index]
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${c.optString("company")} • ${c.optString("position")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = KarirTextDark
                        )
                        Text(
                            c.optString("email"),
                            fontSize = 12.sp,
                            color = KarirOrangeDeep
                        )
                        val isActive = c.optBoolean("vacancy_verified") && c.optBoolean("active")
                        Text(
                            if (isActive) "✓ Lowongan aktif terverifikasi" else "Kontak spontan terdata",
                            fontSize = 11.5.sp,
                            color = if (isActive) Color(0xFF16A34A) else KarirTextMuted
                        )
                    }
                }
            }
        }

        // Deliveries History
        item {
            Text(
                "Riwayat Pengiriman",
                color = KarirTextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        if (deliveries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Belum ada riwayat pengiriman dari server.",
                        color = KarirTextMuted,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(deliveries.size) { index ->
                val d = deliveries[index]
                val gmailId = d.optString("gmail_message_id").takeIf(AutoApplyConfirmation::hasReference) ?: d.optString("gmail_id")
                val confirmed = AutoApplyConfirmation.sent(d.optString("status"), gmailId)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC6DFE8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                d.optString("company"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = KarirTextDark
                            )
                            Text(
                                if (confirmed) "Terkirim via Gmail"
                                else if (d.optString("status") == "sent") "Menunggu konfirmasi Gmail"
                                else d.optString("status", "Dalam antrean"),
                                fontSize = 12.sp,
                                color = if (confirmed) Color(0xFF16A34A) else KarirTextMuted
                            )
                        }
                        Icon(
                            imageVector = if (confirmed) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (confirmed) Color(0xFF16A34A) else KarirOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
