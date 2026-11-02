package com.example.automatesmartbot.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import com.example.automatesmartbot.BuildConfig
import com.example.automatesmartbot.data.career.TrainingCertificateIdentity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

/** One landscape artwork for the in-app preview and exported PDF. Coordinates: 842 x 595. */
internal object TrainingCertificateArtwork {
    private const val W = 842f
    private const val H = 595f
    private val navy = Color.rgb(12, 35, 86)
    private val orange = Color.rgb(255, 105, 13)
    private val gold = Color.rgb(207, 151, 37)
    private val cream = Color.rgb(255, 253, 247)
    fun draw(canvas: Canvas, item: JSONObject, context: Context) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun box(color: Int, l: Float, t: Float, r: Float, b: Float) {
            p.color = color; p.style = Paint.Style.FILL
            canvas.drawRect(l, t, r, b, p)
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = gold, width: Float = 1f) {
            p.color = color; p.strokeWidth = width; p.style = Paint.Style.STROKE
            canvas.drawLine(x1, y1, x2, y2, p); p.style = Paint.Style.FILL
        }
        fun label(value: String, x: Float, y: Float, size: Float, color: Int = navy, bold: Boolean = false, serif: Boolean = false) {
            p.color = color; p.style = Paint.Style.FILL
            p.typeface = Typeface.create(if (serif) Typeface.SERIF else Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            p.textSize = size; p.textAlign = Paint.Align.CENTER
            val fitted = if (p.measureText(value) > 730f) {
                p.textSize *= 730f / p.measureText(value)
                value
            } else value
            canvas.drawText(fitted, x, y, p)
        }
        canvas.drawColor(cream)
        box(navy, 0f, 0f, W, 17f)
        box(navy, 0f, 555f, W, H)
        box(orange, 0f, 17f, W, 21f)
        box(orange, 0f, 551f, W, 555f)
        // Orange corner ribbons and the thin gold inset frame.
        val ribbon = Path().apply { moveTo(0f, 400f); lineTo(0f, 555f); lineTo(118f, 555f); close() }
        p.color = orange; canvas.drawPath(ribbon, p)
        val right = Path().apply { moveTo(W, 400f); lineTo(W, 555f); lineTo(W - 118f, 555f); close() }
        canvas.drawPath(right, p)
        p.color = gold; p.style = Paint.Style.STROKE; p.strokeWidth = 2f
        canvas.drawRoundRect(23f, 34f, 819f, 542f, 9f, 9f, p)
        p.style = Paint.Style.FILL
        for (x in listOf(44f, 798f)) {
            for (i in 0..2) {
                line(x - 13f + i * 8f, 54f, x + 18f + i * 8f, 54f, gold, 1.5f)
                line(x - 13f + i * 8f, 54f, x - 13f + i * 8f, 85f, gold, 1.5f)
            }
        }
        label("▣  KarirMate", 421f, 76f, 33f, orange, true)
        label("PROGRAM PELATIHAN KERJA DIGITAL", 421f, 97f, 12f)
        label("S E R T I F I K A T", 421f, 160f, 47f, navy, true, true)
        label("P E N Y E L E S A I A N   P E L A T I H A N", 421f, 188f, 15f, gold, true)
        label("Dengan ini menyatakan bahwa:", 421f, 221f, 15f)
        label(item.optString("name").ifBlank { "Nama Peserta" }, 421f, 273f, 43f, orange, false, true)
        line(220f, 284f, 622f, 284f, orange, 1.4f)
        label("Telah berhasil menyelesaikan seluruh materi, evaluasi, dan tugas praktik", 421f, 311f, 14f)
        label("pada program pelatihan kerja digital:", 421f, 330f, 14f)
        label(item.optString("course"), 421f, 363f, 26f, navy, true)
        label("Nilai akhir: ${item.optInt("score")}/100    |    Status: LULUS", 421f, 390f, 16f, navy, true)
        // Certificate-only stylized signatory marks; not a reproduction of a legal signature.
        fun elegantMark(text: String, centerX: Float, baseline: Float, size: Float) {
            val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                typeface = Typeface.create("cursive", Typeface.BOLD_ITALIC)
                textSize = size
                textAlign = Paint.Align.CENTER
                isSubpixelText = true
            }
            canvas.drawText(text, centerX, baseline, ink)
            val flourish = Path().apply {
                moveTo(centerX - 90f, baseline + 7f)
                cubicTo(centerX - 38f, baseline - 5f, centerX + 24f, baseline + 20f, centerX + 93f, baseline - 1f)
            }
            val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = navy
                style = Paint.Style.STROKE
                strokeWidth = 1.6f
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(flourish, pen)
            pen.color = gold
            pen.strokeWidth = 1.7f
            val accent = Path().apply {
                moveTo(centerX - 46f, baseline + 15f)
                cubicTo(centerX - 10f, baseline + 4f, centerX + 39f, baseline + 15f, centerX + 75f, baseline + 7f)
            }
            canvas.drawPath(accent, pen)
        }
        elegantMark("T. Kurnia", 196f, 445f, 37f)
        elegantMark("I. R", 646f, 444f, 49f)
        line(97f, 469f, 295f, 469f, orange, 1.5f)
        line(547f, 469f, 745f, 469f, orange, 1.5f)
        label("Tanti Kurnia", 196f, 489f, 16f, navy, true)
        label("Training Manager", 196f, 507f, 12f)
        label("Ilham Ramdhani", 646f, 489f, 16f, navy, true)
        label("Program Director", 646f, 507f, 12f)
        val qrPayload = if (item.optBoolean("serverRegistered") && BuildConfig.TRAINING_BASE_URL.startsWith("https://"))
            TrainingCertificateIdentity.verifiedPublicUrl(item.optString("id"), BuildConfig.TRAINING_BASE_URL, true, false)
        else TrainingCertificateIdentity.payload(item.optString("id"), item.optString("courseId"), item.optString("date"), item.optInt("score"))
        val qr = QRCodeWriter().encode(qrPayload, BarcodeFormat.QR_CODE, 88, 88)
        box(Color.WHITE, 377f, 416f, 465f, 504f)
        for (y in 0 until qr.height) for (x in 0 until qr.width) {
            if (qr[x, y]) box(Color.BLACK, 377f + x, 416f + y, 378f + x, 417f + y)
        }
        label(if (item.optBoolean("serverRegistered")) "Verifikasi online" else "Identitas lokal", 421f, 517f, 10f)
        label("No: ${item.optString("id").take(30)}     |     Terbit: ${item.optString("date")}", 421f, 536f, 10f)
        label("Bukti penyelesaian pelatihan internal KarirMate • Bukan sertifikasi kompetensi BNSP", 421f, 579f, 12f, Color.WHITE)
    }
}
