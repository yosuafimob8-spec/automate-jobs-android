package com.example.automatesmartbot.ui.screens

import com.example.automatesmartbot.BuildConfig
import android.graphics.Paint
import android.graphics.Color
import com.example.automatesmartbot.data.career.TrainingCertificateIdentity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
    Button(onClick = { launcher.launch("KarirMate-Sertifikat-Pelatihan.pdf") }) {
        Text("⬇ Unduh Sertifikat (PDF)")
    }
    if (status.isNotBlank()) Text(status)
}
