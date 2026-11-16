package com.example.automatesmartbot.data.interview

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * Membaca teks CV dari file PDF / DOCX / TXT (maks 10 MB, 40 halaman).
 * Dipakai oleh Latihan Terpandu; fungsi extractCv lama di AiInterviewScreen
 * tidak diubah agar alur server yang sudah ada tetap sama.
 */
fun extractCvText(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri).orEmpty()
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            require(output.size() <= 10 * 1024 * 1024) { "CV maksimal 10 MB." }
        }
        output.toByteArray()
    } ?: error("CV tidak dapat dibuka.")
    require(bytes.isNotEmpty()) { "File CV kosong." }
    val text = when {
        mime == "application/pdf" -> {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(bytes).use {
                require(it.numberOfPages <= 40) { "CV maksimal 40 halaman." }
                PDFTextStripper().getText(it)
            }
        }
        mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
            val output = StringBuilder()
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val parser = Xml.newPullParser()
                        parser.setInput(zip, "UTF-8")
                        var event = parser.eventType
                        while (event != XmlPullParser.END_DOCUMENT) {
                            if (event == XmlPullParser.START_TAG && parser.name == "t") output.append(parser.nextText())
                            else if (event == XmlPullParser.END_TAG && parser.name == "p") output.append(10.toChar())
                            require(output.length <= 100000) { "Isi CV terlalu panjang." }
                            event = parser.next()
                        }
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            output.toString()
        }
        mime.startsWith("text/") || uri.lastPathSegment.orEmpty().endsWith(".txt", ignoreCase = true) -> {
            bytes.toString(Charsets.UTF_8)
        }
        else -> error("Pilih file PDF, DOCX, atau TXT.")
    }
    val result = text.trim()
    require(result.length >= 30) { "Teks CV tidak terbaca. PDF hasil scan memerlukan OCR." }
    return result.take(6000)
}

private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")
private val PHONE_REGEX = Regex("(\\+62|62|0)8\\d{7,12}|\\b0\\d{8,12}\\b")
private val DATE_REGEX = Regex("\\b\\d{1,2}[-/ ]\\d{1,2}[-/ ]\\d{2,4}\\b|\\b\\d{1,2} (Januari|Februari|Maret|April|Mei|Juni|Juli|Agustus|September|Oktober|November|Desember) \\d{4}\\b")
private val ADDRESS_HINTS = listOf("kp.", "kp ", "jl.", "jl ", "jalan ", "desa", "kec.", "kecamatan", "kab.", "kabupaten", "rt ", "rw ", "kode pos")

/**
 * HANYA untuk tampilan: hapus email, nomor HP, tanggal lahir, dan baris alamat
 * dari teks apa pun yang berasal dari CV. Tidak ada data pribadi yang boleh
 * ditampilkan atau dibacakan dari profil CV.
 */
fun redactSensitive(text: String): String {
    var out = text
    out = EMAIL_REGEX.replace(out, "[email disembunyikan]")
    out = PHONE_REGEX.replace(out, "[nomor disembunyikan]")
    out = DATE_REGEX.replace(out, "[tanggal disembunyikan]")
    val kept = out.lines().filter { line ->
        val lower = line.lowercase()
        ADDRESS_HINTS.none { it in lower }
    }
    return kept.joinToString("\n").trim()
}

data class CvProfile(
    val name: String?,
    val skills: List<String>,
    val lastCompany: String?,
    val lastActivity: String?,
)

private val SKILL_KEYWORDS = listOf(
    "Manajemen Gudang", "Pengelolaan Stok", "Stock Opname", "Inventory",
    "Penyusunan Rute", "Pengiriman", "Penerimaan Barang", "Problem Solving",
    "Manajemen Waktu", "Ketelitian", "Koordinasi", "Pelayanan Pelanggan",
    "Administrasi", "Microsoft Excel", "Komunikasi", "Kerja Tim",
    "Pengemasan", "Pemeriksaan Barang", "Distribusi", "Logistik",
)

