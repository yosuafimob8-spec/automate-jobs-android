package com.example.automatesmartbot.data.cv

import android.content.Context
import android.net.Uri

data class PdfCvAnalysis(
    val source: String, val score: Int, val strengths: List<String>, val weaknesses: List<String>,
    val recommendations: List<String>, val cv: CvContent, val extractedText: String,
    val analyzedAt: Long = System.currentTimeMillis()
)

/** Local, rule-based completeness analysis. A failed extraction never produces a score. */
object PdfCvAnalyzer {
    fun extract(context: Context, uri: Uri): String = SharedCvParser.extract(context, uri).text

    fun analyzeContent(cv: CvContent): PdfCvAnalysis {
        require(cv.hasUserText()) { "CV Saya belum berisi data. Isi CV atau pilih PDF dari penyimpanan." }
        val text = SharedCvParser.renderContent(cv)
        return analyzeText(text, "CV Saya — ${cv.contact.fullName}").copy(cv = cv)
    }

    fun analyzeText(text: String, source: String): PdfCvAnalysis {
        val parsed = SharedCvParser.parseText(text)
        val cv = parsed.content
        val email = cv.contact.email
        val phone = cv.contact.phone
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
