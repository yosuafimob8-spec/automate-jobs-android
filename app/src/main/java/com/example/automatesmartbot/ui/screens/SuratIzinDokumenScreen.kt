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
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirOrangeDeep
import com.example.automatesmartbot.ui.theme.KarirPageGradient
import com.example.automatesmartbot.ui.theme.KarirTextDark
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                val p = Paint(3).apply { color = android.graphics.Color.rgb(15, 23, 42); textSize = 18f; isFakeBoldText = true }
                c.drawText(type.uppercase(Locale.ROOT), 52f, 90f, p)
                p.isFakeBoldText = false
                p.textSize = 12f
                p.color = android.graphics.Color.DKGRAY
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
                    c.drawText(current, 52f, y, p)
                    y += 24f
                }
                p.color = android.graphics.Color.rgb(190, 55, 55)
                p.textSize = 14f
                p.isFakeBoldText = true
                c.drawText("DRAF - BUKAN SURAT KETERANGAN DOKTER", 90f, 788f, p)
                pdf.finishPage(pg)
                pdf.writeTo(output)
                pdf.close()
            }
            val history = JSONArray(prefs.getString("history", "[]"))
            history.put(JSONObject().put("title", "$type | $patient").put("uri", uri.toString()).put("date", date).put("official", false))
            prefs.edit().putString("last", "$type | $patient | $date").putString("history", history.toString()).apply()
            historyVersion++
            fileUri = uri
            notice = "PDF berhasil disimpan"
            page = 4
        } catch (e: Exception) { notice = "Gagal menyimpan PDF: ${e.message}" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarirPageGradient)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (page != 0) {
                TextButton(onClick = { page = when (page) { 1 -> 0; 2 -> 1; 3 -> 2; else -> 0 } }) {
                    Text("← Kembali", color = KarirOrangeDeep, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                when (page) {
                    0 -> "Surat Izin & Dokumen Kerja"
                    1 -> "Pilih Klinik"
                    2 -> "Data Surat"
                    3 -> "Preview Surat"
                    else -> "Dokumen Tersimpan"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KarirTextDark
            )

            if (notice.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(notice, color = Color(0xFF9A3412), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
            }

            when (page) {
                0 -> {
                    Text("Buat draf surat izin resmi dan kelola dokumen kerja dengan mudah.", color = KarirTextMuted, fontSize = 13.sp)
                    listOf("Surat Izin Sakit", "Surat Izin Cuti", "Surat Izin Keperluan Keluarga").forEach { title ->
                        SuratCard {
                            Text(title, fontSize = 16.sp, color = KarirTextDark, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { type = title; page = 1 },
                                colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Buat Surat Ini →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    SuratCard {
                        Text("Dokumen Klinik Resmi", fontSize = 16.sp, color = KarirTextDark, fontWeight = FontWeight.Bold)
                        Text("Simpan dan arsipkan surat keterangan dokter resmi yang diterbitkan klinik.", color = KarirTextMuted, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = { officialPicker.launch(arrayOf("application/pdf", "image/*")) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, KarirOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = KarirOrange)
                        ) {
                            Text("Unggah Surat Dokter Resmi", fontWeight = FontWeight.Bold)
                        }
                    }
                    SuratCard {
                        Text("Riwayat Dokumen", fontSize = 16.sp, color = KarirTextDark, fontWeight = FontWeight.Bold)
                        Text(prefs.getString("last", "Belum ada dokumen tersimpan") ?: "", color = KarirTextMuted, fontSize = 12.sp)
                        val records = remember(historyVersion) { JSONArray(prefs.getString("history", "[]")) }
                        if (records.length() == 0) {
                            Text("Belum ada dokumen yang dibuat.", color = KarirTextMuted, fontSize = 12.sp)
                        }
                        for (i in records.length() - 1 downTo 0) {
                            val record = records.optJSONObject(i) ?: continue
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${record.optString("title")} • ${record.optString("date")}",
                                    color = KarirTextDark,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    try {
                                        val uri = Uri.parse(record.optString("uri"))
                                        ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, if (record.optBoolean("official")) "*/*" else "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        })
                                    } catch (_: Exception) { notice = "Dokumen tidak dapat dibuka. Pastikan file masih tersedia." }
                                }) {
                                    Text("Buka", color = KarirOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("Riwayat tersimpan di perangkat lokal Anda.", color = KarirTextMuted, fontSize = 11.sp)
                    }
                }
                1 -> {
                    Text("Cari klinik melalui peta, lalu lengkapi informasi tempat berobat bila ada.", color = KarirTextMuted, fontSize = 12.5.sp)
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=klinik+terdekat"))
                            try { ctx.startActivity(intent) } catch (_: Exception) { notice = "Aplikasi peta tidak tersedia" }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔎 Cari Klinik di Google Maps", fontWeight = FontWeight.Bold)
                    }
                    SuratField("Nama Klinik (opsional)", clinic) { clinic = it }
                    SuratField("Alamat Klinik", address) { address = it }
                    SuratField("Nama Dokter (jika diketahui)", doctor) { doctor = it }
                    Text("Nama klinik dan dokter hanya sebagai informasi tempat berobat pribadi.", color = KarirTextMuted, fontSize = 11.sp)
                    SuratCard {
                        Text("Aset Resmi Klinik", color = KarirTextDark, fontWeight = FontWeight.Bold)
                        Text("Unggah cap dan tanda tangan yang diberikan langsung oleh klinik untuk arsip penerbit.", color = KarirTextMuted, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { stampPicker.launch(arrayOf("image/*")) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (clinicStamp == null) "Unggah Cap" else "Cap Dipilih ✓", color = KarirOrangeDeep, fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = { signaturePicker.launch(arrayOf("image/*")) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (doctorSignature == null) "Unggah TTD" else "TTD Dipilih ✓", color = KarirOrangeDeep, fontSize = 11.5.sp)
                            }
                        }
                    }
                    SuratNext("Lanjutkan ke Data Surat") { page = 2 }
                }
                2 -> {
                    SuratField("Nama Lengkap", patient) { patient = it }
                    SuratField("Ditujukan Kepada (HRD / Perusahaan)", destination) { destination = it }
                    SuratField("Tanggal Surat", date) { date = it }
                    SuratField("Jumlah Hari Izin", days) { days = it }
                    SuratField("Alasan Izin", reason) { reason = it }
                    SuratNext("Lihat Preview Surat") {
                        if (patient.isBlank()) notice = "Isi nama lengkap terlebih dahulu"
                        else { notice = ""; page = 3 }
                    }
                }
                3 -> {
                    SuratCard {
                        Text(type.uppercase(), fontSize = 17.sp, color = KarirTextDark, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Color(0xFFF1E5DC))
                        Text("Tanggal: $date", color = KarirTextDark, fontSize = 12.5.sp)
                        Text("Kepada Yth. ${destination.ifBlank { "HRD / Pimpinan" }}", color = KarirTextDark, fontSize = 12.5.sp)
                        Text("Dengan hormat, saya $patient mengajukan izin selama $days hari karena $reason.", color = KarirTextDark, fontSize = 12.5.sp)
                        if (clinic.isNotBlank()) Text("Tempat berobat: $clinic, $address", color = KarirTextDark, fontSize = 12.sp)
                        if (doctor.isNotBlank()) Text("Dokter (informasi): $doctor", color = KarirTextDark, fontSize = 12.sp)
                        Text("Hormat saya,\n$patient", color = KarirTextDark, fontSize = 12.5.sp)
                        Text("DRAF - BUKAN SURAT KETERANGAN DOKTER", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    SuratNext("Simpan Sebagai PDF") { export.launch("Surat_Izin_${System.currentTimeMillis()}.pdf") }
                    OutlinedButton(
                        onClick = { page = 2 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFDEC9))
                    ) {
                        Text("Edit Data Surat", color = KarirTextDark)
                    }
                }
                4 -> {
                    Text("PDF telah berhasil disimpan. Anda dapat membagikannya langsung.", color = KarirTextDark, fontSize = 13.sp)
                    Button(
                        onClick = {
                            fileUri?.let { uri ->
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                ctx.startActivity(Intent.createChooser(intent, "Bagikan surat"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bagikan PDF Sekarang", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { page = 0 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFDEC9))
                    ) {
                        Text("Kembali ke Menu Utama", color = KarirTextDark)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuratCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFFFDEC9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SuratField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = KarirTextDark,
            unfocusedTextColor = KarirTextDark,
            focusedBorderColor = KarirOrange,
            unfocusedBorderColor = Color(0xFFFFDEC9),
            focusedLabelColor = KarirOrange,
            cursorColor = KarirOrange
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SuratNext(label: String, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