private val NAME_BLOCKLIST = listOf(
    "detail", "profil", "curriculum", "riwayat", "daftar", "tentang", "data",
    "pendidikan", "pengalaman", "keahlian", "kontak", "alamat",
)

/** Analisis ringan dari teks CV yang sudah dibersihkan (tanpa data sensitif). */
fun analyzeCv(rawText: String): CvProfile {
    val clean = redactSensitive(rawText)
    val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }

    val name = lines.firstOrNull()?.takeIf { line ->
        val words = line.split(Regex("\\s+"))
        words.size in 2..4 && words.all { w -> w.all { ch -> ch.isLetter() || ch == '.' || ch == '-' } } &&
            NAME_BLOCKLIST.none { it in line.lowercase() }
    }?.split(Regex("\\s+"))?.take(3)?.joinToString(" ")

    val lower = clean.lowercase()
    val skills = SKILL_KEYWORDS.filter { it.lowercase() in lower }.take(8)

    // Perusahaan + aktivitas terakhir: cari baris berisi tahun, lalu baris kerja sesudahnya.
    var company: String? = null
    var activity: String? = null
    val yearLineIdx = lines.indexOfFirst { Regex("(19|20)\\d{2}").containsMatchIn(it) }
    if (yearLineIdx >= 0) {
        val yearLine = lines[yearLineIdx]
        val afterYears = yearLine.replace(Regex("(19|20)\\d{2}\\s*[-–/]\\s*(19|20)?\\d{2,4}"), "")
            .replace(Regex("(19|20)\\d{2}"), "").trim(' ', '-', '–', '/').trim()
        // Baris tahun biasanya "Jabatan Tahun - Tahun Perusahaan": ambil kata setelah jabatan.
        val titleWords = listOf("koordinator", "staff", "staf", "supervisor", "manager", "manajer", "operator", "admin", "kepala", "asisten")
        val lowerLine = afterYears.lowercase()
        val titlePos = titleWords.mapNotNull { w -> lowerLine.indexOf(w).takeIf { it >= 0 } }.minOrNull()
        val candidate = if (titlePos != null) {
            // Perusahaan = kata-kata setelah jabatan pada baris yang sama.
            val words = afterYears.split(Regex("\\s+"))
            val titleIdx = words.indexOfFirst { w -> titleWords.any { t -> w.lowercase().startsWith(t) } }
            if (titleIdx >= 0 && titleIdx + 1 < words.size) words.drop(titleIdx + 1).joinToString(" ") else afterYears
        } else afterYears
        company = candidate.split(Regex("\\s+")).take(3).joinToString(" ")
            .trim().takeIf { it.length >= 3 && !it.contains("20") }
        val verbPrefixes = listOf("mengatur", "memastikan", "memantau", "mengelola", "berkoordinasi", "menangani", "menyusun", "mengawasi", "melakukan", "bertanggung")
        for (i in yearLineIdx + 1 until minOf(lines.size, yearLineIdx + 6)) {
            val l = lines[i]
            if (verbPrefixes.any { l.lowercase().startsWith(it) }) {
                activity = l.trimEnd('.').take(90)
                break
            }
        }
    }
    return CvProfile(name = name, skills = skills, lastCompany = company, lastActivity = activity)
}

/**
 * Soal pengalaman singkat dari CV — HANYA aktivitas + perusahaan, tidak
 * pernah teks CV mentah. Dipakai menggantikan Soal 2 naskah HRD bila CV ada.
 * Contoh hasil: "Ceritakan pengalaman Anda mengatur jadwal pengiriman kurir
 * di Shopee Express. Apa tanggung jawab Anda dan apa hasilnya?"
 */
fun buildExperienceQuestion(profile: CvProfile): String? {
    val activity = profile.lastActivity?.replaceFirstChar { it.lowercase() } ?: return null
    val company = profile.lastCompany ?: return null
    return "Ceritakan pengalaman Anda $activity di $company. Apa tanggung jawab Anda dan apa hasilnya?"
}
