package com.example.automatesmartbot.data.career

enum class TrainingLevel(val label: String) { BASIC("Basic"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced") }

object TrainingPathways {
    private val common = listOf(
        "Keselamatan dan etika kerja",
        "Alur kerja dan tanggung jawab",
        "Dokumentasi dan pelaporan",
        "Pemecahan masalah operasional",
        "Peningkatan kualitas dan evaluasi"
    )
    fun courseFor(position: String, level: TrainingLevel): TrainingCourse {
        require(TrainingPositions.groups.any { position in it.positions }) { "Posisi tidak tersedia" }
        val category = TrainingPositions.groups.first { position in it.positions }.category
        val depth = when (level) {
            TrainingLevel.BASIC -> "kenali konsep, alat, dan prosedur dasar"
            TrainingLevel.INTERMEDIATE -> "terapkan prosedur pada kasus operasional dan identifikasi penyimpangan"
            TrainingLevel.ADVANCED -> "analisis risiko, tentukan prioritas, dan rancang perbaikan yang terukur"
        }
        val lessons = common.mapIndexed { i, topic ->
            val content = when (i) {
                0 -> "Dalam posisi $position pada bidang $category, $depth. Pelajari aturan keselamatan, kerahasiaan data, dan batas kewenangan. Laporkan bahaya dan jangan melakukan tindakan yang tidak berizin."
                1 -> "Petakan pekerjaan $position dari penerimaan tugas, persiapan alat dan data, pelaksanaan, pemeriksaan hasil, sampai serah terima. $depth. Ikuti SOP perusahaan yang berlaku."
                2 -> "Catat tanggal, pihak terkait, bukti pekerjaan, dan hasil verifikasi untuk tugas $position. Gunakan format resmi dan batasi akses data. $depth."
                3 -> "Saat menemukan selisih atau kendala pada tugas $position, hentikan tindakan berisiko, verifikasi fakta, dokumentasikan masalah, lalu eskalasi sesuai SOP. $depth."
                else -> "Tinjau mutu, ketepatan waktu, keamanan, dan kepuasan pemangku kepentingan pada pekerjaan $position. Gunakan indikator yang dapat diperiksa. $depth."
            }
            topic to content
        }
        val questions = listOf(
            TrainingQuestion("Apa tindakan pertama ketika menemukan bahaya dalam tugas $position?", listOf("Mengabaikannya", "Mengikuti SOP keselamatan dan melapor", "Menyembunyikannya"), 1),
            TrainingQuestion("Bagaimana alur kerja $position yang benar?", listOf("Persiapan, pelaksanaan, verifikasi, serah terima", "Langsung selesai tanpa pemeriksaan", "Menghapus catatan"), 0),
            TrainingQuestion("Dokumentasi pekerjaan harus?", listOf("Dibuat-buat", "Disebarkan tanpa izin", "Akurat dan sesuai hak akses"), 2),
            TrainingQuestion("Jika ditemukan selisih dalam pekerjaan?", listOf("Verifikasi, catat, dan eskalasi", "Ubah data diam-diam", "Abaikan"), 0),
            TrainingQuestion("Peningkatan kualitas sebaiknya berdasarkan?", listOf("Tebakan", "Data hasil kerja yang terukur", "Rumor"), 1)
        )
        return TrainingCourse(
            "role_" + position.lowercase().replace(Regex("[^a-z0-9]+"), "_") + "_" + level.name.lowercase(),
            "$position • ${level.label}",
            lessons,
            questions
        )
    }
}
