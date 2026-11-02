package com.example.automatesmartbot.data.career

/** Offline QR identification; it does not assert authenticity against a remote registry. */
object TrainingCertificateIdentity {
    fun payload(id: String, courseId: String, date: String, score: Int): String {
        require(id.matches(Regex("[a-fA-F0-9-]{36}"))) { "ID sertifikat tidak valid" }
        require(courseId.matches(Regex("[a-z0-9_]+"))) { "Kode pelatihan tidak valid" }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Tanggal tidak valid" }
        require(score in 0..100) { "Nilai tidak valid" }
        return "KarirMate|INTERNAL|$id|$courseId|$date|$score"
    }
}
