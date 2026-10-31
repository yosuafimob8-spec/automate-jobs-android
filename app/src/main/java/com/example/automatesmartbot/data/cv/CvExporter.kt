package com.example.automatesmartbot.data.cv

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Exports user-owned CV data through Android's document picker. */
object CvExporter {
    private fun lines(c: CvContent): List<String> = buildList {
        add(c.contact.fullName.ifBlank { "Curriculum Vitae" })
        if (c.contact.headline.isNotBlank()) add(c.contact.headline)
        add(listOf(c.contact.email, c.contact.phone, c.contact.location, c.contact.website,
            c.contact.linkedin, c.contact.github).filter { it.isNotBlank() }.joinToString(" | "))
        fun section(title: String, items: List<String>) {
            if (items.isNotEmpty()) { add(""); add(title); addAll(items) }
        }
        section("PROFIL", listOf(c.summary).filter { it.isNotBlank() })
        section("PENGALAMAN KERJA", c.experience.flatMap { e ->
            listOf("${e.title} - ${e.company} (${e.start} - ${if (e.current) "Sekarang" else e.end})") + e.bullets.filter { it.isNotBlank() }.map { "• $it" }
        })
        section("PENDIDIKAN", c.education.flatMap { e ->
            listOf("${e.school} - ${e.degree} ${e.field} (${e.start} - ${e.end})") + e.details.filter { it.isNotBlank() }
        })
        section("KEAHLIAN", c.skills.map { "${it.category}: ${it.items.joinToString(", ")}" })
        section("PROYEK", c.projects.flatMap { p -> listOf("${p.name} - ${p.tech} ${p.link}") + p.bullets })
        section("SERTIFIKASI", c.certifications.map { "${it.name} - ${it.issuer} (${it.date}) ${it.link}" })
    }.filter { it.isNotBlank() || it.isEmpty() }

    fun exportPdf(context: Context, uri: Uri, content: CvContent) {
        val doc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = android.graphics.Color.BLACK }
        val width = 595; val height = 842; val margin = 48f
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = margin
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            y = margin
        }
        try {
            newPage()
            for (line in lines(content)) {
                if (line.isBlank()) { y += 10f; continue }
                var remaining = line
                while (remaining.isNotEmpty()) {
                    if (y > height - margin) newPage()
                    val available = width - 2 * margin
                    val count = paint.breakText(remaining, true, available, null).coerceAtLeast(1)
                    var end = count
                    if (count < remaining.length) {
                        val space = remaining.lastIndexOf(' ', count - 1)
                        if (space > 0) end = space + 1
                    }
                    page!!.canvas.drawText(remaining.substring(0, end).trim(), margin, y, paint)
                    remaining = remaining.substring(end).trimStart()
                    y += 17f
                }
            }
            page?.let { doc.finishPage(it) }
            context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                ?: error("Tidak dapat membuka lokasi penyimpanan")
        } finally { doc.close() }
    }

    private fun escapeXml(s: String): String = buildString {
        s.forEach { c -> append(when (c) {
            '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&apos;"
            else -> if (c.code in 0..8 || c.code in 11..12 || c.code in 14..31) "" else c.toString()
        }) }
    }

    fun exportDocx(context: Context, uri: Uri, content: CvContent) {
        val paragraphs = lines(content).joinToString("") { line ->
            "<w:p><w:r><w:t xml:space=\"preserve\">${escapeXml(line)}</w:t></w:r></w:p>"
        }
        val document = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paragraphs<w:sectPr/></w:body></w:document>"""
        val contentTypes = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val rels = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                for ((name, data) in listOf("[Content_Types].xml" to contentTypes, "_rels/.rels" to rels, "word/document.xml" to document)) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        } ?: error("Tidak dapat membuka lokasi penyimpanan")
    }
}
