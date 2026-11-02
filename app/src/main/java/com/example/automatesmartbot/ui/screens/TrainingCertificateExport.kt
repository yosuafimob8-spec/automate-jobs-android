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
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                        fun text(value: String, y: Float, size: Float, color: Int = android.graphics.Color.BLACK, bold: Boolean = false) {
                            paint.color = color
                            paint.textSize = size
                            paint.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                            paint.style = Paint.Style.FILL
                            paint.textAlign = Paint.Align.CENTER
                            canvas.drawText(value, 421f, y, paint)
                        }
                        val orange = android.graphics.Color.rgb(255, 106, 15)
                        val gold = android.graphics.Color.rgb(207, 149, 35)
                        val dark = android.graphics.Color.rgb(25, 46, 65)
                        canvas.drawColor(android.graphics.Color.rgb(255, 253, 247))
                        paint.style = Paint.Style.FILL
                        paint.color = dark
                        canvas.drawRect(0f, 0f, 842f, 24f, paint)
                        canvas.drawRect(0f, 567f, 842f, 595f, paint)
                        paint.color = orange
                        canvas.drawRect(0f, 24f, 842f, 29f, paint)
                        canvas.drawRect(0f, 562f, 842f, 567f, paint)
                        paint.color = gold
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 5f
                        canvas.drawRoundRect(22f, 41f, 820f, 554f, 8f, 8f, paint)
                        paint.strokeWidth = 1.5f
                        canvas.drawRoundRect(31f, 50f, 811f, 545f, 6f, 6f, paint)
                        paint.style = Paint.Style.FILL
                        text("K A R I R M A T E", 93f, 22f, orange, true)
                        text("S E R T I F I K A T", 154f, 43f, dark, true)
                        text("PENYELESAIAN PELATIHAN", 184f, 19f, dark, true)
                        text("Dengan ini menyatakan bahwa", 230f, 16f)
                        text(item.optString("name").take(44), 268f, 38f, orange, true)
                        paint.color = gold
                        paint.strokeWidth = 1.5f
                        canvas.drawLine(230f, 280f, 612f, 280f, paint)
                        text("Telah berhasil menyelesaikan program pelatihan kerja digital", 313f, 16f)
                        text(item.optString("course").take(55), 344f, 23f, dark, true)
                        text("Nilai akhir: ${item.optInt("score")}/100   •   Status: LULUS", 373f, 17f)
                        paint.color = gold
                        paint.strokeWidth = 1.2f
                        canvas.drawLine(110f, 467f, 305f, 467f, paint)
                        canvas.drawLine(535f, 467f, 730f, 467f, paint)
                        fun signature(value: String, role: String, x: Float) {
                            paint.color = dark
                            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            paint.textAlign = Paint.Align.CENTER
                            paint.textSize = 16f
                            canvas.drawText(value, x, 489f, paint)
                            paint.textSize = 12f
                            canvas.drawText(role, x, 508f, paint)
                        }
                        signature("Tanti Kurnia", "Training Manager", 207f)
                        signature("Ilham Ramdhani", "Program Director", 633f)
                        val qrPayload = if (item.optBoolean("serverRegistered") && BuildConfig.TRAINING_BASE_URL.startsWith("https://"))
                            TrainingCertificateIdentity.verifiedPublicUrl(item.optString("id"), BuildConfig.TRAINING_BASE_URL, true, false)
                        else TrainingCertificateIdentity.payload(item.optString("id"), item.optString("courseId"), item.optString("date"), item.optInt("score"))
                        val qr = QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 104, 104)
                        val cell = 0.92f
                        paint.style = Paint.Style.FILL
                        for (y in 0 until qr.height) for (x in 0 until qr.width) {
                            paint.color = if (qr[x, y]) Color.BLACK else Color.WHITE
                            canvas.drawRect(373f + x * cell, 402f + y * cell, 373f + (x + 1) * cell, 402f + (y + 1) * cell, paint)
                        }
                        text("Tanggal: ${item.optString("date")}  •  ID: ${item.optString("id")}", 538f, 10f)
                        text(if (item.optBoolean("serverRegistered")) "Pindai QR untuk verifikasi sertifikat melalui server KarirMate." else "QR identitas lokal; bukan verifikasi online.", 550f, 9f)
                        text("Pelatihan internal KarirMate • Bukan sertifikasi kompetensi BNSP.", 550f, 9f)
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
