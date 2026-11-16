package com.example.automatesmartbot.data.interview

import android.content.Context
import org.json.JSONObject

data class BankSource(val name: String, val url: String)

data class BankCategory(
    val id: Int,
    val title: String,
    val questions: List<String>,
    val followUp: String,
    val sources: List<BankSource>,
)

data class UniversalBank(val categories: List<BankCategory>) {
    val totalQuestions: Int get() = categories.sumOf { it.questions.size }
}

/** Muat bank 128 soal / 13 kategori dari assets/interview/universal_bank.json. */
fun loadUniversalBank(context: Context): UniversalBank {
    val text = context.assets.open("interview/universal_bank.json").bufferedReader().use { it.readText() }
    val root = JSONObject(text)
    val categories = mutableListOf<BankCategory>()
    val arr = root.getJSONArray("categories")
    for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val qs = mutableListOf<String>()
        val qa = o.getJSONArray("questions")
        for (j in 0 until qa.length()) qs.add(qa.getString(j))
        val srcs = mutableListOf<BankSource>()
        val sa = o.optJSONArray("sources")
        if (sa != null) {
            for (j in 0 until sa.length()) {
                val s = sa.getJSONObject(j)
                srcs.add(BankSource(s.optString("name"), s.optString("url")))
            }
        }
        categories.add(
            BankCategory(
                id = o.getInt("id"),
                title = o.getString("title"),
                questions = qs,
                followUp = o.optString("followUp"),
                sources = srcs,
            ),
        )
    }
    return UniversalBank(categories.sortedBy { it.id })
}

/**
 * Sesi latihan acak dari bank (mode tambahan, BUKAN sesi utama).
 * Komposisi 10 soal (8 slot utama + 2 variasi unik): Pembuka, Motivasi, Kelebihan, satu behavioral acak,
 * satu Teamwork/Situasional acak, satu Pengalaman/Kepemimpinan acak,
 * Gaji/Karier, Penutup. Kategori Logistik hanya dipakai bila diminta.
 */
fun buildRandomBankSession(bank: UniversalBank, includeLogistics: Boolean = false): List<Pair<String, String>> {
    val byId = bank.categories.associateBy { it.id }
    fun pick(id: Int): Pair<String, String> {
        val cat = byId[id] ?: return "Latihan" to "Ceritakan pengalaman Anda."
        return cat.title to cat.questions.random()
    }
    fun pickAny(ids: List<Int>): Pair<String, String> = pick(ids.random())
    val session = mutableListOf<Pair<String, String>>()
    session += pick(1)
    session += pick(2)
    session += pick(3)
    session += pickAny(listOf(5, 6))
    session += pickAny(listOf(7, 10))
    session += pickAny(listOf(4, 8, 9))
    session += if (includeLogistics) pick(13) else pick(11)
    session += pick(12)

    // KM_P1C_TEN_UNIQUE_QUESTIONS
    // Pertahankan delapan slot utama; lengkapi menjadi 10 soal unik.
    val seen = mutableSetOf<String>()
    val unique = session.filter { (_, question) ->
        val key = question.trim().lowercase()
        key.isNotEmpty() && seen.add(key)
    }.toMutableList()

    val alternatives = bank.categories
        .filter { includeLogistics || it.id != 13 }
        .flatMap { category ->
            category.questions.map { question ->
                category.title to question
            }
        }
        .shuffled()

    for (candidate in alternatives) {
        if (unique.size >= 10) break
        val key = candidate.second.trim().lowercase()
        if (key.isNotEmpty() && seen.add(key)) {
            unique += candidate
        }
    }

    require(unique.size == 10) {
        "Bank interview tidak memiliki cukup soal unik untuk sesi 10 pertanyaan."
    }
    return unique
}
