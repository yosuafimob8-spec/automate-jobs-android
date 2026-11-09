package com.example.automatesmartbot.ui.screens

import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.data.career.TrainingCertificateIdentity
import com.example.automatesmartbot.data.network.ApiClient
import com.example.automatesmartbot.data.psychotest.PsychotestPolicy
import com.example.automatesmartbot.ui.theme.ScreenSurface
import com.example.automatesmartbot.ui.theme.ScreenText
import com.example.automatesmartbot.ui.theme.ScreenMuted
import com.example.automatesmartbot.ui.theme.karirInputColors
import com.example.automatesmartbot.ui.theme.screenCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


/** Read-only result for the signed-in account. The server is authoritative for history.
 * A psychotest score is not a certificate or proof that any pass threshold was met.
 */
private val certBlue = Color(0xFF0864B5)
private val certNavy = Color(0xFF10254A)
private val certGold = Color(0xFFF7B52B)

/** Update 5: BODY ONLY. The app shell and all header composables are untouched. */
@Composable
internal fun CertificateHistoryBody(
    history: JSONArray,
    testMode: Boolean = false,
    scores: CertificateScoreReadiness,
    onShowCertificate: (JSONObject) -> Unit,
    onBackToCatalog: () -> Unit
) {
    val all = remember(history.toString()) { (0 until history.length()).mapNotNull { history.optJSONObject(it) } }
    val latest = all.lastOrNull()
    val ink = ScreenText
    val muted = ScreenMuted
    var qrInput by rememberSaveable { mutableStateOf("") }
    var qrFeedback by rememberSaveable { mutableStateOf("") }
    val surface = ScreenSurface

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Riwayat Sertifikat", fontWeight = FontWeight.ExtraBold, fontSize = 27.sp,
                lineHeight = 31.sp, color = ink)
            Text("Kumpulan sertifikat pelatihan yang Anda peroleh di KarirMate.",
                fontSize = 13.sp, lineHeight = 19.sp, color = muted)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(90.dp).clip(RoundedCornerShape(23.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFD8ECFF), Color(0xFFEDF8FF)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CardMembership, null, Modifier.size(65.dp), tint = certBlue)
            Icon(Icons.Default.Stars, null, Modifier.align(Alignment.BottomEnd).size(31.dp), tint = certGold)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFA5D0F5), Color(0xFF3C668A))),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null,
                    tint = certBlue, modifier = Modifier.size(31.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Syarat Mendapatkan Sertifikat", color = ink,
                        fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text("Selesaikan tahapan berikut sesuai ketentuan program sertifikat terkait.",
                        fontSize = 12.sp, color = muted, lineHeight = 16.sp)
                }
                com.example.automatesmartbot.ui.components.MembershipBadge(style = com.example.automatesmartbot.ui.components.MembershipBadgeStyle.LIGHT)
            }
            val psychotestSummary = if (testMode)
                "Selesai 100% (mode uji)"
            else scores.psychStatus
            CertificateRequirementRow(
                icon = { Icon(Icons.Default.AssignmentTurnedIn, null, tint = Color(0xFF168E9D), modifier = Modifier.size(29.dp)) },
                title = "Tes Gabungan", detail = "Hanya nilai Tes Gabungan yang dihitung",
                status = psychotestSummary,
                achieved = testMode || scores.psychPassed
            )
            CertificateRequirementRow(
                icon = { Icon(Icons.Default.MenuBook, null, tint = certBlue, modifier = Modifier.size(30.dp)) },
                title = "Lolos Pelatihan Kerja", detail = "Level Basic sampai Advance",
                status = when { testMode -> "Selesai 100% (mode uji)"; latest == null -> "Belum ada"; latest.optBoolean("serverRegistered") -> "Terverifikasi saat diterbitkan"; else -> "Tercatat lokal (belum diverifikasi online)" }, achieved = testMode || latest != null
            )
            CertificateRequirementRow(
                icon = { Icon(Icons.Default.SmartToy, null, tint = certBlue, modifier = Modifier.size(30.dp)) },
                title = "Interview Suara", detail = "Hanya hasil Interview Suara yang dihitung",
                status = if (testMode) "Selesai 100% (mode uji)"
                     else scores.voiceStatus,
                achieved = testMode || scores.voicePassed
            )
            Text(if (testMode)
                    "Mode pengujian akun khusus: semua persyaratan disimulasikan selesai. Sertifikat simulasi bukan sertifikasi resmi."
                else
                    "Hanya Tes Gabungan dan Interview Suara yang dihitung. Skor Interview disimpan lokal per akun dan belum diverifikasi server. Sertifikat lokal bukan sertifikasi resmi BNSP.",
                fontSize = 10.sp, lineHeight = 15.sp, color = muted)
        }
    }

    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            shape = RoundedCornerShape(19.dp),
            border = BorderStroke(1.dp, screenCanvas(Color(0xFF88BCEB), Color(0xFF3C668A))),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            if (latest == null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 27.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CardMembership, null, tint = certBlue, modifier = Modifier.size(58.dp))
                    Text("Belum ada sertifikat", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Selesaikan pelatihan dan evaluasinya. Sertifikat asli akan tampil di sini setelah diterbitkan.",
                        fontSize = 12.sp, color = muted, lineHeight = 18.sp, textAlign = TextAlign.Center)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 34.dp, bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth >= 355.dp) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CertificateThumb(latest, Modifier.width(145.dp).height(116.dp))
                                CertificateInfo(latest, Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CertificateThumb(latest, Modifier.fillMaxWidth().height(160.dp))
                                CertificateInfo(latest)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { onShowCertificate(latest) },
                            modifier = Modifier.weight(1.15f),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = certBlue),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Lihat Sertifikat", fontSize = 11.sp, maxLines = 1)
                        }
                        CertificateDownloadButton(latest, Modifier.weight(1f))
                        CertificateShareButton(latest, Modifier.weight(.8f))
                    }
                }
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp),
            shape = RoundedCornerShape(10.dp), color = certBlue,
            shadowElevation = 3.dp
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = certGold, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sertifikat Terbaru", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC8DDED), Color(0xFF365D7A))),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(49.dp).clip(RoundedCornerShape(11.dp))
                    .background(screenCanvas(Color(0xFFE5F2FF), Color(0xFF223F56))),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QrCode2, null, tint = certBlue, modifier = Modifier.size(30.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verifikasi Sertifikat", color = ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Masukkan isi QR sertifikat untuk mencocokkan identitasnya dengan catatan lokal.",
                        color = muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = certBlue)
            }
            HorizontalDivider(color = screenCanvas(Color(0xFFDFEAF3), Color(0xFF3E5871)))
            OutlinedTextField(
                value = qrInput,
                onValueChange = { qrInput = it; qrFeedback = "" },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Isi QR sertifikat") },
                leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, tint = certBlue) },
                shape = RoundedCornerShape(10.dp), colors = karirInputColors()
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val registered = all.mapNotNull { record ->
                        runCatching {
                            TrainingCertificateIdentity.payload(record.optString("id"), record.optString("courseId"),
                                record.optString("date"), record.optInt("score"))
                        }.getOrNull()
                    }
                    qrFeedback = when {
                        qrInput.isBlank() -> "Masukkan isi kode QR sertifikat terlebih dahulu."
                        TrainingCertificateIdentity.verifyLocal(qrInput.trim(), registered) ->
                            "Cocok dengan catatan lokal perangkat ini. Ini bukan verifikasi online."
                        else -> "Kode QR tidak cocok dengan catatan sertifikat lokal pada perangkat ini."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = certBlue),
                shape = RoundedCornerShape(11.dp)
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Periksa Sertifikat", fontWeight = FontWeight.Bold)
            }
            if (qrFeedback.isNotEmpty()) Text(qrFeedback, color = ink, fontSize = 12.sp)
        }
    }

    if (all.size > 1) {
        Text("Sertifikat Lainnya", fontWeight = FontWeight.ExtraBold, color = ink, fontSize = 18.sp)
        all.dropLast(1).asReversed().forEach { item ->
            OutlinedCard(onClick = { onShowCertificate(item) }, modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = surface),
                border = BorderStroke(1.dp, screenCanvas(Color(0xFFBED6EA), Color(0xFF3C668A)))) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.CardMembership, null, tint = certBlue)
                    Column(Modifier.weight(1f)) {
                        Text(item.optString("course"), color = ink, fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text("${item.optString("name")} • ${item.optString("date")}", color = muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = certBlue)
                }
            }
        }
    }

    OutlinedButton(
        onClick = onBackToCatalog, modifier = Modifier.fillMaxWidth(.70f).heightIn(min = 48.dp),
        border = BorderStroke(1.dp, certBlue),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = screenCanvas(certNavy, Color.White))
    ) {
        Icon(Icons.Default.MenuBook, null, Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Text("Kembali ke Katalog", fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CertificateRequirementRow(
    icon: @Composable () -> Unit,
    title: String,
    detail: String,
    status: String,
    achieved: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = screenCanvas(Color(0xFFF0F7FD), Color(0xFF183044)),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC3D8EB), Color(0xFF345B75)))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) { icon() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                    color = ScreenText, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(detail, fontSize = 11.sp, lineHeight = 16.sp, color = ScreenMuted)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (achieved) screenCanvas(Color(0xFFCFF4DA), Color(0xFF175539))
                            else screenCanvas(Color(0xFFE7EEF5), Color(0xFF33475B))
                ) {
                    Text(
                        (if (achieved) "✓ " else "• ") + status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = if (achieved) screenCanvas(Color(0xFF086529), Color(0xFFBEF4D4)) else ScreenMuted,
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 15.sp,
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateThumb(item: JSONObject, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier.clip(RoundedCornerShape(13.dp))
        .background(screenCanvas(Color(0xFFE5F2FB), Color(0xFF24445C)))) {
        Canvas(Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            canvas.save()
            try {
                canvas.scale(size.width / 842f, size.height / 595f)
                TrainingCertificateArtwork.draw(canvas, item, context)
            } finally { canvas.restore() }
        }
    }
}

@Composable
private fun CertificateInfo(item: JSONObject, modifier: Modifier = Modifier) {
    val ink = ScreenText
    val muted = ScreenMuted
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Sertifikat Pelatihan KarirMate", color = ink, fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold, lineHeight = 21.sp)
        Surface(shape = RoundedCornerShape(50),
            color = screenCanvas(Color(0xFFE4F4FB), Color(0xFF254C69))) {
            Text(if (item.optBoolean("serverRegistered")) "Terdaftar server" else "Identitas lokal",
                color = screenCanvas(Color(0xFF0964A7), Color(0xFFB5E0FF)), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        CertificateInfoRow("Nama", item.optString("name", "-"))
        CertificateInfoRow("Tanggal Terbit", formatCertificateDate(item.optString("date")))
        CertificateInfoRow("Nomor Sertifikat", item.optString("id", "-"))
    }
}

@Composable
private fun CertificateInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, color = ScreenMuted, fontSize = 10.sp)
        Text(value.ifBlank { "-" }, color = ScreenText, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatCertificateDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID")))
}.getOrDefault(value.ifBlank { "-" })

@Composable
private fun CertificateDownloadButton(item: JSONObject, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            feedback = runCatching {
                withContext(Dispatchers.IO) {
                    val pdf = PdfDocument()
                    try {
                        val page = pdf.startPage(PdfDocument.PageInfo.Builder(1191, 842, 1).create())
                        val canvas = page.canvas
                        canvas.scale(1191f / 842f, 842f / 595f)
                        TrainingCertificateArtwork.draw(canvas, item, context)
                        pdf.finishPage(page)
                        context.contentResolver.openOutputStream(uri, "wt")?.use { pdf.writeTo(it) }
                            ?: error("Gagal menyimpan PDF")
                    } finally { pdf.close() }
                }
                "PDF disimpan."
            }.getOrElse { "Gagal menyimpan PDF: ${it.message}" }
        }
    }
    Column(modifier) {
        OutlinedButton(onClick = { launcher.launch("KarirMate-Sertifikat-${item.optString("id").take(8)}.pdf") },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
            border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6D8EE), Color(0xFF3C668A))),
            shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.FileDownload, null, Modifier.size(15.dp), tint = certBlue)
            Spacer(Modifier.width(3.dp))
            Text("Unduh PDF", color = ScreenText, fontSize = 11.sp, maxLines = 1)
        }
        if (feedback.isNotBlank()) Text(feedback, color = ScreenMuted, fontSize = 9.sp)
    }
}

@Composable
private fun CertificateShareButton(item: JSONObject, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val body = """KarirMate • Sertifikat Penyelesaian Pelatihan
Nama: ${item.optString("name")}
Pelatihan: ${item.optString("course")}
Nomor: ${item.optString("id")}
Tanggal: ${item.optString("date")}
Status: ${if (item.optBoolean("serverRegistered")) "Tercatat pada server saat penerbitan" else "Tercatat lokal, belum diverifikasi online"}

Bukti pelatihan internal, bukan sertifikasi kompetensi resmi BNSP.""".trimIndent()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan informasi sertifikat"))
    }, modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        border = BorderStroke(1.dp, screenCanvas(Color(0xFFC6D8EE), Color(0xFF3C668A))),
        shape = RoundedCornerShape(12.dp)) {
        Icon(Icons.Default.Share, null, Modifier.size(14.dp), tint = certBlue)
        Spacer(Modifier.width(3.dp))
        Text("Bagikan", color = ScreenText, fontSize = 11.sp, maxLines = 1)
    }
}
