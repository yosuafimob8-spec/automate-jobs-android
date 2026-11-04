package com.example.automatesmartbot.data.cv

import android.graphics.*
import android.graphics.pdf.PdfDocument

data class CvTextBlock(val text: String, val heading: Boolean = false, val title: Boolean = false)
data class CvDrawText(val text: String, val x: Float, val baseline: Float, val size: Float, val bold: Boolean, val color: Int)
data class CvPage(val texts: List<CvDrawText>, val template: CvTemplate, val first: Boolean, val photoPath: String = "")

/** Layout once in A4 points; the exact same pages are painted into preview and PDF. */
object CvRenderer {
    private var regular = Typeface.DEFAULT
    private var boldFace = Typeface.DEFAULT_BOLD
    fun initialize(context: android.content.Context) {
        regular = Typeface.createFromAsset(context.assets, "fonts/DejaVuSans.ttf")
        boldFace = Typeface.createFromAsset(context.assets, "fonts/DejaVuSans-Bold.ttf")
    }
    const val WIDTH = 595
    const val HEIGHT = 842
    private const val INK = 0xFF172744.toInt()
    private fun join(vararg values: String) = values.filter { it.isNotBlank() }.joinToString(" — ")

    fun blocks(c: CvContent, template: CvTemplate): List<CvTextBlock> {
        val sections = linkedMapOf<String, List<CvTextBlock>>()
        fun section(id: String, title: String, body: List<String>) {
            val lines = body.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) sections[id] = listOf(CvTextBlock(title, heading = true)) + lines.map { CvTextBlock(it) }
        }
        section("summary", "PROFIL", listOf(c.summary))
        section("experience", "PENGALAMAN KERJA", c.experience.flatMap { e ->
            listOf(join(e.title, e.company, e.location), join(e.start, if (e.current) "Sekarang" else e.end)) + e.bullets.filter { it.isNotBlank() }.map { "• $it" }
        })
        section("education", "PENDIDIKAN", c.education.flatMap { e ->
            listOf(join(e.school, e.degree, e.field), join(e.start, e.end, e.location, if (e.gpa.isBlank()) "" else "IPK ${e.gpa}")) + e.details
        })
        section("skills", "KETERAMPILAN", c.skills.map { join(it.category, it.items.filter { s -> s.isNotBlank() }.joinToString(", ")) })
        section("projects", "PROYEK", c.projects.flatMap { p -> listOf(join(p.name, p.tech), join(p.start, p.end), p.link) + p.bullets })
        section("certifications", "SERTIFIKASI", c.certifications.map { join(it.name, it.issuer, it.date, it.link) })
        c.extras.forEach { section(it.id, it.title.uppercase(), listOf(it.text)) }
        val priority = when (template.layout) {
            CvLayout.ACADEMIC -> listOf("summary", "education", "projects", "skills")
            CvLayout.SKILLS_FIRST -> listOf("summary", "skills", "projects")
            CvLayout.PORTFOLIO -> listOf("summary", "projects", "experience")
            else -> emptyList()
        }
        return (priority + c.sectionOrder + sections.keys).distinct().flatMap { sections[it].orEmpty() }
    }

    fun layout(c: CvContent, templateId: String): List<CvPage> {
        val template = CvTemplates.resolve(templateId)
        val l = template.layout
        // Per-template geometry variation, shared by preview and exported PDF.
        val variant = template.id.substringAfterLast('_').toIntOrNull()?.minus(1) ?: 0
        val inset = (variant % 3) * 7f
        val headerScale = 1f + (variant % 4) * 0.045f
        val lineScale = 1f + (variant % 3) * 0.035f
        val sidebar = l == CvLayout.SIDEBAR_LEFT || l == CvLayout.SIDEBAR_RIGHT
        val left = (when (l) { CvLayout.SIDEBAR_LEFT -> 205f; CvLayout.TIMELINE -> 100f; CvLayout.EDITORIAL -> 78f; else -> 42f }) + inset
        val right = (if (l == CvLayout.SIDEBAR_RIGHT) 390f else 553f) - inset
        val bodySize = (if (l == CvLayout.COMPACT) 10f else 11f) * lineScale
        // Rotate section hierarchy across the catalog without changing saved user content.
        val sectionVariant = variant % 4
        val output = mutableListOf<CvPage>()
        var text = mutableListOf<CvDrawText>()
        var first = true
        var y = 48f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun finish() { output += CvPage(text.toList(), template, first, c.contact.photoPath); text = mutableListOf(); first = false; y = 50f }
        fun line(value: String, size: Float = bodySize, bold: Boolean = false, color: Int = INK, x: Float = left, width: Float = right - x, centered: Boolean = false) {
            paint.textSize = size; paint.typeface = if (bold) boldFace else regular
            for (paragraph in value.split('\n')) {
                var rest = paragraph.trim()
                if (rest.isEmpty()) { y += size * 0.8f; continue }
                while (rest.isNotEmpty()) {
                    if (y + size * 1.4f > HEIGHT - 48) finish()
                    val count = paint.breakText(rest, true, width.coerceAtLeast(20f), null).coerceAtLeast(1)
                    var end = count
                    if (count < rest.length) { val space = rest.lastIndexOf(' ', count - 1); if (space > 0) end = space + 1 }
                    val part = rest.substring(0, end).trim()
                    text += CvDrawText(part, if (centered) x + (width - paint.measureText(part)) / 2 else x, y + size, size, bold, color)
                    y += size * 1.5f
                    rest = rest.substring(end).trimStart()
                }
            }
        }
        val photo = template.supportsPhoto && c.contact.photoPath.isNotBlank()
        val headerX = if (l == CvLayout.MONOGRAM) 112f else left
        val headerWidth = right - headerX - if (photo && !sidebar) 86f else 0f
        if (l == CvLayout.BANNER || l == CvLayout.EXECUTIVE) y = 62f
        line(c.contact.fullName.ifBlank { "Curriculum Vitae" }, (if (l == CvLayout.EXECUTIVE) 28f else 23f) * headerScale, true, template.accent, headerX, headerWidth, l == CvLayout.CENTERED)
        if (c.contact.headline.isNotBlank()) line(c.contact.headline, 12f, true, x = headerX, width = headerWidth, centered = l == CvLayout.CENTERED)
        val contacts = listOf(c.contact.email, c.contact.phone, c.contact.location, c.contact.website, c.contact.linkedin, c.contact.github).filter { it.isNotBlank() }
        if (sidebar) {
            val savedY = y
            y = if (photo) 158f else 60f
            val sx = if (l == CvLayout.SIDEBAR_LEFT) 42f else 425f
            line("KONTAK", 11f, true, template.accent, sx, 130f)
            contacts.forEach { line(it, 10f, x = sx, width = 130f) }
            y = maxOf(savedY + 22f, 160f)
        } else {
            contacts.forEach { line(it, 10f, x = left, width = right - left - if (photo) 86f else 0f, centered = l == CvLayout.CENTERED) }
            y = maxOf(y + 18f, if (photo) 150f else 0f)
        }
        val contentBlocks = blocks(c, template)
        val arrangedBlocks = if (sectionVariant == 0) contentBlocks else {
            val sections = mutableListOf<MutableList<CvTextBlock>>()
            contentBlocks.forEach { block ->
                if (block.heading || sections.isEmpty()) sections.add(mutableListOf())
                sections.last().add(block)
            }
            val reordered = when (sectionVariant) {
                1 -> sections.take(1) + sections.drop(1).reversed()
                2 -> sections.sortedBy { it.firstOrNull()?.text?.length ?: 0 }
                else -> sections.take(1) + sections.drop(1).sortedByDescending { it.size }
            }
            reordered.flatten()
        }
        arrangedBlocks.forEach { block ->
            if (block.heading) {
                // A heading and at least two following lines must fit on the same page.
                if (y + 66f > HEIGHT - 48) finish()
                y += (if (l == CvLayout.COMPACT) 9f else 17f) + (variant % 3) * 3f
                line(block.text, 12f, true, template.accent)
                y += 4f
            } else line(block.text)
        }
        finish()
        return output
    }

    fun letterPages(content: String): List<CvPage> {
        val template = CvTemplates.all.first()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = regular }
        val pages = mutableListOf<CvPage>(); var lines = mutableListOf<CvDrawText>(); var y = 58f
        fun finish() { pages += CvPage(lines.toList(), template, pages.isEmpty()); lines = mutableListOf(); y = 58f }
        content.lines().forEach { paragraph ->
            var remaining = paragraph
            if (remaining.isBlank()) y += 14f
            while (remaining.isNotEmpty()) {
                if (y > 780f) finish()
                val count = paint.breakText(remaining, true, 479f, null).coerceAtLeast(1)
                val space = if (count < remaining.length) remaining.lastIndexOf(' ', count - 1) else -1
                val end = if (space > 0) space + 1 else count
                lines += CvDrawText(remaining.substring(0, end).trim(), 58f, y, 12f, false, INK)
                y += 19f; remaining = remaining.substring(end).trimStart()
            }
        }
        finish(); return pages
    }

    fun draw(canvas: Canvas, page: CvPage, pageNumber: Int, includeText: Boolean = true) {
        canvas.drawColor(Color.WHITE)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = page.template.accent
        when (page.template.layout) {
            CvLayout.SIDEBAR_LEFT -> { p.color = 0xFFE9F0F4.toInt(); canvas.drawRect(24f, 24f, 184f, 812f, p) }
            CvLayout.SIDEBAR_RIGHT -> { p.color = 0xFFE9F0F4.toInt(); canvas.drawRect(410f, 24f, 571f, 812f, p) }
            CvLayout.BANNER -> canvas.drawRect(0f, 0f, WIDTH.toFloat(), 22f, p)
            CvLayout.EXECUTIVE -> { canvas.drawRect(28f, 28f, 34f, 160f, p); canvas.drawRect(42f, 30f, 553f, 34f, p) }
            CvLayout.EDITORIAL -> canvas.drawRect(42f, 42f, 48f, 790f, p)
            CvLayout.TIMELINE -> canvas.drawRect(72f, 172f, 74f, 790f, p)
            CvLayout.PORTFOLIO -> { canvas.drawRect(0f, 0f, 16f, 842f, p); canvas.drawRect(16f, 0f, 130f, 15f, p) }
            CvLayout.MONOGRAM -> if (page.first) {
                canvas.drawCircle(70f, 73f, 28f, p)
                p.color = Color.WHITE; p.textSize = 22f; p.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(page.texts.firstOrNull()?.text?.take(1) ?: "C", 62f, 81f, p)
            }
            CvLayout.ACADEMIC -> canvas.drawRect(42f, 28f, 553f, 31f, p)
            CvLayout.SKILLS_FIRST -> canvas.drawRect(42f, 24f, 180f, 30f, p)
            else -> Unit
        }
        if (page.first) {
            // Distinct visual signature per catalog entry, visible in preview and PDF.
            val variant = page.template.id.substringAfterLast('_').toIntOrNull() ?: 1
            p.color = page.template.accent
            when (variant % 5) {
                0 -> { p.strokeWidth = 2f; canvas.drawLine(42f, 135f, 553f, 135f, p) }
                1 -> canvas.drawRoundRect(42f, 24f, 150f + variant * 2f, 29f, 3f, 3f, p)
                2 -> { p.style = Paint.Style.STROKE; p.strokeWidth = 2f; canvas.drawRoundRect(28f, 28f, 567f, 814f, 12f, 12f, p); p.style = Paint.Style.FILL }
                3 -> canvas.drawRect(540f - variant * 2f, 28f, 553f, 100f, p)
                else -> { p.strokeWidth = 3f; canvas.drawLine(42f, 34f, 42f, 120f + variant * 2f, p) }
            }
        }
        page.texts.forEach { t ->
            p.color = t.color; p.textSize = t.size; p.typeface = if (t.bold) boldFace else regular
            if (includeText) canvas.drawText(t.text, t.x, t.baseline, p)
            if (t.bold && t.size == 12f && page.template.layout == CvLayout.RULED) {
                p.strokeWidth = 0.7f; canvas.drawLine(t.x, t.baseline + 4f, 553f, t.baseline + 4f, p)
            }
        }
        if (page.first && page.template.supportsPhoto && page.photoPath.isNotBlank()) {
            val bitmap = BitmapFactory.decodeFile(page.photoPath)
            if (bitmap != null) {
                val x = when (page.template.layout) { CvLayout.SIDEBAR_LEFT -> 80f; CvLayout.SIDEBAR_RIGHT -> 447f; else -> 474f }
                val rect = RectF(x, 48f, x + 64f, 112f)
                val side = minOf(bitmap.width, bitmap.height)
                canvas.save(); canvas.clipPath(Path().apply { addOval(rect, Path.Direction.CW) })
                canvas.drawBitmap(bitmap, Rect((bitmap.width - side) / 2, (bitmap.height - side) / 2, (bitmap.width + side) / 2, (bitmap.height + side) / 2), rect, p)
                canvas.restore(); bitmap.recycle()
            }
        }
        p.color = INK; p.textSize = 9f; p.typeface = Typeface.DEFAULT
        canvas.drawText("$pageNumber", 540f, 818f, p)
    }

    fun bitmap(page: CvPage, number: Int, width: Int = 900): Bitmap =
        Bitmap.createBitmap(width, width * HEIGHT / WIDTH, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it); canvas.scale(width / WIDTH.toFloat(), width / WIDTH.toFloat()); draw(canvas, page, number)
        }

    fun writePdf(context: android.content.Context, pages: List<CvPage>, output: java.io.OutputStream) {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        initialize(context)
        com.tom_roush.pdfbox.pdmodel.PDDocument().use { document ->
            val normal = context.assets.open("fonts/DejaVuSans.ttf").use { com.tom_roush.pdfbox.pdmodel.font.PDType0Font.load(document, it) }
            val bold = context.assets.open("fonts/DejaVuSans-Bold.ttf").use { com.tom_roush.pdfbox.pdmodel.font.PDType0Font.load(document, it) }
            pages.forEachIndexed { index, data ->
                val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle(WIDTH.toFloat(), HEIGHT.toFloat()))
                document.addPage(page)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(document, page).use { stream ->
                    // Decorative geometry and photo use the same drawing code as the preview.
                    // The CV text is embedded separately as selectable Unicode text.
                    val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
                    draw(Canvas(bitmap), data, index + 1, includeText = false)
                    val background = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, bitmap)
                    stream.drawImage(background, 0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat())
                    bitmap.recycle()
                    data.texts.forEach { t ->
                        stream.beginText()
                        stream.setFont(if (t.bold) bold else normal, t.size)
                        stream.setNonStrokingColor(Color.red(t.color), Color.green(t.color), Color.blue(t.color))
                        stream.newLineAtOffset(t.x, HEIGHT - t.baseline)
                        stream.showText(t.text)
                        stream.endText()
                    }
                }
            }
            document.save(output)
        }
    }
}
