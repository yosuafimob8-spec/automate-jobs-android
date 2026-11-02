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
        val domain = TrainingDomainContent.forCategory(category)
        val depth = when (level) {
            TrainingLevel.BASIC -> "kenali konsep, alat, dan prosedur dasar"
            TrainingLevel.INTERMEDIATE -> "terapkan prosedur pada kasus operasional dan identifikasi penyimpangan"
            TrainingLevel.ADVANCED -> "analisis risiko, tentukan prioritas, dan rancang perbaikan yang terukur"
        }
        val lessons = common.mapIndexed { i, topic ->
            val content = when (i) {
                0 -> "Sebagai $position di bidang $category, pelajari alat dan istilah: ${domain.tool}. Untuk tingkat ${level.label}, $depth. Identifikasi risiko, hak akses, dan SOP keselamatan yang berlaku sebelum bekerja."
                1 -> "Simulasikan alur kerja $position: ${domain.workflow}. Tuliskan urutan tindakan, titik pemeriksaan, dan pihak yang menyetujui. Pada tingkat ${level.label}, $depth."
                2 -> "Buat contoh dokumen $position menggunakan: ${domain.record}. Cantumkan tanggal, nomor referensi, hasil verifikasi, dan penanggung jawab. Pastikan data akurat serta aksesnya terbatas."
                3 -> "Studi kasus $position: ${domain.incident}. Jelaskan cara mengidentifikasi fakta, mengamankan proses, mendokumentasikan bukti, dan melakukan eskalasi. Pada tingkat ${level.label}, $depth."
                else -> "Evaluasi pekerjaan $position dengan indikator ${domain.metric}. Tentukan baseline, target realistis, cara pengukuran, dan tindakan perbaikan berdasarkan bukti."
            }
            topic to content
        }
        val questions = listOf(
            TrainingQuestion("Perangkat atau istilah yang relevan bagi $position?", listOf("Tidak perlu memahami alat", domain.tool, "Gunakan data tanpa izin"), 1),
            TrainingQuestion("Alur kerja yang sesuai untuk $position?", listOf(domain.workflow, "Abaikan verifikasi", "Langsung tutup pekerjaan tanpa bukti"), 0),
            TrainingQuestion("Dokumen kerja yang relevan untuk $position?", listOf("Catatan palsu", "Data tanpa izin", domain.record), 2),
            TrainingQuestion("Jika terjadi ${domain.incident}, apa yang tepat?", listOf("Verifikasi, dokumentasikan, dan eskalasi sesuai SOP", "Sembunyikan kejadian", "Ubah data diam-diam"), 0),
            TrainingQuestion("Indikator kinerja yang relevan untuk $position?", listOf("Rumor tanpa data", domain.metric, "Tidak perlu mengukur hasil"), 1)
        )
        return TrainingCourse(
            "role_" + position.lowercase().replace(Regex("[^a-z0-9]+"), "_") + "_" + level.name.lowercase(),
            "$position • ${level.label}",
            lessons,
            questions
        )
    }
}
