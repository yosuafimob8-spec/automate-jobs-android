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
        c.extras.forEach { section(it.title, listOf(it.text)) }
    }.filter { it.isNotBlank() || it.isEmpty() }

    suspend fun exportPdf(context: Context, uri: Uri, content: CvContent, templateId: String = "ats_01") {
        CvExportService().export(context, uri, CvExportRequest(templateId, content), CvExportFormat.PDF)
    }

    fun exportLetter(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            CvRenderer.writePdf(context, CvRenderer.letterPages(content), it)
        } ?: error("Tidak dapat membuka lokasi penyimpanan")
    }

    private fun escapeXml(s: String): String = buildString {
        s.forEach { c -> append(when (c) {
            '&' -> "&amp;"; '<' -> "&lt;"; '>' -> "&gt;"; '"' -> "&quot;"; '\'' -> "&apos;"
            else -> if (c.code in 0..8 || c.code in 11..12 || c.code in 14..31) "" else c.toString()
        }) }
    }

    suspend fun exportDocx(context: Context, uri: Uri, content: CvContent, templateId: String = "ats_01") {
        CvExportService().export(context, uri, CvExportRequest(templateId, content), CvExportFormat.DOCX)
    }

    /** Word frames and page artwork use exactly the A4 geometry used by preview/PDF. */
    fun writeDocx(pages: List<CvPage>, output: java.io.OutputStream) {
        require(pages.isNotEmpty()) { "Dokumen kosong." }
        val images = mutableListOf<ByteArray>()
        val body = buildString {
            pages.forEachIndexed { index, page ->
                if (index > 0) append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                val bitmap = android.graphics.Bitmap.createBitmap(CvRenderer.WIDTH, CvRenderer.HEIGHT, android.graphics.Bitmap.Config.ARGB_8888)
                CvRenderer.draw(android.graphics.Canvas(bitmap), page, index + 1, includeText = false)
                val bytes = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, bytes); bitmap.recycle(); images.add(bytes.toByteArray())
                // Behind-document artwork, anchored to the same page as its editable text frames.
                append("""<w:p><w:pPr><w:spacing w:after="0" w:line="1" w:lineRule="exact"/></w:pPr><w:r><w:drawing><wp:anchor distT="0" distB="0" distL="0" distR="0" simplePos="0" relativeHeight="0" behindDoc="1" locked="0" layoutInCell="1" allowOverlap="1"><wp:simplePos x="0" y="0"/><wp:positionH relativeFrom="page"><wp:posOffset>0</wp:posOffset></wp:positionH><wp:positionV relativeFrom="page"><wp:posOffset>0</wp:posOffset></wp:positionV><wp:extent cx="7556500" cy="10693400"/><wp:wrapNone/><wp:docPr id="${index + 1}" name="CV page ${index + 1}"/><wp:cNvGraphicFramePr/><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="0" name="Artwork"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="rId${index + 1}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="7556500" cy="10693400"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:anchor></w:drawing></w:r></w:p>""")
                page.texts.forEach { text ->
                    val x = (text.x * 20).toInt(); val y = ((text.baseline - text.size) * 20).toInt()
                    val color = "%06X".format(text.color and 0xFFFFFF)
                    append("""<w:p><w:pPr><w:framePr w:w="${(CvRenderer.WIDTH * 20 - x - 400).coerceAtLeast(200)}" w:h="${(text.size * 32).toInt()}" w:x="$x" w:y="$y" w:hAnchor="page" w:vAnchor="page" w:wrap="none"/><w:spacing w:before="0" w:after="0"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii="DejaVu Sans" w:hAnsi="DejaVu Sans"/><w:sz w:val="${(text.size * 2).toInt()}"/><w:color w:val="$color"/>${if (text.bold) "<w:b/>" else ""}</w:rPr><w:t xml:space="preserve">${escapeXml(text.text)}</w:t></w:r></w:p>""")
                }
            }
        }
        val namespaces = """xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture" """
        val document = """<?xml version="1.0" encoding="UTF-8"?><w:document $namespaces><w:body>$body<w:sectPr><w:pgSz w:w="11900" w:h="16840"/><w:pgMar w:top="0" w:right="0" w:bottom="0" w:left="0" w:header="0" w:footer="0" w:gutter="0"/></w:sectPr></w:body></w:document>"""
        val types = """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
        val relationships = """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">${images.indices.joinToString("") { "<Relationship Id=\"rId${it + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/page${it + 1}.png\"/>" }}</Relationships>"""
        val root = """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
            entry("[Content_Types].xml", types.toByteArray()); entry("_rels/.rels", root.toByteArray())
            entry("word/document.xml", document.toByteArray()); entry("word/_rels/document.xml.rels", relationships.toByteArray())
            images.forEachIndexed { index, bytes -> entry("word/media/page${index + 1}.png", bytes) }
        }
    }
}
