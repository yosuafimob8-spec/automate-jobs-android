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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.autoapply.AutoApplyConfirmation
import com.example.automatesmartbot.data.autoapply.AutoApplyTargetPolicy
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.network.AutoApplyEnableRequest
import com.example.automatesmartbot.data.network.AutoApplyTargetRequest
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.screenCanvas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject


private fun targetEchoMatches(
    response: JSONObject?,
    positions: Set<String>,
    regions: Set<String>,
    flag: String
): Boolean {
    val payload = response ?: return false
    if (!payload.optBoolean(flag)) return false
    val receivedRoles = payload.optJSONArray("positions") ?: return false
    val receivedRegions = payload.optJSONArray("locations") ?: return false
    val roles = (0 until receivedRoles.length()).map { receivedRoles.optString(it) }.toSet()
    val cities = (0 until receivedRegions.length()).map { receivedRegions.optString(it) }.toSet()
    return roles == positions && cities == regions
}

@Composable
private fun AutoApplyTargetPicker(
    label: String,
    items: List<String>,
    selected: Set<String>,
    enabled: Boolean,
    onChange: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            color = com.example.automatesmartbot.ui.theme.ScreenText)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = ScreenSurface)
            ) {
                Text(
                    when (selected.size) {
                        0 -> "Pilih $label"
                        1 -> selected.first()
                        else -> "${selected.size} pilihan: ${selected.sorted().take(2).joinToString(", ")}${if (selected.size > 2) "…" else ""}"
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    fontSize = 12.sp,
                    color = com.example.automatesmartbot.ui.theme.ScreenText
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Buka pilihan $label")
            }
            DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
                items.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 13.sp) },
                        leadingIcon = {
                            Checkbox(checked = option in selected, onCheckedChange = null)
                        },
                        onClick = {
                            onChange(if (option in selected) selected - option else selected + option)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Selesai", fontWeight = FontWeight.Bold) },
                    onClick = { expanded = false }
                )
            }
        }
    }
}

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
    val targetPrefs = remember { context.getSharedPreferences("auto_apply_targets", 0) }
    var selectedPositions by remember(accountId) {
        mutableStateOf(targetPrefs.getStringSet("positions_${accountId.orEmpty()}", emptySet()).orEmpty().toSet())
    }
    var selectedRegions by remember(accountId) {
        mutableStateOf(targetPrefs.getStringSet("regions_${accountId.orEmpty()}", emptySet()).orEmpty().toSet())
    }
    var targetsConfirmed by remember { mutableStateOf(false) }

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
        targetsConfirmed = targetEchoMatches(a, selectedPositions, selectedRegions, "preferences_enforced")
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
            .background(com.example.automatesmartbot.ui.theme.ScreenPageGradient),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
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
                            border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8)))
                        ) {
                            Text(
                                "KARIRMATE • " + when (com.example.automatesmartbot.ui.components.LocalMembershipTier.current) {
                                    com.example.automatesmartbot.ui.components.MembershipTier.PRO -> "PRO"
                                    com.example.automatesmartbot.ui.components.MembershipTier.FREE -> "FREE"
                                    else -> "CEK"
                                },
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "Kirim Lamaran Otomatis",
                        color = com.example.automatesmartbot.ui.theme.ScreenText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Kelola CV, persetujuan, kandidat perusahaan, dan riwayat pengiriman resmi dalam satu alur terpusat.",
                        color = com.example.automatesmartbot.ui.theme.ScreenMuted,
                        fontSize = 13.sp
                    )
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFDCFCE7)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Security, null, tint = Color(0xFF079B35),
                                modifier = Modifier.size(19.dp))
                            Text("Maksimal 5 email per hari • Aman & Tanpa Duplikasi",
                                color = Color(0xFF07852F), fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }


        // User-selected job positions and regions. This does not silently enable sending.
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Work, contentDescription = null,
                            tint = Color(0xFF2563A6), modifier = Modifier.size(23.dp))
                        Text("Target Lamaran", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                            color = com.example.automatesmartbot.ui.theme.ScreenText)
                    }
                    Text("Pilih satu atau beberapa posisi dan wilayah tujuan. Hanya kandidat sesuai pilihan yang ditampilkan.",
                        fontSize = 12.sp,
                        color = com.example.automatesmartbot.ui.theme.ScreenMuted)
                    AutoApplyTargetPicker(
                        label = "Posisi Pekerjaan",
                        items = AutoApplyTargetPolicy.positions,
                        selected = selectedPositions,
                        enabled = accountId != null && !busy && !enabled,
                        onChange = { new ->
                            selectedPositions = new
                            targetsConfirmed = false
                            accountId?.let { id -> targetPrefs.edit().putStringSet("positions_$id", new).apply() }
                        }
                    )
                    AutoApplyTargetPicker(
                        label = "Wilayah Tujuan",
                        items = AutoApplyTargetPolicy.regions,
                        selected = selectedRegions,
                        enabled = accountId != null && !busy && !enabled,
                        onChange = { new ->
                            selectedRegions = new
                            targetsConfirmed = false
                            accountId?.let { id -> targetPrefs.edit().putStringSet("regions_$id", new).apply() }
                        }
                    )
                    if (selectedPositions.isNotEmpty() && selectedRegions.isNotEmpty()) {
                        Surface(color = screenCanvas(Color(0xFFE8F5F7)), shape = RoundedCornerShape(10.dp)) {
                            Text("${selectedPositions.size} posisi • ${selectedRegions.size} wilayah dipilih",
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                fontSize = 12.sp,
                                color = com.example.automatesmartbot.ui.theme.ScreenText,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        enabled = isOnline && statusLoaded && !busy && !enabled && accountId != null &&
                            selectedPositions.isNotEmpty() && selectedRegions.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val response = ApiClient.apiService.saveAutoApplyPreferences(
                                        AutoApplyTargetRequest(selectedPositions.sorted(), selectedRegions.sorted())
                                    )
                                    if (!response.isSuccessful) {
                                        response.errorBody()?.close()
                                        error("Backend belum mendukung penyimpanan target lamaran (HTTP ${response.code()}). Pengiriman tidak diaktifkan.")
                                    }
                                    val saved = JSONObject(response.body()?.use { it.string() } ?: "{}")
                                    if (!targetEchoMatches(saved, selectedPositions, selectedRegions, "applied_to_delivery")) {
                                        error("Backend belum mengonfirmasi bahwa target dipakai untuk pengiriman. Pengiriman tidak diaktifkan.")
                                    }
                                    refresh()
                                    if (!targetsConfirmed) {
                                        error("Status backend belum mengonfirmasi filter pengiriman. Pengiriman tidak diaktifkan.")
                                    }
                                    message = "Target posisi dan wilayah berhasil disimpan di backend."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    targetsConfirmed = false
                                    message = e.message ?: "Gagal menyimpan target."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563A6))
                    ) {
                        Text("Simpan Posisi & Wilayah", fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Place, contentDescription = null,
                            tint = if (targetsConfirmed) Color(0xFF16A34A) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp))
                        Text(
                            if (targetsConfirmed) "Filter pengiriman terkonfirmasi oleh backend."
                            else "Pilihan tersimpan di HP untuk pratinjau. Pengiriman menunggu dukungan dan konfirmasi backend.",
                            fontSize = 11.5.sp, lineHeight = 16.sp,
                            color = com.example.automatesmartbot.ui.theme.ScreenMuted
                        )
                    }
                    if (enabled) Text("Jeda Auto Apply terlebih dahulu untuk mengubah target.",
                        fontSize = 11.5.sp, color = Color(0xFFD97706))
                }
            }
        }

        // Status Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Layers, contentDescription = null,
                                tint = Color(0xFF086ABC), modifier = Modifier.size(25.dp))
                            Text("Status & Kesiapan", color = com.example.automatesmartbot.ui.theme.ScreenText,
                                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                                maxLines = 1)
                        }
                        TextButton(onClick = { scope.launch { load() } },
                            enabled = !busy && isOnline,
                            modifier = Modifier.background(screenCanvas(Color(0xFFE9F5FF)), RoundedCornerShape(13.dp))) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                tint = KarirOrangeDeep, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Muat Ulang", color = KarirOrangeDeep,
                                fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
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
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFFD8B2))) {
                            Row(Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null,
                                    tint = Color(0xFFE9900C), modifier = Modifier.size(21.dp))
                                Text(message, color = Color(0xFF9A3412),
                                    fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                    lineHeight = 17.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Gmail & Auto Apply Badge Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF5FAFC),
                        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
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
                                    color = com.example.automatesmartbot.ui.theme.ScreenText
                                )
                            }
                            Text(
                                if (operatorSendAllowed) "Izin operasional tersedia; jadwal pengiriman diverifikasi oleh sistem."
                                else "Pengiriman produksi memerlukan izin operasional; Anda tetap dapat menyiapkan konfigurasi.",
                                fontSize = 11.5.sp,
                                color = com.example.automatesmartbot.ui.theme.ScreenMuted
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
                                containerColor = ScreenSurface,
                                contentColor = KarirOrange
                            )
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Unggah CV PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = onNavigateProfil) {
                            Icon(Icons.Default.Person, contentDescription = null,
                                tint = KarirOrangeDeep, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Profil & CV", color = KarirOrangeDeep,
                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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
                colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null,
                            tint = Color(0xFF086ABC), modifier = Modifier.size(24.dp))
                        Text("Izin & Kontrol Auto Apply",
                            color = com.example.automatesmartbot.ui.theme.ScreenText,
                            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Text(
                        "Saya mengizinkan penggunaan CV dan profil untuk mengirim lamaran melalui Gmail, dengan batas maksimal 5 email per hari. Persetujuan berlaku sampai Anda menjeda Auto Apply.",
                        color = com.example.automatesmartbot.ui.theme.ScreenMuted,
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
                                color = com.example.automatesmartbot.ui.theme.ScreenText,
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
                                    val response = ApiClient.apiService.enableAutoApply(
                                        AutoApplyEnableRequest(
                                            consent = agreed || consentSaved,
                                            positions = selectedPositions.sorted(),
                                            locations = selectedRegions.sorted()
                                        )
                                    )
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
                        enabled = statusLoaded && gmailConnected && cvUploaded && isPro && isOnline && !busy && !enabled &&
                            targetsConfirmed && selectedPositions.isNotEmpty() && selectedRegions.isNotEmpty() &&
                            (agreed || consentSaved),
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
                            containerColor = ScreenSurface,
                            contentColor = KarirTextDark
                        )
                    ) {
                        Text("Jeda Auto Apply", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        val visibleCandidates = candidates.filter { c ->
            AutoApplyTargetPolicy.matchesCandidate(
                c.optString("position"), c.optString("location"), selectedPositions, selectedRegions
            )
        }

        // Candidates List
        item {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Groups, contentDescription = null,
                    tint = Color(0xFF086ABC), modifier = Modifier.size(23.dp))
                Text("Kandidat Lamaran", color = com.example.automatesmartbot.ui.theme.ScreenText,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }

        if (visibleCandidates.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (selectedPositions.isEmpty() || selectedRegions.isEmpty())
                            "Pilih posisi dan wilayah terlebih dahulu untuk melihat kandidat."
                        else "Belum ada kandidat yang sesuai posisi dan wilayah pilihan. Kandidat tanpa data lokasi tidak ditampilkan.",
                        color = com.example.automatesmartbot.ui.theme.ScreenMuted,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(visibleCandidates.size) { index ->
                val c = visibleCandidates[index]
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${c.optString("company")} • ${c.optString("position")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = com.example.automatesmartbot.ui.theme.ScreenText
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
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.History, contentDescription = null,
                    tint = Color(0xFF086ABC), modifier = Modifier.size(23.dp))
                Text("Riwayat Pengiriman", color = com.example.automatesmartbot.ui.theme.ScreenText,
                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }

        if (deliveries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Belum ada riwayat pengiriman dari server.",
                        color = com.example.automatesmartbot.ui.theme.ScreenMuted,
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
                    colors = CardDefaults.cardColors(containerColor = ScreenSurface),
                    border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6DFE8))),
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
                                color = com.example.automatesmartbot.ui.theme.ScreenText
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
