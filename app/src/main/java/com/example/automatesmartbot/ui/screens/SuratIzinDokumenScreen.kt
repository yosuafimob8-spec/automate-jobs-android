package com.example.automatesmartbot.ui.screens

import android.content.Intent
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.RectF
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SuratNavy = Color(0xFF102C4D)
private val SuratOrange = Color(0xFFFF6B2B)
private val SuratBg = Color(0xFFF4F7FB)

@Composable
fun SuratIzinDokumenScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("karirmate_surat_izin", 0) }
    var page by remember { mutableStateOf(0) }
    var type by remember { mutableStateOf("Surat Izin Sakit") }
    var clinic by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var patient by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Sedang sakit dan membutuhkan istirahat") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())) }
    var days by remember { mutableStateOf("1") }
    var notice by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var clinicStamp by remember { mutableStateOf<Uri?>(null) }
    var doctorSignature by remember { mutableStateOf<Uri?>(null) }
    var officialLetter by remember { mutableStateOf<Uri?>(null) }
    var historyVersion by remember { mutableIntStateOf(0) }
    val stampPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        clinicStamp = uri
        uri?.let { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    val signaturePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        doctorSignature = uri
        uri?.let { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    val officialPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            officialLetter = uri
            ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val history = JSONArray(prefs.getString("history", "[]"))
            history.put(JSONObject().put("title", "Surat dokter asli (diunggah)").put("uri", uri.toString()).put("date", date).put("official", true))
            prefs.edit().putString("history", history.toString()).apply()
            historyVersion++
            notice = "Surat resmi tersimpan dari dokumen asli"
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) try {
            ctx.contentResolver.openOutputStream(uri)?.use { output ->
                val pdf = PdfDocument()
                val info = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val pg = pdf.startPage(info)
                val c = pg.canvas
                val p = Paint(3).apply { color = android.graphics.Color.rgb(16,44,77); textSize = 18f; isFakeBoldText = true }
                c.drawText(type.uppercase(Locale.ROOT), 52f, 90f, p)
                p.isFakeBoldText = false; p.textSize = 12f; p.color = android.graphics.Color.DKGRAY
                val lines = listOf(
                    "Tanggal: $date", "Kepada: ${destination.ifBlank { "HRD / Pimpinan" }}", "", "Dengan hormat,",
                    "Saya yang bertanda tangan di bawah ini:", "Nama: $patient", "", "Dengan ini mengajukan izin karena:", reason,
                    "Lama izin: $days hari", "", "Informasi tempat berobat (jika ada):", "Klinik: ${clinic.ifBlank { "-" }}",
                    "Alamat: ${address.ifBlank { "-" }}", "Dokter (informasi): ${doctor.ifBlank { "-" }}", "",
                    "Surat ini merupakan pemberitahuan pribadi, bukan surat dokter.", "", "Hormat saya,", patient
                )
                var y = 132f
                for (line in lines) {
                    val words = line.split(" ")
                    var current = ""
                    for (word in words) {
                        val test = if (current.isEmpty()) word else "$current $word"
                        if (p.measureText(test) > 480f) { c.drawText(current, 52f, y, p); y += 22f; current = word }
                        else current = test
                    }
                    c.drawText(current, 52f, y, p); y += 24f
                }
                p.color = android.graphics.Color.rgb(190, 55, 55); p.textSize = 14f; p.isFakeBoldText = true
                c.drawText("DRAF - BUKAN SURAT KETERANGAN DOKTER", 90f, 788f, p)
                // Stempel dan TTD tidak dicetak pada surat izin pribadi.
                // Aset resmi hanya boleh digunakan oleh penerbit klinik yang berwenang.
                pdf.finishPage(pg); pdf.writeTo(output); pdf.close()
            }
            val history = JSONArray(prefs.getString("history", "[]"))
            history.put(JSONObject().put("title", "$type | $patient").put("uri", uri.toString()).put("date", date).put("official", false))
            prefs.edit().putString("last", "$type | $patient | $date").putString("history", history.toString()).apply()
            historyVersion++
            fileUri = uri; notice = "PDF berhasil disimpan"; page = 4
        } catch (e: Exception) { notice = "Gagal menyimpan PDF: ${e.message}" }
    }
    Column(Modifier.fillMaxSize().background(SuratBg).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (page != 0) TextButton(onClick = { page = when(page) { 1 -> 0; 2 -> 1; 3 -> 2; else -> 0 } }) { Text("← Kembali", color = SuratNavy) }
        Text(when(page) { 0 -> "Surat Izin & Dokumen Kerja"; 1 -> "Pilih Klinik"; 2 -> "Data Surat"; 3 -> "Preview Surat"; else -> "Dokumen Tersimpan" }, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = SuratNavy)
        if (notice.isNotEmpty()) Text(notice, color = SuratNavy)
        when(page) {
            0 -> {
                Text("Buat surat izin dan kelola dokumen kerja dengan mudah.", color = SuratNavy)
                listOf("Surat Izin Sakit", "Surat Izin Cuti", "Surat Izin Keperluan Keluarga").forEach { title ->
                    SuratCard {
                        Text(title, fontSize = 19.sp, color = SuratNavy, fontWeight = FontWeight.Bold)
                        Button(onClick = { type = title; page = 1 }, colors = ButtonDefaults.buttonColors(containerColor = SuratOrange)) { Text("Buat Surat →") }
                    }
                }
                SuratCard {
                    Text("Dokumen Klinik", fontSize = 19.sp, color = SuratNavy, fontWeight = FontWeight.Bold)
                    Text("Simpan surat keterangan dokter asli yang diterbitkan klinik.", color = SuratNavy)
                    OutlinedButton(onClick = { officialPicker.launch(arrayOf("application/pdf", "image/*")) }) {
                        Text("Unggah Surat Dokter Resmi", color = SuratNavy)
                    }
                }
                SuratCard {
                    Text("Riwayat Dokumen", fontSize = 19.sp, color = SuratNavy, fontWeight = FontWeight.Bold)
                    Text(prefs.getString("last", "Belum ada dokumen tersimpan") ?: "", color = SuratNavy)
                    val records = remember(historyVersion) { JSONArray(prefs.getString("history", "[]")) }
                    if (records.length() == 0) Text("Belum ada dokumen", color = SuratNavy)
                    for (i in records.length() - 1 downTo 0) {
                        val record = records.optJSONObject(i) ?: continue
                        Text("${record.optString("title")} • ${record.optString("date")}", color = SuratNavy)
                        TextButton(onClick = {
                            try {
                                val uri = Uri.parse(record.optString("uri"))
                                ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, if (record.optBoolean("official")) "*/*" else "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                            } catch (_: Exception) { notice = "Dokumen tidak dapat dibuka. Pastikan file masih tersedia." }
                        }) { Text("Buka Dokumen", color = SuratOrange) }
                    }
                    Text("Riwayat tersimpan di perangkat. File asli harus tetap tersedia.", color = Color.Gray, fontSize = 12.sp)
                }
            }
            1 -> {
                Text("Cari klinik melalui peta, lalu isi informasi klinik sesuai hasil pencarian.", color = SuratNavy)
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=klinik+terdekat"))
                    try { ctx.startActivity(intent) } catch (_: Exception) { notice = "Aplikasi peta tidak tersedia" }
                }, colors = ButtonDefaults.buttonColors(containerColor = SuratOrange), modifier = Modifier.fillMaxWidth()) { Text("🔎 Cari Klinik di Peta") }
                SuratField("Nama Klinik (opsional)", clinic) { clinic = it }
                SuratField("Alamat Klinik", address) { address = it }
                SuratField("Nama Dokter (jika diketahui)", doctor) { doctor = it }
                Text("Nama klinik dan dokter hanya informasi tempat berobat, bukan pengesahan surat.", color = Color.Gray, fontSize = 12.sp)
                SuratCard {
                    Text("Aset Resmi Klinik", color = SuratNavy, fontWeight = FontWeight.Bold)
                    Text("Unggah hanya cap dan tanda tangan yang diberikan langsung oleh klinik. Aset disimpan untuk integrasi penerbit klinik, bukan untuk mengesahkan surat pribadi.", color = SuratNavy, fontSize = 12.sp)
                    OutlinedButton(onClick = { stampPicker.launch(arrayOf("image/*")) }) {
                        Text(if (clinicStamp == null) "Unggah Cap Klinik" else "Cap Klinik Dipilih ✓", color = SuratNavy)
                    }
                    OutlinedButton(onClick = { signaturePicker.launch(arrayOf("image/*")) }) {
                        Text(if (doctorSignature == null) "Unggah TTD Dokter" else "TTD Dokter Dipilih ✓", color = SuratNavy)
                    }
                }
                SuratNext("Lanjutkan") { page = 2 }
            }
            2 -> {
                SuratField("Nama Lengkap", patient) { patient = it }
                SuratField("Ditujukan Kepada (HRD/Perusahaan)", destination) { destination = it }
                SuratField("Tanggal Surat", date) { date = it }
                SuratField("Jumlah Hari Izin", days) { days = it }
                SuratField("Alasan Izin", reason) { reason = it }
                SuratNext("Lihat Preview") { if (patient.isBlank()) notice = "Isi nama lengkap terlebih dahulu" else { notice = ""; page = 3 } }
            }
            3 -> {
                SuratCard {
                    Text(type.uppercase(), fontSize = 19.sp, color = SuratNavy, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text("Tanggal: $date", color = SuratNavy)
                    Text("Kepada Yth. ${destination.ifBlank { "HRD / Pimpinan" }}", color = SuratNavy)
                    Text("Dengan hormat, saya $patient mengajukan izin selama $days hari karena $reason.", color = SuratNavy)
                    if (clinic.isNotBlank()) Text("Tempat berobat: $clinic, $address", color = SuratNavy)
                    if (doctor.isNotBlank()) Text("Dokter (informasi): $doctor", color = SuratNavy)
                    Text("Hormat saya,\n$patient", color = SuratNavy)
                    Text("DRAF - BUKAN SURAT KETERANGAN DOKTER", color = Color(0xFFB33B3B), fontWeight = FontWeight.Bold)
                }
                SuratNext("Simpan PDF") { export.launch("Surat_Izin_${System.currentTimeMillis()}.pdf") }
                OutlinedButton(onClick = { page = 2 }, modifier = Modifier.fillMaxWidth()) { Text("Edit Surat") }
            }
            4 -> {
                Text("PDF telah disimpan. Kamu bisa membagikannya melalui aplikasi lain.", color = SuratNavy)
                Button(onClick = {
                    fileUri?.let { uri ->
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                        ctx.startActivity(Intent.createChooser(intent, "Bagikan surat"))
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = SuratOrange)) { Text("Bagikan PDF") }
                OutlinedButton(onClick = { page = 0 }) { Text("Kembali ke Menu") }
            }
        }
    }
}

@Composable
private fun SuratCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SuratField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SuratNavy, unfocusedTextColor = SuratNavy, focusedBorderColor = SuratOrange),
        shape = RoundedCornerShape(12.dp))
}

@Composable
private fun SuratNext(label: String, action: () -> Unit) {
    Button(onClick = action, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = SuratOrange), shape = RoundedCornerShape(14.dp)) { Text(label) }
}
