package com.example.automatesmartbot.data.career

/** Offline QR identification; it does not assert authenticity against a remote registry. */
object TrainingCertificateIdentity {
    fun verifyLocal(payload: String, certificates: List<String>): Boolean {
        val parts = payload.split("|")
        if (parts.size != 6 || parts[0] != "KarirMate" || parts[1] != "INTERNAL") return false
        return certificates.contains(payload)
    }
    /** Public QR is enabled only after the server has issued and registered the certificate. */
    fun publicUrl(id: String, baseUrl: String): String {
        require(id.matches(Regex("[a-fA-F0-9-]{36}"))) { "ID sertifikat tidak valid" }
        require(baseUrl.startsWith("https://") && !baseUrl.contains("?") && !baseUrl.contains("#")) {
            "Verifikasi publik wajib menggunakan HTTPS"
        }
        return baseUrl.trimEnd('/') + "/certificate?id=" + id
    }
    fun payload(id: String, courseId: String, date: String, score: Int): String {
        require(id.matches(Regex("[a-fA-F0-9-]{36}"))) { "ID sertifikat tidak valid" }
        require(courseId.matches(Regex("[a-z0-9_]+"))) { "Kode pelatihan tidak valid" }
        require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { "Tanggal tidak valid" }
        require(score in 0..100) { "Nilai tidak valid" }
        return "KarirMate|INTERNAL|$id|$courseId|$date|$score"
    }
}
