package com.example.automatesmartbot.data.career

object TrainingRoleSpecialization {
    data class Plan(val tasks: List<String>, val deliverables: List<String>)
    fun forPosition(position: String): Plan {
        val group = TrainingPositions.groups.firstOrNull { position in it.positions }
            ?: error("Posisi tidak dikenal: $position")
        val domain = TrainingDomainContent.forCategory(group.category)
        val responsibility = when {
            position.contains("Supervisor") || position.contains("Kepala") || position.contains("Leader") ->
                "membagi tugas tim, memeriksa hasil, mengendalikan risiko dan melakukan coaching"
            position.contains("Admin") || position.contains("Data Entry") || position.contains("Document") ->
                "memvalidasi input, mencatat bukti, menjaga versi dokumen dan menyelesaikan selisih"
            position.contains("Quality") || position.contains("Tester") || position.contains("Control") ->
                "melakukan inspeksi berbasis kriteria, mencatat temuan dan memverifikasi tindakan koreksi"
            position.contains("Driver") || position.contains("Kurir") || position.contains("Dispatcher") ->
                "memastikan keselamatan perjalanan, ketepatan tujuan dan bukti serah terima"
            position.contains("Sales") || position.contains("Marketing") || position.contains("SPG") ->
                "mengidentifikasi kebutuhan pelanggan, menyampaikan penawaran dan mencatat tindak lanjut"
            position.contains("Teknisi") || position.contains("Technician") || position.contains("Maintenance") ->
                "melakukan pemeriksaan keselamatan, diagnosis dan uji fungsi sebelum serah terima"
            else -> "menjalankan tanggung jawab jabatan $position berdasarkan SOP yang berlaku"
        }
        return Plan(
            listOf(
                "Identifikasi risiko saat $responsibility menggunakan ${domain.tool}",
                "Urutkan langkah $responsibility dalam proses ${domain.workflow}",
                "Lakukan verifikasi dan pencatatan $responsibility pada ${domain.record}",
                "Selesaikan studi kasus ${domain.incident} saat $responsibility",
                "Usulkan perbaikan $responsibility dengan ukuran ${domain.metric}"
            ),
            listOf(
                "checklist risiko dan kewenangan $position",
                "bagan alur kerja $position",
                "contoh dokumen dan bukti kerja $position",
                "laporan insiden dan eskalasi $position",
                "rencana pengukuran dan perbaikan $position"
            )
        )
    }
    fun positions(): Set<String> = TrainingPositions.groups.flatMap { it.positions }.toSet()
}
