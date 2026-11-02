package com.example.automatesmartbot.data.career

/** Transparent practice rubric, not an AI competency assessment. */
object TrainingPractice {
    data class Result(val accepted: Boolean, val feedback: String)
    fun assess(answer: String): Result {
        val words = answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 20) return Result(false, "Jelaskan kasus dengan minimal 20 kata.")
        if (answer.trim().length < 120) return Result(false, "Tambahkan rincian langkah kerja dan hasil yang diharapkan.")
        return Result(true, "Jawaban memenuhi kriteria kelengkapan awal. Ini bukan penilaian kompetensi oleh asesor.")
    }
}
