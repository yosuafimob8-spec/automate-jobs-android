package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.automatesmartbot.data.network.ApiClient
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.DarkBackground
import com.example.automatesmartbot.ui.theme.DarkSurface
import com.example.automatesmartbot.ui.theme.ProGold
import com.example.automatesmartbot.ui.theme.TextGray
import com.example.automatesmartbot.ui.theme.TextWhite

private val accent = Color(0xFFFF6428)

@Composable
fun KirimLamaranScreen(
    isPro: Boolean,
    isOnline: Boolean,
    onNavigateProfil: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Memuat status...") }
    var autoApplyEnabled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        statusText = try {
            val r = ApiClient.apiService.getAutoApplyStatus()
            if (r.isSuccessful) {
                val j = org.json.JSONObject(r.body()?.string() ?: "{}")
                val a = j.optJSONObject("auto_apply")
                autoApplyEnabled = a?.optBoolean("enabled") == true
                "Gmail: " + (if (j.optBoolean("gmail_connected")) "Terhubung" else "Belum terhubung") + " | Auto Apply: " + (if (a?.optBoolean("enabled") == true) "Aktif" else "Jeda") + " | Terkirim: " + (a?.optJSONObject("counts")?.optInt("sent", 0) ?: 0)
            } else "Status tidak tersedia (${r.code()})"
        } catch (e: Exception) { "Koneksi API gagal" }
    }
    var emailStyle by remember { mutableStateOf("AI Smart Match") }
    var agreed by remember { mutableStateOf(false) }
    val styles = listOf("Standar Profesional", "Menonjolkan Keahlian", "Menonjolkan Pengalaman", "AI Smart Match")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Kirim Lamaran Otomatis", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("AI mencari pekerjaan, mencocokkan CV, dan menyiapkan email lamaran.", color = Color(0xFF64748B), fontSize = 13.sp)
        }

        item {
            var refresh by remember { mutableIntStateOf(0) }
            var result by remember { mutableStateOf("Memuat kandidat...") }

            LaunchedEffect(refresh, isPro) {
                if (!isPro) {
                    result = "Khusus pengguna PRO"
                } else {
                    result = try {
                        val r = ApiClient.apiService.getKarirMateCandidates()
                        if (r.isSuccessful) {
                            val j = org.json.JSONObject(r.body()?.string() ?: "{}")
                            val arr = j.optJSONArray("items")
                            if (arr == null || arr.length() == 0) {
                                "Belum ada kandidat HR"
                            } else {
                                buildString {
                                    append("Total kandidat: ${arr.length()}\n\n")
                                    for (i in 0 until arr.length()) {
                                        val x = arr.getJSONObject(i)
                                        append(x.optString("company"))
                                        append("\n")
                                        append(x.optString("position"))
                                        append("\n")
                                        append(x.optString("email"))
                                        append("\n\n")
                                    }
                                }
                            }
                        } else "Gagal mengambil kandidat: HTTP ${r.code()}"
                    } catch (e: Exception) {
                        "Koneksi gagal: ${e.message}"
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Kandidat Email HR",
                        fontWeight = FontWeight.Bold)
                    Text(result)
                    Button(
                        onClick = { refresh++ },
                        enabled = isPro && isOnline
                    ) {
                        Text("Refresh Kandidat")
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("1. Pencarian Otomatis Seluruh Indonesia", fontWeight = FontWeight.Bold)
                    Text("Sistem memilih sendiri mode Sesuai CV atau Random. Tidak perlu mengatur posisi, kota, atau persentase kecocokan.")
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. CV & Profil", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text("CV dan informasi kontak akan digunakan untuk mencocokkan posisi dan menyusun lamaran. Integrasi pembacaan CV belum terhubung di layar ini.", color = Color(0xFF64748B), fontSize = 12.sp)
                    TextButton(onClick = onNavigateProfil) { Text("Buka Profil / CV", color = ProGold) }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("3. Gaya Email", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    styles.forEach { style ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = emailStyle == style, onClick = { emailStyle = style }, colors = RadioButtonDefaults.colors(selectedColor = accent))
                            Text(style, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFFD8E9F5))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("4. Aktivasi Pengiriman", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(if (isPro) "Akun PRO" else "Fitur otomatis khusus PRO", color = ProGold, fontWeight = FontWeight.SemiBold)
                    Text("Pengiriman memerlukan PRO aktif dan persetujuan. Status terkirim hanya setelah Gmail mengonfirmasi; hasil yang belum pasti tidak dikirim ulang otomatis.", color = Color(0xFF64748B), fontSize = 12.sp)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it }, colors = CheckboxDefaults.colors(checkedColor = accent))
                        Text("Saya menyetujui aturan pengiriman di atas", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    if (!isOnline) Text("Offline: sambungkan internet untuk menjalankan pencarian.", color = Color(0xFFF87171), fontSize = 12.sp)
                    Button(
                        onClick = { scope.launch {
                            isProcessing = true
                            message = try {
                                val r = ApiClient.apiService.enableAutoApply()
                                if (r.isSuccessful) {
                                    val status = ApiClient.apiService.getAutoApplyStatus()
                                    if (status.isSuccessful) {
                                        val j = org.json.JSONObject(status.body()?.string() ?: "{}")
                                        val a = j.optJSONObject("auto_apply")
                                        autoApplyEnabled = a?.optBoolean("enabled") == true
                                        statusText = "Gmail: " + (if (j.optBoolean("gmail_connected")) "Terhubung" else "Belum terhubung") + " | Auto Apply: " + (if (autoApplyEnabled) "Aktif" else "Jeda") + " | Terkirim: " + (a?.optJSONObject("counts")?.optInt("sent", 0) ?: 0)
                                        if (autoApplyEnabled) "Auto Apply berhasil diaktifkan" else "Auto Apply belum aktif"
                                    } else "Gagal memuat status: ${status.code()}"
                                } else "Aktivasi ditolak: ${r.code()}"
                            } catch (e: Exception) {
                                "Koneksi API gagal: ${e.message ?: "periksa jaringan"}"
                            } finally {
                                isProcessing = false
                            }
                        } },
                        enabled = isPro && isOnline && agreed && !autoApplyEnabled && !isProcessing,
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFD8E9F5), disabledContentColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(if (autoApplyEnabled) "Auto Apply Sudah Aktif" else if (isProcessing) "Memproses..." else "Aktifkan Auto Apply") }
                    Text(statusText, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { scope.launch { message = try { val r = ApiClient.apiService.pauseAutoApply(); if (r.isSuccessful) { autoApplyEnabled = false; statusText = "Auto Apply: Jeda"; "Auto Apply dijeda" } else "Gagal menjeda: ${r.code()}" } catch (e: Exception) { "Koneksi API gagal" } } }) { Text("Jeda Auto Apply") }
                    if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.onSurface)
                    if (!isPro) TextButton(onClick = onNavigateProfil) { Text("Lihat status PRO di Profil", color = ProGold) }
                }
            }
        }
        item {
            Text("Pencarian dan penentuan mode dilakukan backend. Jadwal pengiriman hanya aktif setelah verifikasi PRO dan izin pengguna.", color = Color(0xFF64748B), fontSize = 11.sp)
        }
    }
}
