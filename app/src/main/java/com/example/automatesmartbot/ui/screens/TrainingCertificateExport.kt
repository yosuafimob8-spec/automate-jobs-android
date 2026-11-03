package com.example.automatesmartbot.ui.screens

import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automatesmartbot.ui.theme.KarirOrange
import com.example.automatesmartbot.ui.theme.KarirTextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Landscape A4 certificate; participant data comes only from a completed course record. */
@Composable
fun TrainingCertificateExport(item: JSONObject) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val document = PdfDocument()
                    try {
                        val width = 1191
                        val height = 842
                        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
                        val canvas = page.canvas
                        canvas.scale(width / 842f, height / 595f)
                        TrainingCertificateArtwork.draw(canvas, item, context)
                        document.finishPage(page)
                        context.contentResolver.openOutputStream(uri, "wt")?.use { document.writeTo(it) }
                            ?: error("Gagal membuka lokasi penyimpanan")
                    } finally { document.close() }
                }
                status = "Sertifikat PDF berhasil disimpan."
            } catch (e: Exception) { status = "Gagal menyimpan sertifikat: ${e.message}" }
        }
    }
    Button(
        onClick = { launcher.launch("KarirMate-Sertifikat-Pelatihan.pdf") },
        colors = ButtonDefaults.buttonColors(containerColor = KarirOrange, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("⬇ Unduh Sertifikat (PDF)", fontWeight = FontWeight.SemiBold)
    }
    if (status.isNotBlank()) {
        Text(
            text = status,
            fontSize = 12.sp,
            color = KarirTextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
