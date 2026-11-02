package com.example.automatesmartbot.ui.screens

import android.graphics.Paint
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
                        val width = 842
                        val height = 595
                        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
                        val canvas = page.canvas
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        fun text(value: String, y: Float, size: Float, color: Int = android.graphics.Color.BLACK, bold: Boolean = false) {
                            paint.color = color
                            paint.textSize = size
                            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                            paint.style = Paint.Style.FILL
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(value, width / 2f, y, paint)
                        }
                        val orange = android.graphics.Color.rgb(255, 106, 15)
                        val gold = android.graphics.Color.rgb(207, 149, 35)
                        val dark = android.graphics.Color.rgb(25, 46, 65)
                        canvas.drawColor(android.graphics.Color.rgb(255, 251, 245))
                        paint.color = gold
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 5f
                        canvas.drawRoundRect(22f, 22f, 820f, 573f, 14f, 14f, paint)
                        paint.strokeWidth = 1.5f
                        canvas.drawRoundRect(33f, 33f, 809f, 562f, 10f, 10f, paint)
                        paint.style = Paint.Style.FILL
                        text("KarirMate", 93f, 30f, orange, true)
                        text("SERTIFIKAT", 154f, 46f, dark, true)
                        text("PENYELESAIAN PELATIHAN", 184f, 19f, dark, true)
                        text("Diberikan kepada", 230f, 16f)
                        text(item.optString("name").take(44), 268f, 30f, dark, true)
                        paint.color = gold
                        paint.strokeWidth = 1.5f
                        canvas.drawLine(230f, 280f, 612f, 280f, paint)
                        text("Telah menyelesaikan pelatihan", 313f, 16f)
                        text(item.optString("course").take(55), 344f, 23f, dark, true)
                        text("Dengan hasil akhir ${item.optInt("score")}/100", 373f, 17f)
                        text("Training Manager", 433f, 14f, dark, true)
                        text("Tanti kurnia", 457f, 16f, dark, true)
                        text("Program Director", 493f, 14f, dark, true)
                        text("Ilham Ramdhan", 516f, 16f, dark, true)
                        text("Tanggal: ${item.optString("date")}  •  ID: ${item.optString("id").take(16)}", 538f, 10f)
                        text("Sertifikat pelatihan internal KarirMate, bukan sertifikasi kompetensi resmi BNSP.", 554f, 9f)
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
