package com.example.automatesmartbot.data.cv

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class ParsedCv(val text: String, val content: CvContent)

/** Shared factual parser for CV analysis, interview context and Auto Apply validation. */
object SharedCvParser {
    fun extract(context: Context, uri: Uri): ParsedCv {
        PDFBoxResourceLoader.init(context.applicationContext)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(10 * 1024 * 1024) }
            ?: error("PDF tidak dapat dibaca. Pilih ulang file.")
        require(bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-") { "File bukan PDF yang valid." }
        return PDDocument.load(bytes).use { pdf ->
            require(!pdf.isEncrypted) { "PDF dilindungi kata sandi. Gunakan PDF tanpa enkripsi." }
            require(pdf.numberOfPages in 1..30) { "PDF harus berisi 1–30 halaman." }
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            parseText(stripper.getText(pdf))
        }
    }

    private val headings = linkedMapOf(
        "summary" to listOf("profil", "ringkasan", "tentang saya", "objektif", "summary", "profile", "objective"),
        "experience" to listOf("pengalaman kerja", "pengalaman", "work experience", "experience", "employment"),
        "education" to listOf("pendidikan", "riwayat pendidikan", "education"),
        "skills" to listOf("keterampilan", "keahlian", "kemampuan", "skills", "technical skills"),
        "projects" to listOf("proyek", "portofolio", "projects", "portfolio"),
        "certifications" to listOf("sertifikasi", "sertifikat", "certifications", "certificates")
    )

    fun renderContent(cv: CvContent, includeContact: Boolean = true): String {
        return buildString {
            if (includeContact) { appendLine(cv.contact.fullName); appendLine(cv.contact.email); appendLine(cv.contact.phone); appendLine(cv.contact.location) }
            appendLine(cv.contact.headline)
            if (cv.summary.isNotBlank()) appendLine("Profil\n${cv.summary}")
            if (cv.experience.isNotEmpty()) { appendLine("Pengalaman Kerja"); cv.experience.forEach { appendLine("${it.title} ${it.company} ${it.start} ${it.end}"); it.bullets.forEach(::appendLine) } }
            if (cv.education.isNotEmpty()) { appendLine("Pendidikan"); cv.education.forEach { appendLine("${it.school} ${it.degree} ${it.field} ${it.start} ${it.end}"); it.details.forEach(::appendLine) } }
            if (cv.skills.isNotEmpty()) { appendLine("Keterampilan"); cv.skills.forEach { appendLine(it.items.joinToString(", ")) } }
            if (cv.projects.isNotEmpty()) { appendLine("Proyek"); cv.projects.forEach { appendLine("${it.name} ${it.tech}"); it.bullets.forEach(::appendLine) } }
            if (cv.certifications.isNotEmpty()) { appendLine("Sertifikasi"); cv.certifications.forEach { appendLine("${it.name} ${it.issuer}") } }
        }
    }

    fun interviewContext(cv: CvContent): String = parseText(renderContent(cv, includeContact = false)).text
        .replace(Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[kontak disembunyikan]")
        .replace(Regex("(?:\\+\\d{1,3}|08)[\\d ()-]{7,18}\\d"), "[kontak disembunyikan]")
        .take(20_000)

    fun parseText(raw: String): ParsedCv {
        require(raw.length <= 250_000) { "Teks CV maksimal 250.000 karakter." }
        val text = raw.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "").trim()
        require(text.count(Char::isLetter) >= 40) { "Teks CV belum dapat dibaca. Gunakan PDF teks; OCR belum tersedia." }
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val sections = mutableMapOf<String, MutableList<String>>()
        var section: String? = null
        lines.forEach { line ->
            val normalized = line.substringBefore(':').lowercase().trim(' ', ':', '-', '•')
            val key = headings.entries.firstOrNull { normalized in it.value }?.key
            if (key != null) { section = key; val values = sections.getOrPut(key) { mutableListOf() }; if (line.contains(':')) line.substringAfter(':').trim().takeIf { it.isNotBlank() }?.let(values::add) }
            else section?.let { sections.getOrPut(it) { mutableListOf() }.add(line) }
        }
        val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").find(text)?.value.orEmpty()
        val phone = Regex("(?:\\+62|08|\\+\\d{1,3})[\\d ()-]{7,18}\\d").find(text)?.value?.trim().orEmpty()
        // Keep extracted source lines intact; never infer an employer, degree, or skill.
        val cv = CvContent(
            contact = CvContact(fullName = lines.firstOrNull()?.takeIf { it.length < 90 && '@' !in it && headings.values.none { names -> it.lowercase() in names } }.orEmpty(), email = email, phone = phone),
            summary = sections["summary"]?.joinToString("\n").orEmpty(),
            experience = sections["experience"]?.takeIf { it.isNotEmpty() }?.let { listOf(CvExperience(bullets = it.toList())) }.orEmpty(),
            education = sections["education"]?.takeIf { it.isNotEmpty() }?.let { listOf(CvEducation(details = it.toList())) }.orEmpty(),
            skills = sections["skills"]?.takeIf { it.isNotEmpty() }?.let { listOf(CvSkillGroup(items = it.toList())) }.orEmpty(),
            projects = sections["projects"]?.takeIf { it.isNotEmpty() }?.let { listOf(CvProject(bullets = it.toList())) }.orEmpty(),
            certifications = sections["certifications"]?.map { CvCertification(name = it) }.orEmpty()
        )
        return ParsedCv(text, cv)
    }
}
