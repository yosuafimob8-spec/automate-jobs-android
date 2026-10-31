package com.example.automatesmartbot.data.cv

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

data class PdfCvAnalysis(
    val source: String, val score: Int, val strengths: List<String>, val weaknesses: List<String>,
    val recommendations: List<String>, val cv: CvContent, val extractedText: String,
    val analyzedAt: Long = System.currentTimeMillis()
)

/** Local, rule-based completeness analysis. A failed extraction never produces a score. */
object PdfCvAnalyzer {
    fun extract(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(10 * 1024 * 1024) }
            ?: error("PDF tidak dapat dibaca. Pilih ulang file.")
        require(bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-") { "File bukan PDF yang valid." }
        return PDDocument.load(bytes).use { pdf ->
            require(!pdf.isEncrypted) { "PDF dilindungi kata sandi. Gunakan PDF tanpa enkripsi." }
            require(pdf.numberOfPages in 1..30) { "PDF harus berisi 1–30 halaman." }
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            stripper.getText(pdf).take(250_000).trim().also {
                require(it.count(Char::isLetter) >= 40) { "Teks PDF tidak dapat diekstrak atau terlalu sedikit. PDF hasil pindai memerlukan OCR; analisis tidak dijalankan." }
            }
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

    fun analyzeText(text: String, source: String): PdfCvAnalysis {
        require(text.count(Char::isLetter) >= 40) { "Tidak cukup teks untuk dianalisis." }
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val sections = mutableMapOf<String, MutableList<String>>()
        var section: String? = null
        lines.forEach { line ->
            val normalized = line.lowercase().trim(' ', ':', '-', '•')
            val key = headings.entries.firstOrNull { normalized in it.value }?.key
            if (key != null) { section = key; sections.getOrPut(key) { mutableListOf() } }
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
        val strengths = mutableListOf<String>(); val weaknesses = mutableListOf<String>(); val recommendations = mutableListOf<String>()
        var score = 0
        fun check(present: Boolean, points: Int, ok: String, missing: String, recommendation: String) {
            if (present) { score += points; strengths += ok } else { weaknesses += missing; recommendations += recommendation }
        }
        check(email.isNotBlank(), 15, "Alamat email terbaca.", "Email belum terdeteksi.", "Cantumkan email profesional sebagai teks.")
        check(phone.isNotBlank(), 10, "Nomor telepon terbaca.", "Nomor telepon belum terdeteksi.", "Cantumkan nomor telepon dengan kode area/negara.")
        check(cv.summary.isNotBlank(), 15, "Profil profesional tersedia.", "Bagian profil belum terdeteksi.", "Tambahkan judul Profil dan ringkasan singkat.")
        check(cv.experience.isNotEmpty(), 20, "Bagian pengalaman tersedia.", "Bagian pengalaman belum terdeteksi.", "Cantumkan pengalaman kerja, magang, atau organisasi yang benar-benar dimiliki.")
        check(cv.education.isNotEmpty(), 15, "Bagian pendidikan tersedia.", "Bagian pendidikan belum terdeteksi.", "Gunakan judul Pendidikan, lalu cantumkan institusi dan periode.")
        check(cv.skills.isNotEmpty(), 15, "Bagian keterampilan tersedia.", "Bagian keterampilan belum terdeteksi.", "Cantumkan keterampilan yang Anda kuasai dengan judul Keterampilan.")
        val words = text.split(Regex("\\s+")).size
        check(words in 100..1200, 10, "Panjang isi cukup ringkas.", "Isi terlalu singkat atau panjang ($words kata).", "Usahakan CV ringkas dengan informasi relevan dan pencapaian nyata.")
        return PdfCvAnalysis(source, score.coerceIn(0, 100), strengths, weaknesses, recommendations, cv, text)
    }
}
