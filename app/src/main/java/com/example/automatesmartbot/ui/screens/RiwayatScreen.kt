package com.example.automatesmartbot.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val historyBlue = Color(0xFF0867BA)
private val historyInk = Color(0xFF101A35)
private val historyLine = Color(0xFFD1E3EF)

/** UI-only redesign. The checked base does NOT have a genuine application-history
 * repository. Never synthesize applications, delivery receipts or success status.
 * onReload can be supplied when a verified data source is integrated later.
 */
@Composable
fun RiwayatScreen(onReload: (() -> Unit)? = null) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val ink = if (dark) Color(0xFFFAFDFF) else historyInk
    val muted = if (dark) Color(0xFFC1D4E6) else Color(0xFF3E5475)
    val panel = if (dark) Color(0xFF192D41) else Color(0xFFFDFEFF)
    val line = if (dark) Color(0xFF355777) else historyLine
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                if (dark) listOf(Color(0xFF0D2032), Color(0xFF152D43), Color(0xFF0D2032))
                else listOf(Color(0xFFECF7FF), Color(0xFFF5FAFF), Color(0xFFEAF5FE))
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // Hero: exact wording and information hierarchy from the design reference.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = if (dark) panel else Color(0xFFF1F8FD),
                border = BorderStroke(1.dp, line),
                shadowElevation = 1.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = historyBlue,
                            modifier = Modifier.size(63.dp))
                        Box(
                            modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)
                                .clip(RoundedCornerShape(9.dp)).background(Color(0xFF0869B7))
                                .border(2.dp, if (dark) panel else Color(0xFFF1F8FD), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White,
                                modifier = Modifier.size(19.dp))
                        }
                    }
                    Box(Modifier.width(1.dp).height(67.dp).background(line))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Riwayat Lamaran", color = ink, fontSize = 18.sp, lineHeight = 22.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Pantau pengiriman yang benar-benar tercatat, tanpa status palsu.",
                            color = ink, fontSize = 12.2.sp, lineHeight = 17.sp)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth(),
                color = panel, border = BorderStroke(1.dp, line), shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFF24A8E9), modifier = Modifier.size(21.dp))
                        Text("Kembangkan kariermu selangkah demi selangkah",
                            color = historyBlue, fontSize = 11.sp, lineHeight = 14.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Box(Modifier.width(1.dp).height(23.dp).background(line))
                        TextButton(onClick = { showInfo = !showInfo }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Text(if (showInfo) "Tutup" else "Info", color = historyBlue,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (showInfo) {
                        HorizontalDivider(color = line)
                        Text(
                            "Riwayat menampilkan pengiriman lamaran yang tercatat dari sumber tepercaya. " +
                                "Status penerimaan dan proses seleksi tidak akan dibuat tanpa konfirmasi.",
                            color = muted, fontSize = 12.sp, lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
                color = panel, border = BorderStroke(1.dp, line), shadowElevation = 2.dp
            ) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, tint = historyBlue,
                            modifier = Modifier.size(28.dp))
                        Text("Status Pengiriman", modifier = Modifier.weight(1f), color = ink,
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Surface(
                            onClick = {
                                if (onReload != null) onReload()
                                else scope.launch {
                                    host.showSnackbar("Sumber riwayat lamaran belum terhubung. Tidak ada status yang dibuat.")
                                }
                            },
                            color = if (dark) Color(0xFF254764) else Color(0xFFE9F4FF),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = historyBlue,
                                    modifier = Modifier.size(17.dp))
                                Text("Muat Ulang", color = if (dark) Color(0xFF88CBFF) else historyBlue,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                    Text("Pantau status lamaran kerja yang sudah Anda ajukan atau kirimkan.",
                        color = ink, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }

            // Deliberate empty state: no fabricated applications while history API is absent.
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryEmptyArtwork()
                    Text("Belum ada riwayat lamaran", color = ink, fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text("Riwayat resmi akan otomatis muncul setelah pengiriman email atau lamaran diproses.",
                        color = muted, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 31.dp))
                }
            }
        }
        SnackbarHost(hostState = host, modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp))
    }
}

@Composable
private fun HistoryEmptyArtwork() {
    Box(Modifier.size(width = 140.dp, height = 120.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(width = 85.dp, height = 101.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFD9ECFF), Color(0xFFBED9F5))))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val blue = Color(0xFF4398F4)
                for (i in 0..2) {
                    val y = size.height * (.27f + i * .23f)
                    drawCircle(blue, radius = size.width * .035f,
                        center = androidx.compose.ui.geometry.Offset(size.width * .19f, y))
                    drawLine(blue,
                        start = androidx.compose.ui.geometry.Offset(size.width * .32f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width * (if (i == 2) .71f else .87f), y),
                        strokeWidth = size.height * .045f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 2.dp)
            .size(55.dp).clip(RoundedCornerShape(50))
            .background(Color(0xFFF0F8FF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.History, contentDescription = null,
                tint = Color(0xFF0874CE), modifier = Modifier.size(53.dp))
        }
        Canvas(Modifier.align(Alignment.TopEnd).size(32.dp)) {
            val blue = Color(0xFF4196FA)
            drawLine(blue,
                androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .15f),
                androidx.compose.ui.geometry.Offset(size.width * .15f, size.height * .47f),
                strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(blue,
                androidx.compose.ui.geometry.Offset(size.width * .60f, size.height * .20f),
                androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .39f),
                strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}
