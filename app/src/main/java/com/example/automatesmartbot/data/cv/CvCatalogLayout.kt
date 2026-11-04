package com.example.automatesmartbot.data.cv

import android.graphics.Paint
import android.graphics.Typeface

/** Structural choices, not color or arbitrary pixel offsets. Original 14 designs retain their renderer. */
data class CvDesign(val header: Int, val columns: Int, val hierarchy: Int)
data class CvRectangle(val x: Float, val y: Float, val width: Float, val height: Float, val color: Int = 0xFFE9F0F4.toInt())

internal object CvCatalogLayout {
    fun layout(content: CvContent, template: CvTemplate): List<CvPage> {
        val design = requireNotNull(template.design)
        val pages = mutableListOf(mutableListOf<CvDrawText>())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ink = 0xFF172744.toInt()
        val accentInk = 0xFFA8430F.toInt()
        val photo = template.supportsPhoto && content.contact.photoPath.isNotBlank()
        fun page(index: Int): MutableList<CvDrawText> {
            while (pages.size <= index) pages.add(mutableListOf())
            return pages[index]
        }
        class Flow(val x: Float, val width: Float, var index: Int = 0, var y: Float = 44f) {
            fun reserve(height: Float) { if (y + height > 786f) { index++; y = 48f } }
            fun text(value: String, size: Float = 10.5f, bold: Boolean = false, centered: Boolean = false, color: Int = ink) {
                paint.textSize = size; paint.typeface = CvRenderer.typeface(bold)
                value.lines().forEach { paragraph ->
                    var rest = paragraph.trim()
                    if (rest.isEmpty()) { reserve(size); y += size; return@forEach }
                    while (rest.isNotEmpty()) {
                        reserve(size * 1.5f)
                        var count = paint.breakText(rest, true, width, null).coerceAtLeast(1)
                        if (count < rest.length) rest.lastIndexOf(' ', count - 1).takeIf { it > 0 }?.let { count = it + 1 }
                        val line = rest.take(count).trim()
                        page(index).add(CvDrawText(line, if (centered) x + (width - paint.measureText(line)) / 2 else x, y + size, size, bold, color))
                        rest = rest.drop(count).trimStart(); y += size * 1.5f
                    }
                }
            }
        }
        val photoBox = if (photo) when (design.header) {
            1 -> CvRectangle(265f, 32f, 64f, 64f)
            2 -> CvRectangle(264f, 44f, 64f, 64f)
            4 -> CvRectangle(42f, 40f, 64f, 64f)
            else -> CvRectangle(489f, 40f, 64f, 64f)
        } else null
        val contacts = listOf(content.contact.email, content.contact.phone, content.contact.location,
            content.contact.website, content.contact.linkedin, content.contact.github).filter(String::isNotBlank)
        val hx = if (design.header == 4) 128f else 42f
        val hw = if (design.header == 2) 208f else 553f - hx - if (photo && design.header in listOf(0,3)) 86f else 0f
        val header = Flow(hx, hw, y = if (design.header == 1 && photo) 112f else 44f)
        header.text(content.contact.fullName.ifBlank { "Curriculum Vitae" }, if (design.header == 3) 26f else 22f, true, design.header == 1, accentInk)
        if (content.contact.headline.isNotBlank()) header.text(content.contact.headline, 12f, true, design.header == 1)
        var headerEnd = header.y
        var headerPage = header.index
        if (design.header == 2) {
            val right = Flow(344f, 209f)
            contacts.forEach { right.text(it, 9.5f) }
            headerPage = maxOf(headerPage, right.index)
            headerEnd = maxOf(headerEnd, right.y, if (photo) 112f else 0f)
        } else {
            contacts.chunked(if (design.header in listOf(1,3)) 2 else 1).forEach {
                header.text(it.joinToString(" | "), 9.5f, centered = design.header == 1)
            }
            headerEnd = maxOf(header.y, if (photo) 112f else 0f); headerPage = header.index
        }
        val bodyStart = headerEnd + 28f
        val groups = mutableListOf<MutableList<CvTextBlock>>()
        CvRenderer.blocks(content, template).forEach { block ->
            if (block.heading || groups.isEmpty()) groups.add(mutableListOf())
            groups.last().add(block)
        }
        val focus = listOf("PENGALAMAN KERJA", "PENDIDIKAN", "KETERAMPILAN", "PROYEK")[design.hierarchy]
        val ordered = groups.sortedBy { when (it.first().text) { focus -> 0; "PROFIL" -> 1; else -> 2 } }
        val left = when (design.columns) {
            1 -> Flow(42f, 242f, headerPage, bodyStart)
            2 -> Flow(42f, 320f, headerPage, bodyStart)
            3 -> Flow(42f, 160f, headerPage, bodyStart)
            else -> Flow(42f, 511f, headerPage, bodyStart)
        }
        val right = when (design.columns) {
            1 -> Flow(311f, 242f, headerPage, bodyStart)
            2 -> Flow(393f, 160f, headerPage, bodyStart)
            3 -> Flow(233f, 320f, headerPage, bodyStart)
            else -> left
        }
        ordered.forEachIndexed { index, group ->
            val flow = when (design.columns) {
                0 -> left
                1 -> if (index % 2 == 0) left else right
                2 -> if (index % 3 == 1) right else left
                else -> if (index % 3 == 1) left else right
            }
            flow.reserve(62f); flow.y += 12f
            group.forEach { if (it.heading) flow.text(it.text, 11.5f, true, color = accentInk) else flow.text(it.text) }
            flow.y += 8f
        }
        return pages.mapIndexed { index, texts ->
            val decorations = buildList {
                if (index == 0) when (design.header) {
                    0 -> add(CvRectangle(42f, 28f, 128f, 4f, template.accent))
                    1 -> add(CvRectangle(180f, 24f, 235f, 3f, template.accent))
                    2 -> add(CvRectangle(330f, 40f, 1f, (headerEnd - 30f).coerceAtLeast(72f), template.accent))
                    3 -> add(CvRectangle(24f, 24f, 547f, headerEnd + 10f, 0xFFF0F3F6.toInt()))
                    4 -> add(CvRectangle(116f, 36f, 3f, (headerEnd - 20f).coerceAtLeast(80f), template.accent))
                }
                if (design.columns != 0) {
                    val start = if (index == headerPage) bodyStart else 48f
                    val x = when (design.columns) { 1 -> 297f; 2 -> 377f; else -> 217f }
                    if (start < 786f) add(CvRectangle(x, start, 1f, 786f - start))
                }
            }
            CvPage(texts, template, index == 0, content.contact.photoPath, decorations, if (index == 0) photoBox else null)
        }
    }
}
