package com.example.automatesmartbot.data.interview

import android.content.Context
import org.json.JSONObject

/** Satu soal HRD tetap + respons HRD-nya (naskah verbatim, jangan diubah). */
data class HrdQuestion(
    val no: Int,
    val title: String,
    val question: String,
    val response: String,
)

data class HrdScript(
    val interviewer: String,
    val questions: List<HrdQuestion>,
    val closing: String,
)

/** Muat naskah 10 soal HRD + penutup dari assets/interview/hrd_script.json. */
fun loadHrdScript(context: Context): HrdScript {
    val text = context.assets.open("interview/hrd_script.json").bufferedReader().use { it.readText() }
    val root = JSONObject(text)
    val questions = mutableListOf<HrdQuestion>()
    val arr = root.getJSONArray("questions")
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        questions.add(
            HrdQuestion(
                no = o.getInt("no"),
                title = o.getString("title"),
                question = o.getString("question"),
                response = o.getString("response"),
            ),
        )
    }
    return HrdScript(
        interviewer = root.optString("interviewer", "Bu Nadia"),
        questions = questions.sortedBy { it.no },
        closing = root.getString("closing"),
    )
}

/** Hasil penilaian STAR otomatis (offline, lokal) untuk satu jawaban. */
data class StarFeedback(
    val score: Int, // 0..100
    val strengths: List<String>,
    val improvements: List<String>,
    val improvedTip: String,
)

private val STAR_MARKERS = mapOf(
    "situasi" to listOf("situasi", "saat itu", "ketika", "waktu itu", "konteks"),
    "tugas" to listOf("tugas", "tanggung jawab", "tanggungjawab", "peran saya", "diminta"),
    "tindakan" to listOf("saya lakukan", "saya melakukan", "saya mengatur", "saya menghubungi", "saya memperbaiki", "saya memastikan", "langkah"),
    "hasil" to listOf("hasil", "hasilnya", "akhirnya", "tercapai", "meningkat", "berhasil", "selesai tepat"),
)

/**
 * Penilaian STAR heuristik — transparan dan offline.
 * Bukan nilai HRD sungguhan; label di UI harus menyebut "penilaian otomatis".
 */
fun scoreStarAnswer(answer: String): StarFeedback {
    val clean = cleanRepeats(answer.trim())
    val lower = clean.lowercase()
    val words = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val wordCount = words.size
    val strengths = mutableListOf<String>()
    val improvements = mutableListOf<String>()
    var score = 0

    // Panjang jawaban.
    when {
        wordCount >= 40 -> { score += 30; strengths += "Jawaban cukup detail (${wordCount} kata)." }
        wordCount >= 20 -> { score += 20; strengths += "Jawaban cukup jelas (${wordCount} kata)." }
        wordCount >= 10 -> { score += 10; improvements += "Tambahkan detail: ceritakan contoh nyata dengan langkah dan hasil." }
        wordCount > 0 -> { improvements += "Jawaban terlalu singkat — ceritakan satu contoh nyata secara lengkap." }
        else -> { improvements += "Jawaban kosong — coba jawab dengan satu contoh nyata." }
    }

    // Unsur STAR.
    val found = STAR_MARKERS.filter { (_, keys) -> keys.any { it in lower } }.keys
    score += found.size * 12
    if ("situasi" in found) strengths += "Menyebutkan situasi/konteks kejadian."
    if ("tugas" in found) strengths += "Menjelaskan tugas atau tanggung jawab Anda."
    if ("tindakan" in found) strengths += "Menjelaskan tindakan konkret yang Anda lakukan."
    if ("hasil" in found) strengths += "Menyebutkan hasil dari tindakan Anda."
    if ("tindakan" !in found) improvements += "Jelaskan tindakan yang Anda lakukan, bukan hanya situasinya."
    if ("hasil" !in found) improvements += "Tutup dengan hasil: apa yang tercapai setelah tindakan Anda."

    // Spesifik / terukur.
    val specific = listOf("contoh", "misalnya", "persen", "%", "rupiah", "hari", "minggu", "target", "tepat waktu")
    if (specific.any { it in lower } || Regex("\\d").containsMatchIn(clean)) {
        score += 12
        strengths += "Memberi detail spesifik (contoh, angka, atau target)."
    } else {
        improvements += "Tambahkan detail spesifik: angka, waktu, atau contoh nyata."
    }

    // Kata pengisi berulang.
    val fillers = listOf("eh", "anu", "apa ya", "gitu", "kayak")
    if (fillers.any { it in lower }) {
        score -= 5
        improvements += "Kurangi kata pengisi seperti “eh” atau “anu” agar terdengar lebih percaya diri."
    }

    val finalScore = score.coerceIn(0, 100)
    val tip = buildString {
        append("Versi lebih kuat: ceritakan dengan struktur Situasi – Tugas – Tindakan – Hasil. ")
        append("Contoh kerangka: “Saat [situasi], tugas saya adalah [tugas]. Saya [tindakan]. Hasilnya [hasil terukur].”")
        if (wordCount in 1..19) append(" Jawaban Anda saat ini masih singkat — tambahkan satu contoh nyata.")
    }
    return StarFeedback(
        score = finalScore,
        strengths = strengths.ifEmpty { listOf("Berani menjawab — itu langkah awal yang bagus.") },
        improvements = improvements.ifEmpty { listOf("Pertahankan struktur ini; tambahkan angka atau hasil terukur bila ada.") },
        improvedTip = tip,
    )
}
